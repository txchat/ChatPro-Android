package com.fzm.chat.biz.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fzm.chat.biz.db.po.LocalNote

/**
 * @author zhengjy
 * @since 2021/08/13
 * Description:
 */
@Dao
interface LocalTxNoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: LocalNote)

    @Query("SELECT * FROM local_tx_note WHERE hash=:hash")
    fun getLocalTxNote(hash: String): LiveData<LocalNote>

}