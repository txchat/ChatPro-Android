package com.fzm.chat.core.data.bean

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/31
 * Description:
 */
data class TransactionResult(
    // 其他字段暂时忽略
    val receipt: Receipt
) : Serializable

val TransactionResult.isSuccess get() = receipt.ty == 2

data class Receipt(
    val ty: Int,
    val logs: List<Map<String, Any?>>?
) : Serializable
