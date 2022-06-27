package com.fzm.chat.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fzm.chat.core.data.po.BackupKeypair

/**
 * @author zhengjy
 * @since 2021/12/07
 * Description:
 */
@Dao
interface BackupKeypairDao {

    @Insert
    suspend fun insert(keypair: BackupKeypair): Long

    @Query("SELECT * FROM backup_keypair WHERE publicKey=:publicKey")
    suspend fun getKeypairByPub(publicKey: String): BackupKeypair?

    @Query("SELECT * FROM backup_keypair WHERE kid=:kid")
    suspend fun getKeypairById(kid: Long): BackupKeypair?
}