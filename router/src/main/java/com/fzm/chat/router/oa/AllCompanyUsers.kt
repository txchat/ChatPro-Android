package com.fzm.chat.router.oa

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/12/29
 * Description:
 */
data class AllCompanyUsers(
    /**
     * 部门负责人信息
     */
    val leader: CompanyUser?,
    /**
     * 部门成员
     */
    val staffList: List<CompanyUser>
) : Serializable