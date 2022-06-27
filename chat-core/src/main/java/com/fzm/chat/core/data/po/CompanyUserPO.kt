package com.fzm.chat.core.data.po

import androidx.room.Entity
import androidx.room.Index
import com.fzm.chat.router.oa.CompanyUser
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/09/05
 * Description:
 */
@Entity(
    tableName = "company_user",
    primaryKeys = ["entId", "id"],
    indices = [
        Index(value = ["id"], unique = true)
    ]
)
data class CompanyUserPO(
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
) : Serializable

fun CompanyUser.toPO(): CompanyUserPO = CompanyUserPO(
    depId, depName, entId, entName, id, leaderId, name,
    phone, shortPhone, email, position, role, workplace, joinTime, isActivated
)