package com.fzm.rtc.impl

import android.content.Context
import android.content.Intent
import android.os.*
import android.view.View
import com.fzm.chat.core.rtc.*
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.rtc.calling.RTCCalling
import com.fzm.chat.core.rtc.calling.RTCCalling.Companion.ENTER_ROOM_TIMEOUT
import com.fzm.chat.core.rtc.calling.RTCCalling.Companion.TYPE_BEING_CALLED
import com.fzm.chat.core.rtc.calling.RTCCallingDelegate
import com.fzm.chat.core.rtc.data.*
import com.fzm.rtc.VideoCallService
import com.fzm.rtc.msg.LocalRTCMessageManager
import com.fzm.rtc.trtc.TRTCManager
import com.tencent.rtmp.ui.TXCloudVideoView
import com.tencent.trtc.TRTCCloudDef
import com.tencent.trtc.TRTCCloudListener
import com.zjy.architecture.util.ActivityUtils
import dtalk.biz.signal.Signaling
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.koin.core.time.measureDuration
import kotlin.coroutines.CoroutineContext

/**
 * @author zhengjy
 * @since 2021/07/08
 * Description:
 */
class RTCCallingImpl(
    private val context: Context,
    private val delegate: LoginDelegate,
    private val rtcMessage: LocalRTCMessageManager
) : RTCCalling, CoroutineScope {

    companion object {
        const val WHAT_WAITING_TIMEOUT = 1
    }

    private val mUserId: String
        get() = delegate.getAddress() ?: ""

    private val taskLock = Mutex()

    private val mCurUserSet = mutableSetOf<String?>()

    private var mCurTask: RTCTask = RTCTask.EMPTY

    private val delegateManager by lazy { DelegateManager() }
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    private var timerJob: Job? = null
    private val counter = suspend {
        var delta = 0L
        while (true) {
            delay(1000 - delta)
            delta = withContext(Dispatchers.Main.immediate) {
                measureDuration {
                    mCurTask.duration++
                    delegateManager.onCallTimeTick(mCurTask.duration)
                }.toLong()
            }
        }
    }

    private val enterTimeout: Runnable = Runnable {
        launch {
            signalingListener.onCallCanceled(
                mCurTask.caller,
                mCurTask,
                Signaling.StopCallReason.Timeout_VALUE
            )
        }
    }

    override suspend fun syncTask(owner: Any?, block: suspend () -> Unit) {
        taskLock.lock(owner)
        try {
            block()
        } finally {
            if (taskLock.isLocked) taskLock.unlock(owner)
        }
    }

    override fun getCurrentTask(): RTCTask {
        return mCurTask
    }

    override fun addDelegate(delegate: RTCCallingDelegate) {
        delegateManager.addDelegate(delegate)
    }

    override fun removeDelegate(delegate: RTCCallingDelegate) {
        delegateManager.removeDelegate(delegate)
    }

    override fun call(userId: String, type: Int) {
        launch {
            syncTask("call") {
                // 通话前检查一次状态
                if (mCurTask.isBusy) return@syncTask
                val task = RTCSignalingManager.call(userId, type)
                if (task == null) {
                    delegateManager.onError(0, "房间分配失败")
                } else {
                    mCurTask = task
                    mCurTask.waiting()
                }
            }
        }
    }

    override fun groupCall(userIdList: List<String>, type: Int, groupId: String) {
        launch {
            syncTask("groupCall") {
                if (mCurTask.isWaiting || mCurTask.isOnCalling) {
                    return@syncTask
                }
            }
        }
    }

    override fun accept() {
        launch {
            syncTask("accept") {
                RTCSignalingManager.accept(mCurTask.id, onSuccess = {
                    mCurTask.sdkAppId = it.sdkAppId
                    mCurTask.roomId = it.roomId
                    mCurTask.signature = it.signature ?: ""
                    mCurTask.privateMapKey = it.privateMapKey
                    TRTCManager.createRoom(mUserId, mCurTask)
                    TRTCManager.setRtcListener(trtcListener)
                }, onFail = {
                    rtcMessage.callFailed(mCurTask)
                    delegateManager.onError(0, "接听失败")
                    release()
                    delegateManager.onCallEnd()
                })
            }
        }
    }

    override fun reject() = launch {
        syncTask("reject") {
            if (mCurTask.caller == delegate.getAddress()) {
                rtcMessage.callCanceled(mCurTask)
            } else {
                rtcMessage.callRejected(mCurTask)
            }
            RTCSignalingManager.reject(mCurTask.id)
            release()
            delegateManager.onCallEnd()
        }
    }

    override fun hangup() = launch {
        syncTask("hangup") {
            rtcMessage.callComplete(mCurTask)
            RTCSignalingManager.hangup(mCurTask.id)
            exitRoom()
        }
    }

    override fun startRemoteView(userId: String?, remoteView: View?) {
        TRTCManager.client.startRemoteView(
            userId,
            TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG,
            remoteView as TXCloudVideoView
        )
    }

    override fun stopRemoteView(userId: String?) {
        TRTCManager.client.stopRemoteView(userId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG)
    }

    override fun switchCamera(): Boolean {
        val front = TRTCManager.client.deviceManager.isFrontCamera
        TRTCManager.client.deviceManager.switchCamera(!front)
        return !front
    }

    override fun switchToAudioCall() {
        launch {
            RTCSignalingManager.switchToAudioCall()
            TRTCManager.client.stopLocalPreview()
            TRTCManager.client.stopAllRemoteView()
        }
    }

    override fun setMicMute(isMute: Boolean) {
        mCurTask.micMute = isMute
        TRTCManager.client.muteLocalAudio(isMute)
    }

    override fun setHandsFree(isHandsFree: Boolean) {
        mCurTask.handsFree = isHandsFree
        if (isHandsFree) {
            TRTCManager.client.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER)
        } else {
            TRTCManager.client.setAudioRoute(TRTCCloudDef.TRTC_AUDIO_ROUTE_EARPIECE)
        }
    }

    private val trtcListener = object : TRTCCloudListener() {
        override fun onError(code: Int, msg: String?, extraInfo: Bundle?) {
            delegateManager.onError(code, msg)
        }

        override fun onEnterRoom(result: Long) {
            if (result <= 0) {
                delegateManager.onCallEnd()
            } else {
                mCurTask.connected()
                delegateManager.onEnterRoom(result)
                if (mCurTask.isGroup) {
                    if (timerJob == null) {
                        timerJob = launch(Dispatchers.IO) { counter() }
                    }
                } else {
                    val msg = Message.obtain(handler, enterTimeout)
                    msg.what = WHAT_WAITING_TIMEOUT
                    handler.sendMessageDelayed(msg, ENTER_ROOM_TIMEOUT)
                }
            }
        }

        override fun onUserVideoAvailable(userId: String?, available: Boolean) {
            delegateManager.onUserVideoAvailable(userId, available)
        }

        override fun onRemoteUserEnterRoom(userId: String?) {
            mCurUserSet.add(userId)
            delegateManager.onUserEnter(userId)
            if (!mCurTask.isGroup) {
                if (handler.hasMessages(WHAT_WAITING_TIMEOUT)) {
                    handler.removeMessages(WHAT_WAITING_TIMEOUT)
                }
                if (timerJob == null) {
                    timerJob = launch(Dispatchers.IO) { counter() }
                }
            }
        }

        /**
         * @param userId    远端用户的用户标识
         * @param reason    离开原因，0表示用户主动退出房间，1表示用户超时退出，2表示被踢出房间。
         */
        override fun onRemoteUserLeaveRoom(userId: String?, reason: Int) {
            mCurUserSet.remove(userId)
            delegateManager.onUserLeave(userId)
            checkExitRoom(userId)
        }

        /**
         * @param reason 离开房间原因，
         * 0：主动调用 exitRoom 退出房间；
         * 1：被服务器踢出当前房间；
         * 2：当前房间整个被解散。
         */
        override fun onExitRoom(reason: Int) {
            TRTCManager.setRtcListener(null)
            delegateManager.onCallEnd()
        }
    }

    /**
     * 每当有人退出房间，检查房间中是否还有其他人，来决定是否挂断通话
     *
     */
    private fun checkExitRoom(leaveUser: String?) {
        if (mCurUserSet.isEmpty()) {
            // 当没有其他用户在房间里了，则结束通话。
            delegateManager.onHangup()
            exitRoom()
        }
    }

    /**
     * 停止音视频预览和传输，重置变量
     *
     */
    override fun release() {
        mCurTask.end()
        TRTCManager.client.stopLocalPreview()
        TRTCManager.client.stopLocalAudio()
        mCurUserSet.clear()
        timerJob?.cancel()
        timerJob = null
        mCurTask = RTCTask.EMPTY
    }

    /**
     * 退出音视频聊天房间
     *
     */
    private fun exitRoom() {
        if (mCurTask.established) {
            release()
            TRTCManager.client.exitRoom()
        } else {
            release()
            TRTCManager.setRtcListener(null)
            delegateManager.onCallEnd()
        }
    }

    private val signalingListener = object : RTCSignalingListener {
        override suspend fun onInvited(caller: String?, task: RTCTask) {
            syncTask("onInvited") {
                if (mCurTask.isBusy) {
                    // 正在通话中，无法处理新的通话
                    rtcMessage.callBusy(task)
                    RTCSignalingManager.busy(task.id)
                    return@syncTask
                } else {
                    mCurTask = task
                    mCurTask.waiting()
                    if (!mCurTask.isGroup) {
                        val intent = Intent(context, VideoCallService::class.java).apply {
                            putExtra("targetId", mCurTask.caller)
                            putExtra("taskId", mCurTask.id)
                            putExtra("roomId", mCurTask.roomId)
                            putExtra("callType", TYPE_BEING_CALLED)
                            putExtra("rtcType", mCurTask.rtcType)
                        }
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                            delegate.preference.NEW_CALL_NOTIFY &&
                            ActivityUtils.isBackground
                        ) {
                            context.startForegroundService(intent.apply {
                                putExtra("startForeground", true)
                            })
                        } else {
                            context.startService(intent)
                        }
                    } else {

                    }
                }
            }
        }

        override suspend fun onAccepted(caller: String?, task: RTCTask, targetId: String?) {
            syncTask("onAccepted") {
                // 当前正在拨打的通话被接听，则建立通话连接
                if (mCurTask.id == task.id && !mCurTask.isOnCalling) {
                    mCurTask = task
                    delegateManager.onAccepted(task.caller, task, targetId)
                    TRTCManager.createRoom(mUserId, mCurTask)
                    TRTCManager.setRtcListener(trtcListener)
                }
            }
        }

        override suspend fun onCallCanceled(caller: String?, task: RTCTask, reason: Int) {
            syncTask("onCallCanceled") {
                when (reason) {
                    // 通用回调
                    Signaling.StopCallReason.Timeout_VALUE -> {
                        rtcMessage.callTimeout(mCurTask)
                        if (delegate.getAddress() == task.caller) {
                            delegateManager.onNoResp(task.caller)
                        } else {
                            delegateManager.onCallingTimeout()
                        }
                        exitRoom()
                    }
                    Signaling.StopCallReason.Hangup_VALUE -> {
                        delegateManager.onHangup()
                        if (!mCurTask.isGroup) {
                            rtcMessage.callComplete(mCurTask)
                            exitRoom()
                        }
                    }
                    // 发起方回调
                    Signaling.StopCallReason.Reject_VALUE -> {
                        delegateManager.onReject(task.caller)
                        if (!mCurTask.isGroup) {
                            rtcMessage.callRejected(mCurTask)
                            exitRoom()
                        }
                    }
                    Signaling.StopCallReason.LineBusy_VALUE -> {
                        delegateManager.onLineBusy(task.caller)
                        rtcMessage.callBusy(mCurTask)
                        exitRoom()
                    }
                    // 接收方回调
                    Signaling.StopCallReason.Cancel_VALUE -> {
                        delegateManager.onCallingCancel()
                        rtcMessage.callCanceled(mCurTask)
                        exitRoom()
                    }
                    else -> { }
                }
            }
        }
    }

    init {
        RTCSignalingManager.addSignalingListener(signalingListener)
    }

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Main.immediate
}