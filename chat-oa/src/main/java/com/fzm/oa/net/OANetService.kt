package com.fzm.oa.net

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.router.oa.AllCompanyUsers
import com.fzm.chat.router.oa.CompanyInfo
import com.fzm.chat.router.oa.CompanyUser
import com.zjy.architecture.net.HttpResult
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * @author zhengjy
 * @since 2021/09/04
 * Description:
 */
@JvmSuppressWildcards
interface OANetService {

    /**
     * 获取企业用户信息
     *
     * @param map   id：用户地址
     */
    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.OA_DOMAIN])
    @POST("v1/staff/get-staff")
    suspend fun getStaffInfo(@Body map: Map<String, Any>): HttpResult<CompanyUser>

    /**
     * 获取企业用户信息
     *
     */
    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.OA_DOMAIN])
    @POST("v1/department/get-all-staffs")
    suspend fun getCompanyUsers(@Body map: Map<String, Any>): HttpResult<AllCompanyUsers>

    /**
     * 获取企业信息
     *
     * @param map   id：企业id
     */
    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.OA_DOMAIN])
    @POST("v1/enterprise/info")
    suspend fun getCompanyInfo(@Body map: Map<String, Any>): HttpResult<CompanyInfo>
}