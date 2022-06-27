package com.fzm.chat.wallet.data.bean

import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.core.utils.mul
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/04
 * Description:
 */
data class TransferParams(
    val to: String,
    val amount: Long,
    val fee: Long,
    val note: String?,
    val isToken: Boolean,
    val isWithdraw: Boolean,
    val tokenSymbol: String?,
    val execName: String?,
    val execer: String,
) : Serializable {

    companion object {

        fun createRawTransaction(
            target: String,
            execer: String,
            assets: String,
            amount: Double,
            fee: Double,
            isToken: Boolean,
            note: String?
        ): TransferParams {
            return TransferParams(
                target,
                amount.mul(AppConfig.AMOUNT_SCALE),
                fee.mul(AppConfig.AMOUNT_SCALE),
                note,
                isToken,
                false,
                if (isToken) assets else null,
                null,
                execer
            )
        }
    }
}