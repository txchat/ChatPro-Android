package com.fzm.chat.router.main

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/13
 * Description:
 */
data class SimpleTx(
    val hash: String,
    /**
     * 交易时间
     */
    val blockTime: Long,
    /**
     * 矿工费
     */
    val fee: String,
    /**
     * 转账地址
     */
    val from: String,
    /**
     * 收款地址
     */
    val to: String,
    /**
     * 区块高度
     */
    val height: Int,
    /**
     * 交易状态
     * 失败：-1，确认中：0，成功：1
     */
    var status: String,
    /**
     * 交易金额
     */
    val value: String,
    /**
     * 上链备注
     */
    val note: String?,
) : Serializable {
    companion object {
        const val FAIL = "-1"
        const val PENDING = "0"
        const val SUCCESS = "1"
    }
}

val SimpleTx.isSuccess: Boolean
    get() = status == SimpleTx.SUCCESS

val SimpleTx.isPending: Boolean
    get() = status == SimpleTx.PENDING

val SimpleTx.isFail: Boolean
    get() = status == SimpleTx.FAIL