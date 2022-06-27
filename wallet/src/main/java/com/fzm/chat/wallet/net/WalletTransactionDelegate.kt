package com.fzm.chat.wallet.net

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.TransactionResult
import com.fzm.chat.core.data.contract.ContractRequest
import com.fzm.chat.core.data.contract.ContractResponse
import com.fzm.chat.core.net.source.TransactionDataSource
import com.fzm.chat.core.net.source.TransactionSource
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * @author zhengjy
 * @since 2021/10/22
 * Description:
 */
internal class WalletTransactionDelegate(private val service: WalletTransactionService) : TransactionDataSource {

    internal interface WalletTransactionService {
        /**
         * 1.创建代扣交易
         * 2.签名交易
         * 3.发送交易
         */
        @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.WALLET_DOMAIN])
        @POST(".")
        suspend fun contractRequest(@Body request: ContractRequest): ContractResponse<String>

        /**
         * 解析交易数据
         */
        @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.WALLET_DOMAIN])
        @POST(".")
        suspend fun decodeRawTransaction(@Body request: ContractRequest): ContractResponse<TransactionSource.TxPack>

        /**
         * 解析交易数据
         */
        @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.WALLET_DOMAIN])
        @POST(".")
        suspend fun getProperFee(@Body request: ContractRequest): ContractResponse<TransactionSource.ProperFee>

        /**
         * 查询交易详情
         */
        @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.WALLET_DOMAIN])
        @POST(".")
        suspend fun queryTransaction(@Body request: ContractRequest): ContractResponse<TransactionResult>
    }

    override suspend fun contractRequest(request: ContractRequest): ContractResponse<String> {
        return service.contractRequest(request)
    }

    override suspend fun decodeRawTransaction(request: ContractRequest): ContractResponse<TransactionSource.TxPack> {
        return service.decodeRawTransaction(request)
    }

    override suspend fun getProperFee(request: ContractRequest): ContractResponse<TransactionSource.ProperFee> {
        return service.getProperFee(request)
    }

    override suspend fun queryTransaction(request: ContractRequest): ContractResponse<TransactionResult> {
        return service.queryTransaction(request)
    }
}