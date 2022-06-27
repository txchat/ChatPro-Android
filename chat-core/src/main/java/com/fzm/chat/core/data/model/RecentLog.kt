package com.fzm.chat.core.data.model

import com.fzm.chat.core.R
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.data.po.MessagePO
import com.fzm.chat.core.rtc.calling.RTCCalling
import com.zjy.architecture.Arch.context
import dtalk.biz.Biz
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/31
 * Description:
 */
data class RecentLog(
    /**
     * 消息id
     */
    val logId: Long,
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
    val datetime: Long,
    /**
     * 发送状态
     */
    val state: Int,
    /**
     * 消息类型
     */
    val msgType: Int,
    /**
     * 消息内容
     */
    val msg: MessageContent,
) : Serializable {
    companion object {
        val EMPTY_LOG = RecentLog(0L, "", "", 0L, 0, 0, MessageContent.empty())
    }
}

fun MessagePO.toRecentLog(): RecentLog {
    return RecentLog(logId, from, target, datetime, state, msgType, msg.toRecentContent())
}

fun MessageContent.toRecentContent(): MessageContent {
    return MessageContent().also {
        it.content = content
        it.fileName = fileName
        it.rtcType = rtcType
        it.contactType = contactType
    }
}

fun RecentLog.getContent(): String {
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