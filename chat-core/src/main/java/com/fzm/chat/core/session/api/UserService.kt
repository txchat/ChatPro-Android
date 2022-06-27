package com.fzm.chat.core.session.api

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.UserResult
import com.fzm.chat.core.data.contract.ContractRequest
import com.fzm.chat.core.data.contract.ContractResponse
import me.jessyan.retrofiturlmanager.RetrofitUrlManager.DOMAIN_NAME_HEADER
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * @author zhengjy
 * @since 2020/12/09
 * Description:
 */
interface UserService {

    /**
     * 获取用户信息
     */
    @Headers(value = [DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
    @POST(".")
    suspend fun getUser(@Body request: ContractRequest): ContractResponse<UserResult>

    /**
     * 更新自己的信息
     */
    @Headers(value = [DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
    @POST(".")
    suspend fun updateUser(@Body request: ContractRequest): ContractResponse<String>

    /**
     * 修改分组列表
     *
     * @param request 合约请求参数
     */
    @Headers(value = [DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
    @POST(".")
    suspend fun modifyServerGroup(@Body request: ContractRequest): ContractResponse<String>
}