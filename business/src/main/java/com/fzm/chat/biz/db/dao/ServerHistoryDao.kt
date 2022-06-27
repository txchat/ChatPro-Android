package com.fzm.chat.biz.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fzm.chat.biz.db.po.ServerHistory

/**
 * @author zhengjy
 * @since 2021/02/23
 * Description:
 */
@Dao
interface ServerHistoryDao {

    @Insert
    suspend fun insert(history: ServerHistory)

    @Query("UPDATE server_history SET name=:name, address=:address WHERE id=:id")
    suspend fun update(id: Int, name: String, address: String)

    @Query("DELETE FROM server_history WHERE id=:id")
    suspend fun delete(id: Int)

    @Query("SELECT * from server_history")
    suspend fun getServerHistory(): List<ServerHistory>

    @Query("SELECT * from server_history WHERE type=1 ORDER BY id DESC LIMIT 10")
    suspend fun getChatServerHistory(): List<ServerHistory>

    @Query("SELECT * from server_history WHERE type=2 ORDER BY id DESC LIMIT 10")
    suspend fun getContractServerHistory(): List<ServerHistory>
}