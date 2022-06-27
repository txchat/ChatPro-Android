package com.fzm.chat.core.data.po

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/11/29
 * Description:
 */
@Entity(
    tableName = "local_account",
    indices = [
        Index(value = ["address"], name = "index_account_address", unique = true)
    ]
)
data class LocalAccount(
    /**
     * 本地账户地址
     */
    val address: String,
    /**
     * 本地账户地址
     */
    val addressHash: String,
    /**
     * 账户公钥
     */
    val publicKey: String,
    /**
     * 本地账户头像
     */
    val avatar: String,
    /**
     * 本地账户昵称
     */
    val nickname: String,
    /**
     * 本地账户绑定手机
     */
    val phone: String?,
    /**
     * 本地账户绑定邮箱
     */
    val email: String?,
    /**
     * 加密助记词
     */
    var encryptMnemonic: String? = null,
    /**
     * 加密助记词所用的密钥id
     */
    var encryptKeyId: Long = 0L
) : Serializable {
    @PrimaryKey(autoGenerate = true)
    var id = 0L
}