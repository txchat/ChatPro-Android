package com.fzm.chat.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fzm.chat.core.data.po.MessageFocusUser

/**
 * @author zhengjy
 * @since 2021/12/27
 * Description:
 */
@Dao
interface FocusUserDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(focusUser: MessageFocusUser)

    @Query("SELECT * FROM message_focus_user WHERE logId=:logId ORDER BY datetime DESC")
    suspend fun getFocusedUsers(logId: Long): List<MessageFocusUser>

    @Query("SELECT count(*) FROM message_focus_user WHERE logId=:logId")
    suspend fun getMessageFocusNum(logId: Long): Int

    @Query("SELECT count(datetime) FROM message_focus_user WHERE logId=:logId AND uid=:address")
    suspend fun focusCount(logId: Long, address: String?): Int

    suspend fun hasFocused(logId: Long, address: String?): Boolean = focusCount(logId, address) > 0
}