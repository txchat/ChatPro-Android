package com.fzm.chat.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.po.RedPacketMessage

/**
 * @author zhengjy
 * @since 2021/10/12
 * Description:
 */
@Dao
interface RedPacketDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(data: RedPacketMessage)

    @Query("""
        SELECT m.* FROM packet_message p
        LEFT JOIN chat_message m
        ON p.msgId=m.msgId
        WHERE p.packetId=:packetId
    """)
    suspend fun getMessageByPacket(packetId: String): ChatMessage?
}