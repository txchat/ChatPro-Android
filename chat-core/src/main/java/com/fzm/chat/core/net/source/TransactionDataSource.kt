package com.fzm.chat.core.net.source

import com.fzm.chat.core.data.bean.TransactionResult
import com.fzm.chat.core.data.contract.ContractRequest
import com.fzm.chat.core.data.contract.ContractResponse

/**
 * @author zhengjy
 * @since 2021/10/22
 * Description:
 */
interface TransactionDataSource {

    /**
     * 1.创建代扣交易
     * 2.签名交易
     * 3.发送交易
     */
    suspend fun contractRequest(request: ContractRequest): ContractResponse<String>

    /**
     * 解析交易数据
     */
    suspend fun decodeRawTransaction(request: ContractRequest): ContractResponse<TransactionSource.TxPack>

    /**
     * 解析交易数据
     */
    suspend fun getProperFee(request: ContractRequest): ContractResponse<TransactionSource.ProperFee>

    /**
     * 查询交易详情
     */
    suspend fun queryTransaction(request: ContractRequest): ContractResponse<TransactionResult>
}