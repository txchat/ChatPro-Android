package com.fzm.chat.core.data.dao

import androidx.room.*
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.data.po.MessagePO
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/28
 * Description:
 */
@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(messages: List<MessagePO>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessagePO)

    @Query("SELECT * FROM chat_message WHERE logId=:logId")
    suspend fun findMessage(logId: Long): ChatMessage?

    @Query(
        """
        SELECT c.*, u.num AS focusNum FROM chat_message c
        LEFT JOIN view_message_focus u
        ON c.logId=u.logId
        WHERE channelType=:channelType AND (`from`=:address OR target=:address) AND c.datetime < :datetime
        ORDER BY datetime DESC LIMIT :count
        
    """
    )
    suspend fun getMessageByTime(address: String, channelType: Int, datetime: Long, count: Int): List<ChatMessage>?

    @Query(
        """
        SELECT c.*, u.num AS focusNum FROM chat_message c
        LEFT JOIN view_message_focus u
        ON c.logId=u.logId
        WHERE channelType=:channelType AND (`from`=:address OR target=:address) AND c.datetime >= (SELECT datetime FROM chat_message WHERE msgId=:msgId)
        ORDER BY datetime DESC
    """
    )
    suspend fun getMessageByMsgId(address: String, channelType: Int, msgId: String): List<ChatMessage>?

    @Query(
        """
        SELECT * FROM chat_message 
        WHERE channelType=:channelType AND (`from`=:address OR target=:address) AND datetime < :datetime AND msgType=:msgType
        ORDER BY datetime DESC LIMIT :count
    """
    )
    suspend fun getOneTypeMessage(address: String, channelType: Int, datetime: Long, msgType: Int, count: Int): List<ChatMessage>?

    @Query(
        """
        SELECT * FROM chat_message 
        WHERE channelType=:channelType AND (`from`=:address OR target=:address) AND datetime < :datetime AND (msgType=:msgType1 OR msgType=:msgType2)
        ORDER BY datetime DESC LIMIT :count
    """
    )
    suspend fun getTwoTypeMessage(address: String, channelType: Int, datetime: Long, msgType1: Int, msgType2: Int, count: Int): List<ChatMessage>?

    @Query("UPDATE chat_message SET state=:state WHERE msgId=:msgId")
    suspend fun updateMsgState(msgId: String, state: Int)

    @Query("UPDATE chat_message SET state=:state WHERE logId in (:logId)")
    suspend fun updateMsgState(logId: List<Long>, state: Int): Int

    @Query("UPDATE chat_message SET logId=:logId, datetime=:datetime, state=:state WHERE msgId=:msgId")
    suspend fun updateMessage(msgId: String, logId: Long, datetime: Long, state: Int)

    @Query("UPDATE chat_message SET msg=:content WHERE msgId=:msgId")
    suspend fun updateMessageContent(msgId: String, content: MessageContent)

    @Query("SELECT * FROM chat_message WHERE logId in (:logId)")
    suspend fun getMessageByLogId(logId: List<Long>): List<ChatMessage>

    @Query("DELETE FROM chat_message WHERE msgId=:msgId AND logId=:logId AND channelType=:channelType")
    suspend fun deleteMsg(msgId: String, logId: Long, channelType: Int)

    @Query("DELETE FROM message_focus_user WHERE logId=:logId")
    suspend fun deleteFocusInfo(logId: Long)

    @Transaction
    suspend fun deleteMessage(msgId: String, logId: Long, channelType: Int) {
        deleteMsg(msgId, logId, channelType)
        if (logId != 0L) {
            deleteFocusInfo(logId)
        }
    }

    @Query("SELECT logId, msgId FROM chat_message WHERE (`from`=:address OR target=:address) AND channelType=:channelType")
    suspend fun findContactMessage(address: String, channelType: Int): List<MsgId>?

    @Transaction
    suspend fun deleteContactMessage(address: String, channelType: Int) {
        val list = findContactMessage(address, channelType)
        list?.forEach {
            deleteMessage(it.msgId, it.logId, channelType)
        }
    }

    class MsgId(
        val logId: Long,
        val msgId: String
    ) : Serializable
}