package com.fzm.oa.impl

import com.fzm.chat.router.oa.AllCompanyUsers
import com.fzm.chat.router.oa.CompanyInfo
import com.fzm.chat.router.oa.CompanyUser
import com.fzm.oa.OADataSource
import com.fzm.oa.net.OANetService
import com.zjy.architecture.data.Result
import com.zjy.architecture.ext.apiCall

/**
 * @author zhengjy
 * @since 2021/09/04
 * Description:
 */
class NetOADataSource(private val service: OANetService) : OADataSource {

    override suspend fun getCompanyByAddress(address: String?): Result<CompanyUser> {
        val map = address?.let { mapOf("id" to it) } ?: emptyMap<String, Any>()
        return apiCall { service.getStaffInfo(map) }
    }

    override suspend fun getCompanyUsers(
        entId: String,
        id: String,
        isDirect: Boolean
    ): Result<AllCompanyUsers> {
        return apiCall {
            service.getCompanyUsers(mapOf("entId" to entId, "id" to id, "isDirect" to isDirect))
        }
    }

    override suspend fun getCompanyInfo(id: String): Result<CompanyInfo> {
        return apiCall { service.getCompanyInfo(mapOf("id" to id)) }
    }
}