package com.fzm.chat.router.oa

import android.content.Context

/**
 * @author zhengjy
 * @since 2021/09/04
 * Description:
 */
interface OAService {

    /**
     * 加载团队成员信息
     */
    suspend fun loadCompanyUsers()

    /**
     * 获取某个地址关联的企业信息
     */
    suspend fun getCompanyUser(address: String?): CompanyUser?

    /**
     * 打开团队创建页面
     */
    fun openCreateCompanyPage()

    /**
     * 打开团队架构页面
     */
    fun openCompanyUserList(context: Context?)

    /**
     * 打开团队管理
     */
    fun openCompanyManagement(context: Context?)

    /**
     * 打开申请加入团队界面
     */
    fun applyJoinTeamPage(context: Context?, map: Map<String, String?>)

    /**
     * 选择团队成员
     */
    fun selectCompanyUserUrl(map: Map<String, String>) : String

    /**
     * 打开OKR页面
     */
    fun openOKRPage(context: Context?)
}