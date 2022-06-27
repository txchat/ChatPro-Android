package com.fzm.oa

import com.fzm.chat.router.oa.AllCompanyUsers
import com.fzm.chat.router.oa.CompanyInfo
import com.fzm.chat.router.oa.CompanyUser
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2021/09/04
 * Description:
 */
interface OADataSource {

    /**
     * 通过地址获取公司职员信息
     */
    suspend fun getCompanyByAddress(address: String?): Result<CompanyUser>

    /**
     * 获取公司部门下的职员信息
     *
     * @param entId     企业id
     * @param id        部门id
     * @param isDirect  是否只返回部门直属成员
     */
    suspend fun getCompanyUsers(entId: String, id: String, isDirect: Boolean): Result<AllCompanyUsers>

    /**
     * 通过公司id获取公司信息
     */
    suspend fun getCompanyInfo(id: String): Result<CompanyInfo>
}