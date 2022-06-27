package com.fzm.chat.core.repo

import com.fzm.chat.core.net.api.RTCService
import com.fzm.chat.core.rtc.data.RTCRoomParams
import com.fzm.chat.core.rtc.data.RTCTaskTO
import com.zjy.architecture.data.Result
import com.zjy.architecture.ext.apiCall

/**
 * @author zhengjy
 * @since 2021/07/14
 * Description:
 */
class RTCRepository(private val service: RTCService) {

    /**
     * 与某人发起音视频通话
     *
     * @param targetId  对方id
     * @param rtcType   音视频类型
     */
    suspend fun call(targetId: String, rtcType: Int): Result<RTCTaskTO> {
        return apiCall { service.call(mapOf("invitees" to listOf(targetId), "RTCType" to rtcType)) }
    }

    suspend fun groupCall(groupId: Long, invitees: List<String>, rtcType: Int): Result<RTCTaskTO> {
        return apiCall { service.call(mapOf("groupId" to groupId, "invitees" to invitees, "RTCType" to rtcType)) }
    }

    suspend fun checkCall(taskId: Long) : Result<RTCTaskTO> {
        return apiCall { service.checkCall(mapOf("traceId" to taskId)) }
    }

    suspend fun acceptCall(taskId: Long) : Result<RTCRoomParams> {
        return apiCall { service.handleCall(mapOf("traceId" to taskId, "answer" to true)) }
    }

    suspend fun stopCall(taskId: Long) : Result<RTCRoomParams> {
        return apiCall { service.handleCall(mapOf("traceId" to taskId, "answer" to false)) }
    }

    suspend fun lineBusy(taskId: Long) : Result<Any> {
        return apiCall { service.lineBusy(mapOf("traceId" to taskId)) }
    }
}