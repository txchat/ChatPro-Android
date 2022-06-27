package com.fzm.chat.core.data.bean

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/02/07
 * Description:
 */
data class TransactionParams(
        var addr: String?,
        var privkey: String?,
        var txHex: String,
        var expire: String?,
        var index: Int,
        var token: String?,
        var fee: Long,
        var newToAddr: String?
) : Serializable {

    companion object {
        @JvmStatic
        fun createSign(private: String, txHex: String, fee: Long): TransactionParams {
            return TransactionParams(null, private, txHex, "1h", 2, null, fee, null)
        }

        @JvmStatic
        fun createNoBalanceTx(private: String, txHex: String): TransactionParams {
            return TransactionParams(null, private, txHex, "1h", 0, null, 0L, null)
        }
    }
}