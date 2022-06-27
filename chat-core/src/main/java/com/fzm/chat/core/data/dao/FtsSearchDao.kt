package com.fzm.chat.core.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.fzm.chat.core.data.model.ChatMessage

/**
 * @author zhengjy
 * @since 2019/09/20
 * Description:Fts本地搜索接口
 */
@Dao
interface FtsSearchDao {

    @Query("""
        SELECT *, snippet(chat_message_fts, '<​>', '</​>', '…', -1, 64) AS matchSnippet 
        FROM chat_message_fts 
        WHERE searchKey MATCH :keywords ORDER BY datetime DESC
    """)
    // json_extract(msg, '$.content')无法使用，因此需要增加searchKey字段
    suspend fun searchChatLogs(keywords: String): List<ChatMessage>

    @Query("""
        SELECT *, snippet(chat_message_fts, '<​>', '</​>', '…', -1, 64) AS matchSnippet 
        FROM chat_message_fts 
        WHERE channelType=0  AND (target=:id OR `from`=:id) AND searchKey MATCH :keywords ORDER BY datetime DESC
    """)
    suspend fun searchFriendChatLogs(id: String, keywords: String): List<ChatMessage>

    @Query("""
        SELECT *, snippet(chat_message_fts, '<​>', '</​>', '…', -1, 64) AS matchSnippet 
        FROM chat_message_fts 
        WHERE channelType=1 AND `target`=:id AND searchKey MATCH :keywords ORDER BY datetime DESC
    """)
    suspend fun searchGroupChatLogs(id: String, keywords: String): List<ChatMessage>

    @Query("""
        SELECT *, snippet(chat_message_fts, '<​>', '</​>', '…', -1, 64) AS matchSnippet 
        FROM chat_message_fts 
        WHERE msgType=5 AND searchKey MATCH :keywords ORDER BY datetime DESC
    """)
    suspend fun searchChatFiles(keywords: String): List<ChatMessage>

    @Query("""
        SELECT *, snippet(chat_message_fts, '<​>', '</​>', '…', -1, 64) AS matchSnippet 
        FROM chat_message_fts 
        WHERE channelType=0  AND (target=:id OR `from`=:id) AND msgType=5 
        AND searchKey MATCH :keywords ORDER BY datetime DESC
    """)
    suspend fun searchFriendChatFiles(id: String, keywords: String): List<ChatMessage>

    @Query("""
        SELECT *, snippet(chat_message_fts, '<​>', '</​>', '…', -1, 64) AS matchSnippet 
        FROM chat_message_fts 
        WHERE channelType=1 AND `target`=:id AND msgType=5 
        AND searchKey MATCH :keywords ORDER BY datetime DESC
    """)
    suspend fun searchGroupChatFiles(id: String, keywords: String): List<ChatMessage>
}
