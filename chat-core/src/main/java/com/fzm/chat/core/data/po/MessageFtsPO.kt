package com.fzm.chat.core.data.po

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/28
 * Description:
 */
@Entity(tableName = "chat_message_fts")
@Fts4(
    // 使用微信的mmicu分词器
    tokenizer = "mmicu",
    contentEntity = MessagePO::class,
    notIndexed = [],
    order = FtsOptions.Order.DESC
)
data class MessageFtsPO(
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
     * 搜索关键词
     */
    val searchKey: String,
) : Serializable