package com.fzm.chat.core.data.po

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/12/07
 * Description:
 */
@Entity(tableName = "backup_keypair")
data class BackupKeypair(
    @PrimaryKey(autoGenerate = true)
    val kid: Long,
    val publicKey: String,
    val privateKey: String
) : Serializable