package com.fzm.chat.core.data.po

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fzm.chat.router.oa.CompanyInfo
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/09/05
 * Description:
 */
@Entity(tableName = "company")
data class CompanyInfoPO(
    /**
     * 企业id
     */
    @PrimaryKey
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
) : Serializable

fun CompanyInfo.toPO() =
    CompanyInfoPO(id, avatar, name, description, imServer, nodeServer, oaServer, rootDepId)