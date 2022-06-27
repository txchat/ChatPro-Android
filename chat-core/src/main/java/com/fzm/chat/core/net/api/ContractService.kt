package com.fzm.chat.core.net.api

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.ServerGroupInfo
import com.fzm.chat.core.data.bean.UserAddress
import com.fzm.chat.core.data.bean.UserResult
import com.fzm.chat.core.data.contract.ContractRequest
import com.fzm.chat.core.data.contract.ContractResponse
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.DOMAIN_NAME_HEADER
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * @author zhengjy
 * @since 2020/02/06
 * Description:
 */
interface ContractService {

    /**
     * 获取用户信息
     */
    @Headers(value = [DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
    @POST(".")
    suspend fun getUser(@Body request: ContractRequest): ContractResponse<UserResult>

    /**
     * 获取好友列表
     *
     * @param request 合约请求参数
     */
    @Headers(value = [DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
    @POST(".")
    suspend fun getFriendList(@Body request: ContractRequest): ContractResponse<UserAddress.Wrapper>

    /**
     * 获取黑名单列表
     *
     * @param request 合约请求参数
     */
    @Headers(value = [DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
    @POST(".")
    suspend fun getBlockList(@Body request: ContractRequest): ContractResponse<UserAddress.Wrapper>

    /**
     * 获取分组列表
     *
     * @param request 合约请求参数
     */
    @Headers(value = [DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
    @POST(".")
    suspend fun getServerGroup(@Body request: ContractRequest): ContractResponse<ServerGroupInfo.Wrapper>

    /**
     * 修改好友
     *
     * @param request 合约请求参数
     */
    @Headers(value = [DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
    @POST(".")
    suspend fun modifyFriend(@Body request: ContractRequest): ContractResponse<String>

    /**
     * 修改黑名单
     *
     * @param request 合约请求参数
     */
    @Headers(value = [DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
    @POST(".")
    suspend fun modifyBlock(@Body request: ContractRequest): ContractResponse<String>

    /**
     * 修改分组列表
     *
     * @param request 合约请求参数
     */
    @Headers(value = [DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
    @POST(".")
    suspend fun modifyServerGroup(@Body request: ContractRequest): ContractResponse<String>
}