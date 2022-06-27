package com.fzm.chat.core.data.bean

import com.fzm.chat.core.data.po.GroupUserPO
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/05/13
 * Description:
 */
data class GroupUserTO(
    @SerializedName(value = "memberId")
    val address: String,
    @SerializedName(value = "memberType")
    var role: Int,
    /**
     * 群昵称
     */
    @SerializedName(value = "memberName")
    var nickname: String,
    /**
     * 禁言时间
     */
    @SerializedName(value = "memberMuteTime")
    var muteTime: Long,
) : Serializable {

    data class Wrapper(
        @SerializedName(value = "id")
        val gid: Long,
        val members: List<GroupUserTO>
    ): Serializable
}

fun GroupUserTO.toPO(gid: Long): GroupUserPO {
    return GroupUserPO(gid, address, role, Contact.RELATION, nickname, muteTime)
}