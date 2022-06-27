package com.fzm.chat.core.rtc.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/07/14
 * Description:
 */
class RTCTaskTO(
    val traceId: Long,
    @SerializedName(value = "RTCType")
    val rtcType: Int,
    /**
     * 主叫者id
     */
    val caller: String?,
    /**
     * 被邀请者id列表
     */
    val invitees: MutableList<String>,
    /**
     * 群聊id
     */
    val groupId: Long,
    val createTime: Long,
    /**
     * 通话超时时长（如45s未接听自动挂断）
     */
    val timeout: Long,
    /**
     * 通话有效期截至时间
     */
    val deadline: Long
) : Serializable {
    fun toRTCTask(): RTCTask = RTCTask(traceId, 0, 0, "", caller, rtcType, invitees, null, groupId, timeout, deadline)
}