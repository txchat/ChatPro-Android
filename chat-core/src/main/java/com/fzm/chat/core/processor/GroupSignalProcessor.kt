package com.fzm.chat.core.processor

import com.fzm.arch.connection.processor.Processor
import com.fzm.chat.core.crypto.decrypt
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.bean.MsgFocus
import com.fzm.chat.core.data.bean.Option
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.contact
import com.fzm.chat.core.data.model.toMessagePO
import com.fzm.chat.core.data.msg.MsgState
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.data.po.MessageFocusUser
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.core.repo.GroupRepository
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.main.MainService
import com.fzm.chat.router.route
import dtalk.biz.Biz
import dtalk.biz.msg.Msg
import dtalk.biz.signal.Signaling

/**
 * @author zhengjy
 * @since 2021/04/26
 * Description:
 */
class GroupSignalProcessor(
    private val delegate: LoginDelegate,
    private val groupRepo: GroupRepository,
    private val contactManager: ContactManager
) : Processor<Signaling.Msg> {

    private val mainService by route<MainService>(MainModule.SERVICE)

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    override suspend fun process(server: String, message: Signaling.Msg): Signaling.Msg? {
        when (Signaling.ActionType.forNumber(message.action)) {
            Signaling.ActionType.Received -> {
                val received = parseProtocol(Signaling.SignalReceived::class.java, message.body)
                if (received.logIdCount > 0) {
                    val affectedRow = database.messageDao()
                        .updateMsgState(received.logIdList, MsgState.SENT_AND_RECEIVE)
                    if (affectedRow != 0) {
                        database.messageDao().getMessageByLogId(received.logIdList).forEach {
                            MessageSubscription.onUpdateState(it)
                        }
                    } else {
                        MessageSubscription.ackSet.addAll(received.logIdList)
                    }
                }
                return null
            }
            Signaling.ActionType.Revoke -> {
                val revoke = parseProtocol(Signaling.SignalRevoke::class.java, message.body)
                database.messageDao().getMessageByLogId(listOf(revoke.logId)).filter {
                    it.msgType != Biz.MsgType.Notification_VALUE
                }.forEach {
                    val name = when {
                        revoke.operator == delegate.getAddress() -> "你"
                        it.channelType == ChatConst.PRIVATE_CHANNEL -> {
                            contactManager.getUserInfo(revoke.operator, true).getDisplayName()
                        }
                        else -> contactManager.getGroupUserInfo(it.target, revoke.operator).getDisplayName()
                    }
                    val self = revoke.operator == it.from
                    val content = "${name}撤回了一条${if (self) "消息" else "成员消息"}"
                    val msgContent = if (it.msgType == Biz.MsgType.Text_VALUE) {
                        if (revoke.operator == delegate.getAddress() && self) {
                            MessageContent.notification(
                                content,
                                it.msg.content,
                                Msg.NotificationType.RevokeMessage_VALUE
                            )
                        } else {
                            MessageContent.notification(
                                content,
                                Msg.NotificationType.RevokeMessage_VALUE
                            )
                        }
                    } else {
                        MessageContent.notification(
                            content,
                            Msg.NotificationType.RevokeMessage_VALUE
                        )
                    }
                    // 直接复用被撤回的消息数据，只修改消息类型和消息内容
                    val msg = it.clone().apply {
                        msgType = Biz.MsgType.Notification_VALUE
                        msg = msgContent
                        source = null
                    }
                    database.recentSessionDao().insertMessageReplace(msg.toMessagePO(), 0, false)
                    MessageSubscription.onMessage(Option.REVOKE_MSG, msg)
                }
                return null
            }
            Signaling.ActionType.JoinGroup -> {
                val join = parseProtocol(Signaling.SignalJoinGroup::class.java, message.body)
                // 自己是否包含在入群名单中
                val joinGroup = join.addressList.contains(delegate.current.value?.address)
                if (joinGroup) {
                    groupRepo.getGroupInfo(server, join.group, Contact.RELATION)
                    groupRepo.getGroupUserList(server, join.group)
                } else {
                    join.addressList.forEach {
                        groupRepo.getGroupUser(server, join.group, it)
                    }
                    database.groupDao().changeGroupUserNumBy(join.group, join.addressCount)
                }
                return null
            }
            Signaling.ActionType.ExitGroup -> {
                val exit = parseProtocol(Signaling.SignalExitGroup::class.java, message.body)
                if (exit.addressCount > 0) {
                    // 自己是否包含在退群名单中
                    val deleteGroup = exit.addressList.contains(delegate.current.value?.address)
                    if (deleteGroup) {
                        database.groupDao().deleteFlag(exit.group, Contact.RELATION)
                    } else {
                        exit.addressList.forEach {
                            database.groupUserDao().disableGroupUsers(exit.group, it)
                        }
                        database.groupDao().changeGroupUserNumBy(exit.group, -(exit.addressCount))
                    }
                }
                return null
            }
            Signaling.ActionType.DisbandGroup -> {
                val disband = parseProtocol(Signaling.SignalDisbandGroup::class.java, message.body)
                database.groupDao().deleteFlag(disband.group, Contact.RELATION)
                return null
            }
            Signaling.ActionType.FocusMessage -> {
                val focus = parseProtocol(Signaling.SignalFocusMessage::class.java, message.body)
                database.focusUserDao().insert(MessageFocusUser(focus.logId, focus.uid, focus.datetime))
                val msg = database.messageDao().findMessage(focus.logId)
                val hasFocused = database.focusUserDao().hasFocused(focus.logId, delegate.getAddress())
                MessageSubscription.onMessage(Option.UPDATE_FOCUS, MsgFocus(focus.logId, focus.currentNum, msg?.contact, hasFocused))
                return null
            }
            Signaling.ActionType.EndPointLogin -> {
                val login = parseProtocol(Signaling.SignalEndpointLogin::class.java, message.body)
                if (login.uuid != AppPreference.uuid) {
                    mainService?.onOtherEndPointLogin(login.deviceName, login.datetime, login.deviceValue)
                }
                return null
            }
            Signaling.ActionType.UpdateGroupJoinType -> {
                val joinType = parseProtocol(Signaling.SignalUpdateGroupJoinType::class.java, message.body)
                database.groupDao().changeJoinType(joinType.group, joinType.typeValue)
                return null
            }
            Signaling.ActionType.UpdateGroupFriendType -> {
                val friendType = parseProtocol(Signaling.SignalUpdateGroupFriendType::class.java, message.body)
                database.groupDao().changeFriendType(friendType.group, friendType.typeValue)
                return null
            }
            Signaling.ActionType.UpdateGroupMuteType -> {
                val muteType = parseProtocol(Signaling.SignalUpdateGroupMuteType::class.java, message.body)
                database.groupDao().changeMuteType(muteType.group, muteType.typeValue)
                return null
            }
            Signaling.ActionType.UpdateGroupMemberType -> {
                val memberType = parseProtocol(Signaling.SignalUpdateGroupMemberType::class.java, message.body)
                database.groupUserDao().changeGroupUserRole(memberType.group, memberType.uid, memberType.typeValue)
                if (delegate.current.value?.address == memberType.uid) {
                    // 自己的群聊角色变更
                    database.groupDao().changeMyRole(memberType.group, memberType.typeValue)
                }
                return null
            }
            Signaling.ActionType.UpdateGroupMemberMuteTime -> {
                val muteUser = parseProtocol(Signaling.SignalUpdateGroupMemberMuteTime::class.java, message.body)
                muteUser.uidList.forEach {
                    database.groupUserDao().changeMuteTime(muteUser.group, it, muteUser.muteTime)
                    if (delegate.current.value?.address == it) {
                        // 自己的禁言时间变更
                        database.groupDao().changeMyMuteTime(muteUser.group, muteUser.muteTime)
                    }
                }
                return null
            }
            Signaling.ActionType.UpdateGroupName -> {
                val updateName = parseProtocol(Signaling.SignalUpdateGroupName::class.java, message.body)
                val groupInfo = database.groupDao().getGroupInfo(updateName.group)
                if (groupInfo != null) {
                    database.groupDao().editGroupName(updateName.group, updateName.name.decrypt(groupInfo.key))
                }
                return null
            }
            Signaling.ActionType.UpdateGroupAvatar -> {
                val updateAvatar = parseProtocol(Signaling.SignalUpdateGroupAvatar::class.java, message.body)
                database.groupDao().editGroupAvatar(updateAvatar.group, updateAvatar.avatar)
                return null
            }
            else -> {

            }
        }
        return message
    }
}