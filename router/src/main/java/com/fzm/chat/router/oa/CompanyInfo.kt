package com.fzm.chat.router.oa

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/09/04
 * Description:
 */
data class CompanyInfo(
    /**
     * 企业id
     */
    val id: String,
    /**
     * 企业头像
     */
    val avatar: String,
    /**
     * 企业名称
     */
    val name: String,
    /**
     * 企业描述
     */
    val description: String,
    /**
     * 企业im服务器
     */
    val imServer: String,
    /**
     * 企业区块链节点
     */
    val nodeServer: String,
    /**
     * 企业oa服务器
     */
    val oaServer: String,
    /**
     * 根部门id
     */
    val rootDepId: String
) : Serializable {

    companion object {
        val EMPTY_COMPANY = CompanyInfo("empty", "", "", "", "", "", "", "")
    }
}

val CompanyInfo.isEmpty: Boolean get() = this.id == "empty"