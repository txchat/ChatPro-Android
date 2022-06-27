package com.fzm.chat.redpacket.net

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.contract.ContractRequest
import com.fzm.chat.core.data.contract.ContractResponse
import com.fzm.chat.redpacket.data.bean.*
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * @author zhengjy
 * @since 2021/08/25
 * Description:
 */
interface RedPacketService {

    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.WALLET_DOMAIN])
    @POST(".")
    suspend fun getRedPacketInfo(@Body request: ContractRequest): ContractResponse<RedPacketInfo.Wrapper>

    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.WALLET_DOMAIN])
    @POST(".")
    suspend fun getReceiveDetail(@Body request: ContractRequest): ContractResponse<ReceiveInfo.Wrapper>

    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.WALLET_DOMAIN])
    @POST(".")
    suspend fun getReceiveRecords(@Body request: ContractRequest): ContractResponse<ReceiveInfo.Wrapper>

    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.WALLET_DOMAIN])
    @POST(".")
    suspend fun getStatisticRecords(@Body request: ContractRequest): ContractResponse<StatisticInfo>
}