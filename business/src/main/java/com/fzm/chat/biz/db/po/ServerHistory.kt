package com.fzm.chat.biz.db.po

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/02/23
 * Description:
 */
@Entity(tableName = "server_history")
data class ServerHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val address: String,
    /**
     * 服务器类型
     * 1：聊天服务器
     * 2：合约服务器
     */
    val type: Int,
) : Serializable