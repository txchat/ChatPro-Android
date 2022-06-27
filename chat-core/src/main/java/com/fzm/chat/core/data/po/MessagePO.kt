package com.fzm.chat.core.data.po

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import dtalk.biz.Biz
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/28
 * Description:
 */
@Entity(
    tableName = "chat_message",
    primaryKeys = ["logId", "msgId"],
    indices = [
        Index(value = arrayOf("logId"), name = "index_logId"),
        Index(value = arrayOf("msgId"), name = "index_msgId", unique = true)
    ])
data class MessagePO(
    /**
     * 服务端生成消息id
     */
    val logId: Long,
    /**
     * 客户端生成消息id
     */
    val msgId: String,
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
    /**
     * 消息转发来源
     */
    val source: MessageSource?,
    /**
     * 消息引用
     */
    @Embedded(prefix = "ref_")
    var reference: Reference?
) : Serializable {

    var searchKey: String = ""
        get() {
            if (field.isNotEmpty()) return field
            return when (Biz.MsgType.forNumber(msgType)) {
                Biz.MsgType.Text, Biz.MsgType.System -> msg.content ?: ""
                Biz.MsgType.File -> msg.fileName ?: ""
                Biz.MsgType.Forward -> msg.content ?: ""
                else -> ""
            }.also { field = it }
        }
}