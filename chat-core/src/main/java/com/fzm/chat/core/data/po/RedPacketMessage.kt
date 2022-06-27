package com.fzm.chat.core.data.po

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/10/12
 * Description:
 */
@Entity(tableName = "packet_message")
data class RedPacketMessage(
    /**
     * 对应的消息msgId
     */
    @PrimaryKey
    val msgId: String,
    /**
     * 红包id
     */
    val packetId: String?,
) : Serializable