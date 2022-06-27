package com.fzm.chat.core.rtc.data

import com.fzm.chat.core.rtc.calling.RTCCalling.Companion.TYPE_AUDIO_CALL
import com.fzm.chat.core.rtc.calling.RTCCalling.Companion.TYPE_VIDEO_CALL
import java.io.Serializable
import java.util.concurrent.atomic.AtomicReference

/**
 * @author zhengjy
 * @since 2021/07/08
 * Description:
 */
data class RTCTask(
    /**
     * 任务id
     */
    val id: Long,
    /**
     * 腾讯云音视频应用 ID
     */
    var sdkAppId: Int,
    /**
     * 通话房间id
     */
    var roomId: Int,
    /**
     * 用户签名，有了签名才能使用TRTC
     */
    var signature: String,
    /**
     * 主叫者id
     */
    val caller: String?,
    /**
     * 通话类型：[TYPE_AUDIO_CALL]，[TYPE_VIDEO_CALL]
     */
    var rtcType: Int,
    /**
     * 被邀请人列表
     */
    val userList: MutableList<String>,
    /**
     * privateKey可以指定用户进入某个房间的权限
     */
    var privateMapKey: String? = null,
    /**
     * 群id，不是群聊则为0
     */
    val groupId: Long = 0L,
    /**
     * 通话超时时长（如45s未接听自动挂断）
     */
    val timeout: Long = 0L,
    /**
     * 通话接通前的超时时间
     */
    val deadline: Long = 0L,
    /**
     * 麦克风静音
     */
    var micMute: Boolean = false,
    /**
     * 免提
     */
    var handsFree: Boolean = false,
    /**
     * 持续时长
     */
    var duration: Long = 0
) : Serializable, Cloneable {

    companion object {
        val EMPTY = RTCTask(0L, 0, 0, "", "", 0, mutableListOf())
    }

    /**
     * 是否成功建立过通话连接
     */
    var established: Boolean = false

    val status = AtomicReference(Status.INITIAL)

    fun waiting() = status.compareAndSet(Status.INITIAL, Status.WAITING)

    fun connected() {
        established = true
        status.compareAndSet(Status.WAITING, Status.ON_CALLING)
    }

    fun reject() = status.compareAndSet(Status.WAITING, Status.END)

    fun end() = status.set(Status.END)

    enum class Status {
        /**
         * 初始状态
         */
        INITIAL,

        /**
         * 等待接听或者等待对方接听状态
         */
        WAITING,

        /**
         * 正在通话中
         */
        ON_CALLING,

        /**
         * 通话结束
         */
        END
    }

    public override fun clone(): RTCTask {
        return RTCTask(id, sdkAppId, roomId, signature, caller, rtcType, userList, privateMapKey,
            groupId, timeout, deadline, micMute, handsFree, duration)
    }
}

val RTCTask.isOnCalling get() = status.get() == RTCTask.Status.ON_CALLING

val RTCTask.isWaiting get() = status.get() == RTCTask.Status.WAITING

val RTCTask.isBusy get() = status.get() == RTCTask.Status.ON_CALLING || status.get() == RTCTask.Status.WAITING

val RTCTask.isAudio get() = rtcType == TYPE_AUDIO_CALL

val RTCTask.isVideo get() = rtcType == TYPE_VIDEO_CALL

val RTCTask.isGroup get() = groupId != 0L