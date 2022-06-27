package com.fzm.oa.impl

import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.po.toPO
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.oa.AllCompanyUsers
import com.fzm.chat.router.oa.CompanyInfo
import com.fzm.chat.router.oa.CompanyUser
import com.fzm.oa.OADataSource
import com.fzm.oa.data.CompanyNotExist
import com.fzm.oa.data.UserNotBindPhone
import com.fzm.oa.data.UserNotExist
import com.zjy.architecture.data.Result
import com.zjy.architecture.exception.ApiException

/**
 * @author zhengjy
 * @since 2021/09/04
 * Description:
 */
class OARepository(
    private val dataSource: OADataSource,
    private val delegate: LoginDelegate
) : OADataSource by dataSource {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    override suspend fun getCompanyByAddress(address: String?): Result<CompanyUser> {
        val user = dataSource.getCompanyByAddress(address)
        if (!user.isSucceed()) {
            val error = user.error()
            if (error is ApiException && (error.code == UserNotExist || error.code == UserNotBindPhone)) {
                database.companyUserDao().deleteByAddress(address ?: delegate.getAddress() ?: "")
            }
        }
        return user
    }

    override suspend fun getCompanyUsers(entId: String, id: String, isDirect: Boolean): Result<AllCompanyUsers> {
        val users = dataSource.getCompanyUsers(entId, id, isDirect)
        if (users.isSucceed()) {
            database.companyUserDao().insert(users.data().staffList.map { it.toPO() })
        }
        return users
    }

    override suspend fun getCompanyInfo(id: String): Result<CompanyInfo> {
        val info = dataSource.getCompanyInfo(id)
        if (!info.isSucceed()) {
            val error = info.error()
            if (error is ApiException && error.code == CompanyNotExist) {
                database.companyDao().delete(id)
            }
        }
        return info
    }
}