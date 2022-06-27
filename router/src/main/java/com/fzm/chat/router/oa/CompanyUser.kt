package com.fzm.chat.router.oa

import androidx.room.Ignore
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/09/04
 * Description:
 */
data class CompanyUser(
    val depId: String,
    val depName: String,
    val entId: String,
    val entName: String,
    /**
     * 员工 ID
     */
    val id: String,
    /**
     * 员工直属领导 ID
     */
    val leaderId: String?,
    /**
     * 员工姓名
     */
    val name: String,
    /**
     * 员工手机号
     */
    val phone: String?,
    /**
     * 员工短号
     */
    val shortPhone: String?,
    /**
     * 员工邮箱
     */
    val email: String?,
    /**
     * 员工职位
     */
    val position: String?,
    /**
     * 0：团队负责人；1：超级管理员；2:客户管理员；3：普通人员
     */
    val role: Int,
    /**
     * 员工工作地点
     */
    val workplace: String?,
    /**
     * 加入团队时间
     */
    val joinTime: Long,
    /**
     * 是否已激活企业用户（登录过一次）
     */
    val isActivated: Boolean,
) : Serializable {
    /**
     * 企业信息
     */
    @Ignore
    var company: CompanyInfo? = null

    companion object {
        val EMPTY_COMPANY_USER = CompanyUser(
            "", "", "", "",
            "empty", "", "", "", "", "", "",
            0, "", 0L, false
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompanyUser

        if (depId != other.depId) return false
        if (depName != other.depName) return false
        if (entId != other.entId) return false
        if (entName != other.entName) return false
        if (id != other.id) return false
        if (leaderId != other.leaderId) return false
        if (name != other.name) return false
        if (phone != other.phone) return false
        if (shortPhone != other.shortPhone) return false
        if (email != other.email) return false
        if (position != other.position) return false
        if (role != other.role) return false
        if (workplace != other.workplace) return false
        if (joinTime != other.joinTime) return false
        if (isActivated != other.isActivated) return false
        if (company != other.company) return false

        return true
    }

    override fun hashCode(): Int {
        var result = depId.hashCode()
        result = 31 * result + depName.hashCode()
        result = 31 * result + entId.hashCode()
        result = 31 * result + entName.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (leaderId?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + (phone?.hashCode() ?: 0)
        result = 31 * result + (shortPhone?.hashCode() ?: 0)
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + (position?.hashCode() ?: 0)
        result = 31 * result + role
        result = 31 * result + (workplace?.hashCode() ?: 0)
        result = 31 * result + joinTime.hashCode()
        result = 31 * result + isActivated.hashCode()
        result = 31 * result + (company?.hashCode() ?: 0)
        return result
    }
}

val CompanyUser.isEmpty: Boolean get() = this.id == "empty"

val CompanyUser?.hasCompany: Boolean get() = this != null && this.id != "empty"

val CompanyUser?.isAdmin: Boolean get() = if (this != null && this.id != "empty") role <= 1 else false

val CompanyUser?.isOwner: Boolean get() = if (this != null && this.id != "empty") role == 0 else false