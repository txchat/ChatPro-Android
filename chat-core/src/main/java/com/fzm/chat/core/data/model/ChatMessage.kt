package com.fzm.chat.core.data.model

import android.content.Context
import androidx.room.Embedded
import androidx.room.Ignore
import com.fzm.chat.core.R
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.msg.MsgState
import com.fzm.chat.core.data.po.*
import com.fzm.chat.core.rtc.calling.RTCCalling
import com.fzm.chat.core.utils.uuid
import dtalk.biz.Biz
import java.io.File
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/25
 * Description:
 */
class ChatMessage(
    /**
     * 服务端生成消息id
     */
    var logId: Long,
    /**
     * 客户端生成消息id
     */
    var msgId: String,
    /**
     * 消息渠道(私聊、群聊等)
     */
    val channelType: Int,
    /**
     * 消息发送方
     */
    val from: String,
    /**
     * 消息接收方
     */
    val target: String,
    /**
     * 消息时间
     */
    var datetime: Long,
    /**
     * 发送状态
     */
    var state: Int,
    /**
     * 消息类型
     */
    var msgType: Int,
    /**
     * 消息内容
     */
    var msg: MessageContent,
    /**
     * 消息转发来源
     */
    var source: MessageSource?,
    /**
     * 消息引用
     */
    @Embedded(prefix = "ref_")
    var reference: Reference?
) : Serializable, Cloneable {

    /**
     * 是否显示消息时间
     */
    @Transient
    var showTime: Boolean = false

    /**
     * 是否需要弹出通知
     */
    @Ignore
    var notify: Boolean = true

    /**
     * 是否选中
     */
    @Transient
    var isSelected: Boolean = false

    /**
     * 下载进度
     */
    @Ignore
    var progress: Float = 0f

    /**
     * 关注人数
     */
    var focusNum: Int? = null

    /**
     * 自己是否关注该消息
     */
    @Ignore
    var hasFocused: Boolean = false

    /**
     * 消息发送方信息
     */
    @Ignore
    var sender: Contact? = null

    /**
     * 全文搜索时匹配词在搜索字段中的切片
     */
    var matchSnippet: String? = null

    companion object {
        const val MSG_TIME = "MSG_TIME"
        const val MSG_NICKNAME = "MSG_NICKNAME"
        const val MSG_AVATAR = "MSG_AVATAR"
        const val MSG_TAG = "MSG_TAG"
        const val MSG_STATE = "MSG_STATE"
        const val MSG_READ_STATE = "MSG_READ_STATE"
        const val MSG_PROGRESS = "MSG_PROGRESS"
        const val MSG_SOURCE = "MSG_SOURCE"
        const val MSG_REFERENCE = "MSG_REFERENCE"
        const val MSG_FOCUS = "MSG_FOCUS"

        fun create(
            from: String?,
            target: String?,
            channelType: Int,
            msgType: Biz.MsgType,
            content: MessageContent,
            source: MessageSource? = null,
            reference: Reference? = null
        ) = ChatMessage(
            0, uuid(), channelType, from ?: "", target ?: "",
            System.currentTimeMillis(), MsgState.SENDING, msgType.number, content, source, reference
        )

        fun create(
            from: String?,
            target: String?,
            channelType: Int,
            msgType: Int,
            content: MessageContent,
            source: MessageSource? = null,
            reference: Reference? = null
        ) = ChatMessage(
            0, uuid(), channelType, from ?: "", target ?: "",
            System.currentTimeMillis(), MsgState.SENDING, msgType, content, source, reference
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChatMessage

        if (logId != other.logId) return false
        if (msgId != other.msgId) return false
        if (channelType != other.channelType) return false
        if (from != other.from) return false
        if (target != other.target) return false
        if (datetime != other.datetime) return false
        if (state != other.state) return false
        if (msgType != other.msgType) return false
        if (msg != other.msg) return false
        if (source != other.source) return false
        if (reference != other.reference) return false
        if (showTime != other.showTime) return false
        if (focusNum != other.focusNum) return false
        if (sender != other.sender) return false

        return true
    }

    override fun hashCode(): Int {
        var result = logId.hashCode()
        result = 31 * result + msgId.hashCode()
        result = 31 * result + channelType
        result = 31 * result + from.hashCode()
        result = 31 * result + target.hashCode()
        result = 31 * result + datetime.hashCode()
        result = 31 * result + state
        result = 31 * result + msgType
        result = 31 * result + msg.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + reference.hashCode()
        result = 31 * result + showTime.hashCode()
        result = 31 * result + focusNum.hashCode()
        result = 31 * result + (sender?.hashCode() ?: 0)
        return result
    }

    public override fun clone(): ChatMessage {
        val message = msg.clone()
        val source = source?.clone()
        val reference = reference?.clone()
        return ChatMessage(logId, msgId, channelType, from, target, datetime, state, msgType, message, source, reference).also {
            it.focusNum = focusNum
            it.matchSnippet = matchSnippet
        }
    }
}

val ChatMessage.focusUserNum: Int get() = focusNum ?: 0

val ChatMessage.isGroup: Boolean
    get() = channelType == ChatConst.GROUP_CHANNEL

val ChatMessage.isSendType: Boolean
    get() = AppPreference.ADDRESS == from

val ChatMessage.contact: String
    get() {
        return if (channelType == ChatConst.PRIVATE_CHANNEL) {
            if (isSendType) target else from
        } else {
            target
        }
    }

val MessagePO.isGroup: Boolean
    get() = channelType == ChatConst.GROUP_CHANNEL

val MessagePO.isSendType: Boolean
    get() = AppPreference.ADDRESS == from

val MessagePO.contact: String
    get() {
        return if (channelType == ChatConst.PRIVATE_CHANNEL) {
            if (isSendType) target else from
        } else {
            target
        }
    }

/**
 * 消息是否已发送
 */
val ChatMessage.hasSent: Boolean
    get() = state >= MsgState.SENT

/**
 * 消息是否能够被合并转发
 */
val ChatMessage.canBatchForward: Boolean
    get() = msgType != Biz.MsgType.RTCCall_VALUE
        && msgType != Biz.MsgType.Transfer_VALUE
        && msgType != Biz.MsgType.RedPacket_VALUE

/**
 * 消息是否能够被转发
 */
val ChatMessage.canForward: Boolean
    get() = canBatchForward
        && msgType != Biz.MsgType.Audio_VALUE

/**
 * 数据库消息结构转换为聊天消息
 */
fun MessagePO.toChatMessage(): ChatMessage {
    return ChatMessage(logId, msgId, channelType, from, target, datetime, state, msgType, msg, source, reference)
}

/**
 * 聊天消息结构转换为数据库消息
 */
fun ChatMessage.toMessagePO(): MessagePO {
    return MessagePO(logId, msgId, channelType, from, target, datetime, state, msgType, msg, source, reference)
}

/**
 * 聊天消息结构转换为转发消息
 */
fun ChatMessage.toForwardMsg(): ForwardMsg {
    val name = if (channelType == ChatConst.PRIVATE_CHANNEL) {
        (sender as FriendUser?)?.getRawName() ?: ""
    } else {
        (sender as GroupUser?)?.getRawName() ?: ""
    }
    val msg = msg.clone().apply {
        if (ChatConfig.FILE_ENCRYPT) {
            // 开启文件加密时，mediaUrl置空，需要重新加密上传文件
            mediaUrl = null
        }
        atList = null
    }
    return ForwardMsg(sender?.getDisplayImage() ?: "", name, msgType, msg, datetime)
}

fun ChatMessage.localExists(): Boolean {
    return File(msg.localUrl ?: "").exists()
}

fun ChatMessage.getContent(context: Context): String {
    return when (Biz.MsgType.forNumber(msgType)) {
        Biz.MsgType.System -> context.getString(R.string.core_msg_type1) + msg.content
        Biz.MsgType.Text -> msg.content ?: ""
        Biz.MsgType.Audio -> context.getString(R.string.core_msg_type2)
        Biz.MsgType.Image -> context.getString(R.string.core_msg_type3)
        Biz.MsgType.Video -> context.getString(R.string.core_msg_type5)
        Biz.MsgType.File -> context.getString(R.string.core_msg_type6) + msg.fileName
        Biz.MsgType.Notification -> context.getString(R.string.core_msg_type7) + msg.content
        Biz.MsgType.Forward -> context.getString(R.string.core_msg_type8)
        Biz.MsgType.RTCCall -> if (msg.rtcType == RTCCalling.TYPE_AUDIO_CALL) {
            context.getString(R.string.core_msg_type9_1)
        } else {
            context.getString(R.string.core_msg_type9_2)
        }
        Biz.MsgType.Transfer -> context.getString(R.string.core_msg_type10)
        Biz.MsgType.RedPacket -> context.getString(R.string.core_msg_type12)
        Biz.MsgType.ContactCard -> {
            when (msg.contactType) {
                1 -> context.getString(R.string.core_msg_type13_1)
                2 -> context.getString(R.string.core_msg_type13_2)
                3 -> context.getString(R.string.core_msg_type13_3)
                else -> ""
            }
        }
        else -> context.getString(R.string.core_msg_type_unknown)
    }
}

fun ChatMessage.getSourceText(context: Context): String {
    return source?.let {
        if (it.channelType == ChatConst.PRIVATE_CHANNEL) {
            context.getString(
                R.string.core_forward_title_others_and_others,
                it.from.name,
                it.target.name
            )
        } else {
            context.getString(R.string.core_forward_title_group)
        }
    } ?: ""
}

fun Iterable<ChatMessage>.filterMedia() = filter {
    it.msgType == Biz.MsgType.Image_VALUE || it.msgType == Biz.MsgType.Video_VALUE
}

fun Iterable<ChatMessage>.filterFile() = filter {
    it.msgType == Biz.MsgType.Audio_VALUE || it.msgType == Biz.MsgType.Image_VALUE ||
            it.msgType == Biz.MsgType.Video_VALUE || it.msgType == Biz.MsgType.File_VALUE
}

val ChatMessage.isFileType: Boolean
    get() = msgType == Biz.MsgType.Audio_VALUE || msgType == Biz.MsgType.Image_VALUE ||
            msgType == Biz.MsgType.Video_VALUE || msgType == Biz.MsgType.File_VALUE