package com.fzm.oa.impl

import android.content.Context
import android.net.Uri
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.base.FunctionModules
import com.fzm.chat.biz.base.enableOA
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.po.toPO
import com.fzm.chat.core.data.transaction
import com.fzm.chat.core.logic.ServerManager
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.oa.CompanyUser
import com.fzm.chat.router.oa.OAModule
import com.fzm.chat.router.oa.OAService
import com.fzm.oa.OAConfig
import com.zjy.architecture.di.rootScope

/**
 * @author zhengjy
 * @since 2021/09/04
 * Description:
 */
@Route(path = OAModule.SERVICE)
class OAServiceImpl : OAService, IProvider {

    private lateinit var mContext: Context
    private val repository by rootScope.inject<OARepository>()
    private val delegate by rootScope.inject<LoginDelegate>()

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    override suspend fun loadCompanyUsers() {
        val func: suspend () -> Unit = {
            delegate.companyUser.value?.company?.apply {
                repository.getCompanyUsers(id, rootDepId, false)
            }
        }
        if (FunctionModules.hasInit) {
            func()
        } else {
            FunctionModules.awaitInit { func() }
        }
    }

    override suspend fun getCompanyUser(address: String?): CompanyUser? {
        return if (FunctionModules.hasInit) {
            getCompanyUserInner(address)
        } else {
            FunctionModules.awaitInit {
                getCompanyUserInner(address)
            }
        }
    }

    private suspend fun getCompanyUserInner(address: String?): CompanyUser? {
        if (!FunctionModules.enableOA) {
            return null
        }
        return repository.getCompanyByAddress(address).dataOrNull()?.let { user ->
            val result = repository.getCompanyInfo(user.entId)
            return if (result.isSucceed()) {
                if (user.isActivated) {
                    database.transaction {
                        database.companyUserDao().insert(user.toPO())
                        database.companyDao().insert(result.data().toPO())
                    }
                }
                // 如果是自己的企业用户信息，则修改对应server
                if (address == null || address == AppPreference.ADDRESS) {
                    ServerManager.changeContractServer(result.data().nodeServer)
                    ServerManager.changeLocalChatServer(result.data().imServer)
                }
                user.company = result.data()
                user
            } else {
                // 企业信息请求失败，则user也返回空
                null
            }
        }
    }

    override fun openCreateCompanyPage() {
        ARouter.getInstance().build(OAModule.WEB)
            .withString("url", "${OAConfig.OA_WEB}/team/create-team")
            .navigation()
    }

    override fun openCompanyUserList(context: Context?) {
        ARouter.getInstance().build(OAModule.WEB)
            .withString("url", "${OAConfig.OA_WEB}/team/team-frame")
            .withTransition(0, 0)
            .navigation(context)
    }

    override fun openCompanyManagement(context: Context?) {
        ARouter.getInstance().build(OAModule.WEB)
            .withString("url", "${OAConfig.OA_WEB}/team/team-management")
            .withTransition(0, 0)
            .navigation(context)
    }

    override fun applyJoinTeamPage(context: Context?, map: Map<String, String?>) {
        val uri = Uri.parse("${OAConfig.OA_WEB}/team/join-team")
            .buildUpon()
            .appendQueryParameter("entId", map["id"] ?: "")
            .appendQueryParameter("server", map["server"] ?: "")
            .appendQueryParameter("inviterId", map["inviterId"] ?: "")
            .build()
        ARouter.getInstance().build(OAModule.WEB)
            .withString("url", uri.toString())
            .navigation(context)
    }

    override fun selectCompanyUserUrl(map: Map<String, String>) : String {
        val uri = Uri.parse("${OAConfig.OA_WEB}/team/selector")
            .buildUpon()
            .appendQueryParameter("action", map["action"] ?: "")
            .appendQueryParameter("excludeSelf", map["excludeSelf"] ?: "true")
            .appendQueryParameter("type", map["type"] ?: "")
            .appendQueryParameter("gid", map["gid"] ?: "")
            .build()
        return uri.toString()
    }

    override fun openOKRPage(context: Context?) {
        ARouter.getInstance().build(OAModule.WEB)
            .withString("url", OAConfig.OKR_WEB)
            .withTransition(0, 0)
            .navigation(context)
    }

    override fun init(context: Context) {
        mContext = context
    }
}