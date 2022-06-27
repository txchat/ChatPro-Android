package com.fzm.chat.core.data.local

import android.content.Context
import android.util.Base64
import com.fzm.chat.core.chain.ChainException
import com.fzm.chat.core.chain.waitForChainResult
import com.fzm.chat.core.crypto.decrypt
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.bean.Option
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.ForwardMsg
import com.fzm.chat.core.data.model.toMessagePO
import com.fzm.chat.core.data.msg.MsgState
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.data.po.MessagePO
import com.fzm.chat.core.logic.MessageSender
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.core.media.DownloadManager2
import com.fzm.chat.core.net.source.SearchDataSource
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.toResult
import com.fzm.chat.router.route
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.router.wallet.WalletService
import com.google.protobuf.ByteString
import com.zjy.architecture.data.Result
import com.zjy.architecture.di.rootScope
import dtalk.biz.Biz
import dtalk.biz.msg.Msg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * @author zhengjy
 * @since 2021/01/18
 * Description:
 */
object MessageRepository {

    private val contactManager by rootScope.inject<ContactManager>()
    private val dataSource by rootScope.inject<SearchDataSource>()
    private val context by rootScope.inject<Context>()
    private val delegate by rootScope.inject<LoginDelegate>()

    private val walletService by route<WalletService>(WalletModule.SERVICE)

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    /**
     * 根据时间获取指定私聊消息
     */
    suspend fun getMessageByTime(target: String, channelType: Int, datetime: Long, count:Int = ChatConfig.PAGE_SIZE): List<ChatMessage> {
        val list = database.messageDao().getMessageByTime(
            target,
            channelType,
            datetime,
            count
        ) ?: listOf()
        updateMessage(list, channelType)
        return list
    }
    suspend fun getMessageByMsgId(target: String, channelType: Int, msgId: String): List<ChatMessage> {
        val list = database.messageDao().getMessageByMsgId(
            target,
            channelType,
            msgId
        ) ?: listOf()
        updateMessage(list, channelType)
        return list
    }

    suspend fun getChatFile(target: String, channelType: Int, datetime: Long): List<ChatMessage> {
        val list = database.messageDao().getOneTypeMessage(
            target,
            channelType,
            datetime,
            Biz.MsgType.File_VALUE,
            ChatConfig.PAGE_SIZE
        ) ?: listOf()
        updateMessage(list, channelType)
        return list
    }

    suspend fun getChatMedia(target: String, channelType: Int, datetime: Long): List<ChatMessage> {
        val list = database.messageDao().getTwoTypeMessage(
            target,
            channelType,
            datetime,
            Biz.MsgType.Image_VALUE,
            Biz.MsgType.Video_VALUE,
            ChatConfig.PAGE_SIZE
        ) ?: listOf()
        updateMessage(list, channelType)
        return list
    }

    private suspend fun updateMessage(list: List<ChatMessage>, channelType: Int) {
        val update = mutableListOf<MessagePO>()
        val cache = mutableMapOf<String, Contact>()
        list.forEach {
            if (it.state == MsgState.SENDING) {
                // 如果消息状态为发送中，但是消息并不在发送队列里，则消息标记为发送失败
                if (!MessageSender.isSending(it)) {
                    it.state = MsgState.FAIL
                    database.messageDao().updateMsgState(it.msgId, MsgState.FAIL)
                }
            }
            when (it.msgType) {
                Biz.MsgType.Audio_VALUE,
                Biz.MsgType.Image_VALUE,
                Biz.MsgType.Video_VALUE -> DownloadManager2.downloadToApp(it)
                Biz.MsgType.Transfer_VALUE -> {
                    val payload = it.msg
                    if (payload.txStatus == "0") {
                        GlobalScope.launch(Dispatchers.IO) {
                            waitForChainResult(0L, checker = { tx -> tx.status != "0" }) {
                                // 查询交易详情
                                walletService?.getTransactionByHash(
                                    payload.chain!!,
                                    payload.txHash!!,
                                    payload.txSymbol!!
                                )?.toResult() ?: Result.Error(ChainException("交易查询失败"))
                            }.dataOrNull()?.also { tx ->
                                payload.txAmount = tx.value
                                payload.txStatus = tx.status
                                payload.txInvalid = tx.from != it.from || tx.to != it.target
                                MessageSubscription.onMessage(Option.UPDATE_CONTENT, it)
                                database.messageDao().insert(it.toMessagePO())
                            }
                        }
                    }
                }
                Biz.MsgType.Notification_VALUE -> {
                    when (Msg.NotificationType.forNumber(it.msg.notificationType)) {
                        Msg.NotificationType.RevokeMessage -> {
                            // 超过一定时长的撤回消息，无法再重新编辑
                            if (!it.msg.reedit.isNullOrEmpty() && System.currentTimeMillis() - it.datetime > ChatConfig.REEDIT_TIMEOUT) {
                                it.msg.reedit = null
                                GlobalScope.launch(Dispatchers.IO) {
                                    database.messageDao().insert(it.toMessagePO())
                                }
                            }
                        }
                    }
                }
            }
            if (it.msgType != Biz.MsgType.Notification_VALUE) {
                // 更新发送者信息
                it.sender = when {
                    cache[it.from] != null -> cache[it.from]
                    channelType == ChatConst.PRIVATE_CHANNEL -> contactManager.getUserInfo(it.from)
                        .also { contact -> cache[it.from] = contact }
                    else -> contactManager.getGroupUserInfo(it.target, it.from)
                        .also { contact -> cache[it.from] = contact }
                }
            }
            // 查询引用的消息
            it.reference?.ref?.let { ref ->
                it.reference?.refMsg = database.messageDao().findMessage(ref)
                it.reference?.refMsg?.let { refMsg ->
                    refMsg.sender = when {
                        cache[refMsg.from] != null -> cache[refMsg.from]
                        channelType == ChatConst.PRIVATE_CHANNEL -> contactManager.getUserInfo(refMsg.from)
                            .also { contact -> cache[refMsg.from] = contact }
                        else -> contactManager.getGroupUserInfo(it.target, refMsg.from)
                            .also { contact -> cache[refMsg.from] = contact }
                    }
                }
            }
            // 旧版本收到新版本消息，在更新版本后需要重新解析
            val type = Biz.MsgType.forNumber(it.msgType)
            val proto = it.msg.proto
            if (type != null && !proto.isNullOrEmpty()) {
                it.msg = parse(ByteString.copyFrom(Base64.decode(proto, Base64.NO_WRAP)), context, type, false)
                update.add(it.toMessagePO())
            }
        }
        if (update.isNotEmpty()) database.messageDao().insert(update)
    }

    suspend fun searchChatFiles(keywords: String, target: ChatTarget): List<ChatMessage> {
        val listByFileName = dataSource.searchChatFilesByTarget(keywords, target)
        // TODO : 2021年5月13日 15:44:44；按照发送人来搜索聊天文件
        val listByNickname = emptyList<ChatMessage>()
        return withContext(Dispatchers.IO) {
            (listByFileName + listByNickname).sortedBy { it.datetime }
                .also { updateMessage(it, target.channelType) }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun parse(
        bytes: ByteString,
        context: Context,
        msgType: Biz.MsgType,
        fromForward: Boolean
    ): MessageContent {
        return when (msgType) {
            Biz.MsgType.System, Biz.MsgType.Text -> {
                val text = Msg.Text.parseFrom(bytes)
                MessageContent.text(text.content, text.mentionList.ifEmpty { null })
            }
            Biz.MsgType.Audio -> {
                if (fromForward) return MessageContent.empty()
                val audio = Msg.Audio.parseFrom(bytes)
                MessageContent.audio(audio.url, null, audio.time)
            }
            Biz.MsgType.Image -> {
                val image = Msg.Image.parseFrom(bytes)
                MessageContent.image(image.url, null, intArrayOf(image.height, image.width))
            }
            Biz.MsgType.Video -> {
                val video = Msg.Video.parseFrom(bytes)
                MessageContent.video(
                    video.url,
                    null,
                    intArrayOf(video.height, video.width),
                    video.time
                )
            }
            Biz.MsgType.File -> {
                val file = Msg.File.parseFrom(bytes)
                MessageContent.file(file.url, null, file.name, file.size, file.md5)
            }
            Biz.MsgType.Notification -> {
                val notification = Msg.Notification.parseFrom(bytes)
                val content = when (notification.type) {
                    Msg.NotificationType.UpdateGroupName -> {
                        val msg = Msg.NotificationGroupName.parseFrom(notification.body)
                        val group = contactManager.getGroupInfo(msg.group.toString())
                        val operator = contactManager.getGroupUserInfo(msg.group.toString(), msg.operator)
                        "${operator.getDisplayName()} 修改群名为 ${msg.name.decrypt(group.key)}"
                    }
                    Msg.NotificationType.JoinGroup -> {
                        val msg = Msg.NotificationJoinGroup.parseFrom(notification.body)
                        val inviter = contactManager.getGroupUserInfo(msg.group.toString(), msg.inviter)
                        if (msg.membersCount > 0) {
                            val members = mutableListOf<Contact>()
                            // 最多显示5个人的昵称
                            val suffix = if (msg.membersCount > 5) "等${msg.membersCount}人进入群聊" else "进入群聊"
                            for (i in 0 until min(5, msg.membersCount)) {
                                members.add(contactManager.getGroupUserInfo(msg.group.toString(), msg.membersList[i]))
                            }
                            val builder = StringBuilder("${inviter.getDisplayName()} 邀请了")
                            members.forEachIndexed { index, contact ->
                                builder.append(contact.getDisplayName())
                                if (index != members.size - 1) {
                                    builder.append("、")
                                }
                            }
                            builder.append(suffix)
                            builder.toString()
                        } else {
                            // 邀请人为空，则表示自己创建了群聊
                            "${inviter.getDisplayName()} 创建了群聊"
                        }
                    }
                    Msg.NotificationType.ExitGroup -> {
                        val msg = Msg.NotificationExitGroup.parseFrom(notification.body)
                        val operator = contactManager.getGroupUserInfo(msg.group.toString(), msg.operator)
                        "${operator.getDisplayName()} 退出群聊"
                    }
                    Msg.NotificationType.KickOut -> {
                        val msg = Msg.NotificationKickOut.parseFrom(notification.body)
                        if (msg.membersList.contains(delegate.getAddress())) {
                            "你已被移出群聊"
                        } else {
                            val members = mutableListOf<Contact>()
                            // 最多显示5个人的昵称
                            val suffix = if (msg.membersCount > 5) "等${msg.membersCount}人被移出群聊" else "被移出群聊"
                            for (i in 0 until min(5, msg.membersCount)) {
                                members.add(contactManager.getGroupUserInfo(msg.group.toString(), msg.membersList[i]))
                            }
                            val builder = StringBuilder()
                            members.forEachIndexed { index, contact ->
                                builder.append(contact.getDisplayName())
                                if (index != members.size - 1) {
                                    builder.append("、")
                                }
                            }
                            builder.append(suffix)
                            builder.toString()
                        }
                    }
                    Msg.NotificationType.DisbandGroup -> "该群已解散"
                    Msg.NotificationType.GroupMute -> {
                        val msg = Msg.NotificationGroupMute.parseFrom(notification.body)
                        val operator = contactManager.getGroupUserInfo(msg.group.toString(), msg.operator)
                        if (msg.type == Msg.NotificationGroupMute.MuteType.MuteAll) {
                            "${operator.getDisplayName()} 设置全员禁言"
                        } else {
                            "${operator.getDisplayName()} 设置全员可发言"
                        }
                    }
                    Msg.NotificationType.GroupMemberMute -> {
                        val msg = Msg.NotificationGroupMemberMute.parseFrom(notification.body)
                        val members = mutableListOf<Contact>()
                        // 最多显示5个人的昵称
                        val suffix = if (msg.membersCount > 5) "等${msg.membersCount}人已禁言" else "已禁言"
                        for (i in 0 until min(5, msg.membersCount)) {
                            members.add(contactManager.getGroupUserInfo(msg.group.toString(), msg.membersList[i]))
                        }
                        val builder = StringBuilder()
                        members.forEachIndexed { index, contact ->
                            builder.append(contact.getDisplayName())
                            if (index != members.size - 1) {
                                builder.append("、")
                            }
                        }
                        builder.append(suffix)
                        builder.toString()
                    }
                    Msg.NotificationType.GroupChangeOwner -> {
                        val msg = Msg.NotificationGroupChangeOwner.parseFrom(notification.body)
                        val newOwner = contactManager.getGroupUserInfo(msg.group.toString(), msg.newOwner)
                        "${newOwner.getDisplayName()} 已成为新群主"
                    }
                    else -> ""
                }
                MessageContent.notification(content, notification.typeValue)
            }
            Biz.MsgType.Forward -> {
                if (fromForward) return MessageContent.empty()
                val forward = Msg.Forward.parseFrom(bytes)
                val logs = mutableListOf<ForwardMsg>()
                forward.logsList.forEach {
                    logs.add(
                        ForwardMsg(
                            it.avatar,
                            it.name,
                            it.msgType,
                            parse(it.msg, context, Biz.MsgType.forNumber(it.msgType), true),
                            it.datetime
                        )
                    )
                }
                MessageContent.forward(context, logs)
            }
            Biz.MsgType.Transfer -> {
                val transfer = Msg.Transfer.parseFrom(bytes)
                MessageContent.transfer(transfer.chain, transfer.platform, transfer.txHash, transfer.coinName, transfer.coinTypeValue)
            }
            Biz.MsgType.RedPacket -> {
                val redPacket = Msg.RedPacket.parseFrom(bytes)
                MessageContent.redPacket(
                    redPacket.packetId,
                    redPacket.exec,
                    redPacket.coinTypeValue,
                    redPacket.coinName,
                    redPacket.remark,
                    redPacket.packetTypeValue,
                    redPacket.privateKey,
                    redPacket.expire
                )
            }
            Biz.MsgType.ContactCard -> {
                val card = Msg.ContactCard.parseFrom(bytes)
                MessageContent.contactCard(
                    card.type,
                    card.id,
                    card.name,
                    card.avatar,
                    card.server,
                    card.inviter,
                )
            }
            else -> MessageContent.unknown(bytes.toByteArray())
        }
    }
}