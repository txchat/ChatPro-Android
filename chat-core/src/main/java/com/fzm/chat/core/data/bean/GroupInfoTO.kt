package com.fzm.chat.core.data.bean

import com.fzm.chat.core.data.po.GroupInfo
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/05/17
 * Description:
 */
data class GroupInfoTO(
    @SerializedName(value = "id")
    val gid: Long,
    val avatar: String,
    var name: String,
    val publicName: String,
    val key: String?,
    val groupType: Int,
    /**
     * 群短 id(仅供展示, 后面可能可以供搜索加群使用)
     */
    val markId: String,
    /**
     * 群主
     */
    val owner: GroupUserTO,
    /**
     * 自己在群里的信息
     */
    val person: GroupUserTO?,
    /**
     * 群成员信息
     */
    val members: List<GroupUserTO>?,
    /**
     * 群人数
     */
    val memberNum: Int,
    /**
     * 群内管理员人数
     */
    var adminNum: Int,
    /**
     * 群内禁言人数
     */
    var muteNum: Int,
    /**
     * 群人数上限
     */
    val maximum: Int,
    /**
     * 群状态，0=正常 1=封禁 2=解散
     */
    val status: Int,
    /**
     * 是否允许群内加好友，0=允许加好友，1=禁止加好友
     */
    val friendType: Int,
    /**
     * 加群方式，0=允许任何方式加群，1=群主和管理员邀请加群
     */
    val joinType: Int,
    /**
     * 禁言， 0=所有人可以发言， 1=群主和管理员可以发言
     */
    val muteType: Int,
) : Serializable {

    data class Wrapper(
        val groups: List<GroupInfoTO>
    ) : Serializable

    fun toGroupInfo(server: Server, flag: Int): GroupInfo {
        var relation = 0
        if (person != null) {
            relation = Contact.RELATION
        }
        return GroupInfo(gid, avatar, name, publicName, key, groupType, server, flag or relation, markId, owner, person, members, memberNum, adminNum, muteNum, maximum, status, friendType, joinType, muteType)
    }
}