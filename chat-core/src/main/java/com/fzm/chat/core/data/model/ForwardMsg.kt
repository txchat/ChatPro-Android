package com.fzm.chat.core.data.model

import android.content.Context
import com.fzm.chat.core.R
import com.fzm.chat.core.data.po.MessageContent
import dtalk.biz.Biz.MsgType.*
import java.io.Serializable
import kotlin.math.min

/**
 * @author zhengjy
 * @since 2021/06/17
 * Description:
 */
data class ForwardMsg(
    val avatar: String,
    val name: String,
    val msgType: Int,
    val msg: MessageContent,
    val datetime: Long
) : Serializable {

    /**
     * 是否显示消息时间
     */
    @Transient
    var showTime: Boolean = false
}

fun List<ForwardMsg>.getSnapShot(context: Context): String {
    if (this.isEmpty()) return ""
    val builder = StringBuilder()
    // 最多获取四条转发消息的摘要信息
    for (i in 0 until min(4, this.size)) {
        val msg = this[i]
        builder.append("${msg.name}:")
        when (forNumber(msg.msgType)) {
            System, Text -> builder.append(msg.msg.content.subMessage())
            Audio -> builder.append(context.getString(R.string.core_msg_type2))
            Image -> builder.append(context.getString(R.string.core_msg_type3))
            Video -> builder.append(context.getString(R.string.core_msg_type5))
            File -> builder.append(context.getString(R.string.core_msg_type6) + msg.msg.fileName.subMessage())
            Notification -> builder.append(context.getString(R.string.core_msg_type7) + msg.msg.content.subMessage())
            Forward -> builder.append(context.getString(R.string.core_msg_type8))
            RTCCall -> builder.append(context.getString(R.string.core_msg_type_special))
            ContactCard -> {
                when (msg.msg.contactType) {
                    1 -> builder.append(context.getString(R.string.core_msg_type13_1) + msg.msg.contactName)
                    2 -> builder.append(context.getString(R.string.core_msg_type13_2) + msg.msg.contactName)
                    3 -> builder.append(context.getString(R.string.core_msg_type13_3) + msg.msg.contactName)
                    else->builder.append("")
                }
            }
            else -> builder.append(context.getString(R.string.core_msg_type_unknown))
        }
        builder.append("\n")
    }
    return builder.toString()
}

/**
 * 防止消息过长
 */
private fun String?.subMessage(): String? {
    if (this == null) return null
    return this.substring(0, min(200, this.length)) + if (this.length > 200) "…" else ""
}

fun ForwardMsg.localExists(): Boolean {
    return java.io.File(msg.localUrl ?: "").exists()
}

fun Iterable<ForwardMsg>.filterMedia() = filter {
    it.msgType == Image_VALUE || it.msgType == Video_VALUE
}

fun Iterable<ForwardMsg>.filterFile() = filter {
    it.msgType == Audio_VALUE || it.msgType == Image_VALUE ||
            it.msgType == Video_VALUE || it.msgType == File_VALUE
}

val ForwardMsg.isFileType: Boolean
    get() = msgType == Audio_VALUE || msgType == Image_VALUE ||
            msgType == Video_VALUE || msgType == File_VALUE