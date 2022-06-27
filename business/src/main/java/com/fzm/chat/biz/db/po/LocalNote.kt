package com.fzm.chat.biz.db.po

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/13
 * Description:
 */
@Entity(tableName = "local_tx_note")
data class LocalNote(
    @PrimaryKey
    val hash: String,
    val note: String
) : Serializable