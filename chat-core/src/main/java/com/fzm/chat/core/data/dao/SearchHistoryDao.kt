package com.fzm.chat.core.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fzm.chat.core.data.model.SearchHistory

/**
 * @author zhengjy
 * @since 2019/09/17
 * Description:
 */
@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY searchTime DESC LIMIT 10")
    fun getSearchHistory(): LiveData<List<SearchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SearchHistory)

    @Query("DELETE FROM search_history WHERE keywords=:key")
    suspend fun deleteHistory(key: String)

    @Query("DELETE FROM search_history")
    suspend fun deleteAllHistory()
}