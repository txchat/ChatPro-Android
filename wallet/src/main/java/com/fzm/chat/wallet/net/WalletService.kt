package com.fzm.chat.wallet.net

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.contract.ContractRequest
import com.fzm.chat.core.data.contract.ContractResponse
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * @author zhengjy
 * @since 2021/08/04
 * Description:
 */
interface WalletService {

    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.WALLET_DOMAIN])
    @POST(".")
    suspend fun transfer(@Body request: ContractRequest): ContractResponse<String>
}