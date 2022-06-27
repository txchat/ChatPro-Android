package com.fzm.chat.core.rtc.calling

import android.view.View
import com.fzm.chat.core.rtc.data.RTCTask
import kotlinx.coroutines.Job

/**
 * @author zhengjy
 * @since 2021/07/07
 * Description:
 */
interface RTCCalling {

    companion object {
        const val TYPE_UNKNOWN = 0

        /**
         * 音频通话
         */
        const val TYPE_AUDIO_CALL = 1

        /**
         * 视频通话
         */
        const val TYPE_VIDEO_CALL = 2

        /**
         * 被叫方
         */
        const val TYPE_BEING_CALLED = 1

        /**
         * 主叫方
         */
        const val TYPE_CALL = 2

        /**
         * 私聊通话，进入房间后最长等待对方连接时间
         * 即：进入房间后，如果对方ENTER_ROOM_TIMEOUT时间后还不连接，则中断会话
         */
        const val ENTER_ROOM_TIMEOUT = 5000L
    }

    /**
     * 给task加锁，防止拨打和接听同时进行出现问题
     */
    suspend fun syncTask(owner: Any? = null, block: suspend () -> Unit)

    /**
     * 获取当前通话task
     */
    fun getCurrentTask(): RTCTask

    /**
     * 增加回调接口
     *
     * @param delegate 上层可以通过回调监听事件
     */
    fun addDelegate(delegate: RTCCallingDelegate)

    /**
     * 移除回调接口
     *
     * @param delegate 需要移除的监听器
     */
    fun removeDelegate(delegate: RTCCallingDelegate)

    /**
     * C2C邀请通话，被邀请方会收到 [RTCCallingDelegate.onInvited] 的回调
     * 如果当前处于通话中，可以调用该函数以邀请第三方进入通话
     *
     * @param userId 被邀请方
     * @param type   1-语音通话，2-视频通话
     */
    fun call(userId: String, type: Int)

    /**
     * IM群组邀请通话，被邀请方会收到 [RTCCallingDelegate.onInvited] 的回调
     * 如果当前处于通话中，可以继续调用该函数继续邀请他人进入通话，同时正在通话的用户会收到 [RTCCallingDelegate.onGroupCallInviteeListUpdate] 的回调
     *
     * @param userIdList 邀请列表
     * @param type       1-语音通话，2-视频通话
     * @param groupId    IM群组ID
     */
    fun groupCall(userIdList: List<String>, type: Int, groupId: String)

    /**
     * 当您作为被邀请方收到 [RTCCallingDelegate.onInvited] 的回调时，可以调用该函数接听来电
     */
    fun accept()

    /**
     * 当您作为被邀请方收到 [RTCCallingDelegate.onInvited] 的回调时，可以调用该函数拒绝来电
     */
    fun reject(): Job

    /**
     * 当您处于通话中，可以调用该函数结束通话
     */
    fun hangup(): Job

    /**
     * 当您收到 onUserVideoAvailable 回调时，可以调用该函数将远端用户的摄像头数据渲染到指定的TXCloudVideoView中
     *
     * @param userId           远端用户id
     * @param remoteView        远端用户数据将渲染到该view中
     */
    fun startRemoteView(userId: String?, remoteView: View?)

    /**
     * 当您收到 onUserVideoAvailable 回调为false时，可以停止渲染数据
     *
     * @param userId 远端用户id
     */
    fun stopRemoteView(userId: String?)

    /**
     * 切换视频聊天使用的摄像头
     *
     * @return  true表示切换为前置，false表示切换为后置
     */
    fun switchCamera(): Boolean

    /**
     * 将视频聊天切换为语音聊天
     *
     */
    fun switchToAudioCall()

    /**
     * 是否静音mic
     *
     * @param isMute true:麦克风关闭 false:麦克风打开
     */
    fun setMicMute(isMute: Boolean)

    /**
     * 是否开启免提
     *
     * @param isHandsFree true:开启免提 false:关闭免提
     */
    fun setHandsFree(isHandsFree: Boolean)

    /**
     * 释放资源
     */
    fun release()
}