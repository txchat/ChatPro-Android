package com.fzm.rtc.trtc

import android.content.Context
import android.os.Bundle
import com.fzm.chat.core.rtc.data.RTCTask
import com.fzm.chat.core.rtc.data.isVideo
import com.tencent.trtc.TRTCCloud
import com.tencent.trtc.TRTCCloudDef.*
import com.tencent.trtc.TRTCCloudListener
import com.tencent.trtc.TRTCStatistics
import com.zjy.architecture.util.logD
import com.zjy.architecture.util.logE

/**
 * @author zhengjy
 * @since 2021/07/06
 * Description:
 */
object TRTCManager : TRTCCloudListener()  {

    lateinit var client: TRTCCloud

    private var listener: TRTCCloudListener? = null

    fun init(context: Context) {
        client = TRTCCloud.sharedInstance(context)
        client.setListener(this)
    }

    fun createRoom(userId: String, task: RTCTask) {
        val trtcParams = TRTCParams()
        trtcParams.userId = userId
        trtcParams.sdkAppId = task.sdkAppId
        trtcParams.roomId = task.roomId
        trtcParams.userSig = task.signature
        trtcParams.privateMapKey = task.privateMapKey
        client.enterRoom(trtcParams, if (task.isVideo) TRTC_APP_SCENE_VIDEOCALL else TRTC_APP_SCENE_AUDIOCALL)
    }

    fun setRtcListener(listener: TRTCCloudListener?) {
        TRTCManager.listener = listener
    }

    override fun onError(errCode: Int, errMsg: String?, extraInfo: Bundle?) {
        logE("TRTC Error：code $errCode, msg $errMsg, extra $extraInfo")
        listener?.onError(errCode, errMsg, extraInfo)
    }

    override fun onWarning(var1: Int, var2: String?, var3: Bundle?) {}

    override fun onEnterRoom(result: Long) {
        if (result > 0) {
            logD("进房成功，总计耗时${result}ms")
        } else {
            logE("进房失败，错误码${result}")
        }
        listener?.onEnterRoom(result)
    }

    override fun onExitRoom(var1: Int) {
        listener?.onExitRoom(var1)
    }

    override fun onSwitchRole(var1: Int, var2: String?) {}

    override fun onSwitchRoom(var1: Int, var2: String?) {}

    override fun onConnectOtherRoom(var1: String?, var2: Int, var3: String?) {}

    override fun onDisConnectOtherRoom(var1: Int, var2: String?) {}

    override fun onRemoteUserEnterRoom(var1: String?) {
        listener?.onRemoteUserEnterRoom(var1)
    }

    override fun onRemoteUserLeaveRoom(var1: String?, var2: Int) {
        listener?.onRemoteUserLeaveRoom(var1, var2)
    }

    override fun onUserVideoAvailable(userId: String?, available: Boolean) {
        listener?.onUserVideoAvailable(userId, available)
    }

    override fun onUserSubStreamAvailable(userId: String?, available: Boolean) {}

    override fun onUserAudioAvailable(userId: String?, available: Boolean) {}

    override fun onFirstVideoFrame(var1: String?, var2: Int, var3: Int, var4: Int) {
        listener?.onFirstVideoFrame(var1, var2, var3, var4)
    }

    override fun onFirstAudioFrame(var1: String?) {}

    override fun onSendFirstLocalVideoFrame(var1: Int) {}

    override fun onSendFirstLocalAudioFrame() {}

    override fun onNetworkQuality(var1: TRTCQuality?, var2: ArrayList<TRTCQuality?>?) {}

    override fun onStatistics(var1: TRTCStatistics?) {}

    override fun onConnectionLost() {
        listener?.onConnectionLost()
    }

    override fun onTryToReconnect() {
        listener?.onTryToReconnect()
    }

    override fun onConnectionRecovery() {
        listener?.onConnectionRecovery()
    }

    override fun onSpeedTest(var1: TRTCSpeedTestResult?, var2: Int, var3: Int) {}

    override fun onCameraDidReady() {}

    override fun onMicDidReady() {}

    override fun onAudioRouteChanged(var1: Int, var2: Int) {}

    override fun onUserVoiceVolume(var1: ArrayList<TRTCVolumeInfo?>?, var2: Int) {}

    override fun onRecvCustomCmdMsg(var1: String?, var2: Int, var3: Int, var4: ByteArray?) {}

    override fun onMissCustomCmdMsg(var1: String?, var2: Int, var3: Int, var4: Int) {}

    override fun onRecvSEIMsg(var1: String?, var2: ByteArray?) {}

    override fun onStartPublishing(var1: Int, var2: String?) {}

    override fun onStopPublishing(var1: Int, var2: String?) {}

    override fun onStartPublishCDNStream(var1: Int, var2: String?) {}

    override fun onStopPublishCDNStream(var1: Int, var2: String?) {}

    override fun onSetMixTranscodingConfig(var1: Int, var2: String?) {}

    override fun onScreenCaptureStarted() {}

    override fun onScreenCapturePaused() {}

    override fun onScreenCaptureResumed() {}

    override fun onScreenCaptureStopped(var1: Int) {}

    override fun onLocalRecordBegin(var1: Int, var2: String?) {}

    override fun onLocalRecording(var1: Long, var3: String?) {}

    override fun onLocalRecordComplete(var1: Int, var2: String?) {}
}