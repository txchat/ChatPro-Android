package com.fzm.rtc

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fzm.chat.biz.ui.DeepLinkHelper
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.rtc.calling.RTCCalling
import com.fzm.chat.core.rtc.calling.RTCCallingDelegate
import com.fzm.chat.core.rtc.data.RTCTask
import com.fzm.chat.core.rtc.data.isBusy
import com.fzm.chat.core.rtc.data.isWaiting
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.app.AppModule
import com.fzm.rtc.ui.RTCCallActivity
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.powerManager
import com.zjy.architecture.ext.vibrator
import com.zjy.architecture.util.ActivityUtils
import com.zjy.architecture.util.immutableFlag
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

/**
 * @author zhengjy
 * @since 2021/07/07
 * Description:
 */
class VideoCallService : Service(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Main.immediate

    companion object {
        const val ID_INCOMING_CALL = 99
        const val ID_CALLING = 100
    }

    /**
     * 来电通知震动模式
     */
    private val timings = longArrayOf(0, 120, 80, 120, 80, 120, 80, 120, 1000)

    private var targetId = ""
    private var taskId: Long = 0L
    private var roomId: Int = 0
    private var callType: Int = 0
    private var rtcType: Int = 0

    private var startForeground = false

    private val rtcCall by inject<RTCCalling>()
    private val contactManager by inject<ContactManager>()
    private val loginDelegate by inject<LoginDelegate>()

    private var targetName: String = ""
    private var avatar: Bitmap? = null

    private val receiver = CallServiceReceiver()
    private val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    private val wakeLock by lazy(LazyThreadSafetyMode.NONE) {
        powerManager?.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "VideoCallService:IncomingCall"
        )
    }

    /**
     * 来电铃声
     */
    private val incomingRingtone by lazy {
        RingtoneManager.getRingtone(this, soundUri).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//                isLooping = true
            }
        }
    }

    private val appStatusChangeListener = object : ActivityUtils.OnAppStateChangedListener {
        override fun onLeaveApp() {
            if (callType == RTCCalling.TYPE_BEING_CALLED && rtcCall.getCurrentTask().isWaiting) {
                sendBroadcast(Intent(RTCConst.ACTION_INCOMING_BACKGROUND))
            }
        }

        override fun onResumeApp() {
            // App回到前台时，如果有电话正等待接听，则自动弹出界面
            if (rtcCall.getCurrentTask().isWaiting) {
                if (!ActivityUtils.isActivityTop(this@VideoCallService, RTCCallActivity::class.java)) {
                    showIncomingCallInner()
                }
            }
        }
    }

    private fun getCallIntent(action: String? = null) = Intent(this@VideoCallService, RTCCallActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra("targetId", targetId)
        putExtra("taskId", taskId)
        putExtra("roomId", roomId)
        putExtra("callType", callType)
        putExtra("rtcType", rtcType)
        setAction(action)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetId = intent?.getStringExtra("targetId") ?: ""
        taskId = intent?.getLongExtra("taskId", 0) ?: 0
        roomId = intent?.getIntExtra("roomId", 0) ?: 0
        callType = intent?.getIntExtra("callType", 0) ?: 0
        rtcType = intent?.getIntExtra("rtcType", 0) ?: 0
        startForeground = intent?.getBooleanExtra("startForeground", false) ?: false
        rtcCall.addDelegate(delegate)
        registerReceiver(receiver, IntentFilter().apply {
            addAction(RTCConst.ACTION_INCOMING_BACKGROUND)
            addAction(RTCConst.ACTION_ENTER_CALL)
            addAction(RTCConst.ACTION_ACCEPT)
            addAction(RTCConst.ACTION_REJECT)
        })
        ActivityUtils.addOnAppStateChangedListener(appStatusChangeListener)
        launch {
            val user = contactManager.getUserInfo(targetId)
            targetName = user.getDisplayName()
            Glide.with(this@VideoCallService).asBitmap().load(user.getDisplayImage())
                .apply(RequestOptions().transform(RoundedCorners(5.dp)))
                .into(object : CustomTarget<Bitmap>(60.dp, 60.dp) {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        avatar = resource
                        if (callType == RTCCalling.TYPE_BEING_CALLED) {
                            showIncomingCall()
                        } else {
                            showCallingNotification()
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        if (callType == RTCCalling.TYPE_BEING_CALLED) {
                            showIncomingCall()
                        } else {
                            showCallingNotification()
                        }
                    }
                })
        }
        return START_STICKY
    }

    /**
     * 显示来电通知
     */
    private fun showIncomingNotification(fullScreen: Boolean = true) {
        // 通话处于等待接听状态时才会显示incoming通知
        if (!rtcCall.getCurrentTask().isWaiting) return
        NotificationCompat.Builder(this, "rtcCall").apply {
            val type = if (rtcType == RTCCalling.TYPE_VIDEO_CALL) "视频" else "语音"
            val remoteView = RemoteViews(packageName, R.layout.notification_incoming_call)
            remoteView.setOnClickPendingIntent(
                R.id.iv_accept,
                PendingIntent.getBroadcast(
                    this@VideoCallService,
                    0,
                    Intent(RTCConst.ACTION_ACCEPT),
                    PendingIntent.FLAG_UPDATE_CURRENT.immutableFlag
                )
            )
            remoteView.setOnClickPendingIntent(
                R.id.iv_reject,
                PendingIntent.getBroadcast(
                    this@VideoCallService,
                    0,
                    Intent(RTCConst.ACTION_REJECT),
                    PendingIntent.FLAG_UPDATE_CURRENT.immutableFlag
                )
            )
            remoteView.setTextViewText(R.id.tv_title, targetName)
            remoteView.setTextViewText(R.id.tv_sub_title, "邀请你${type}通话")
            remoteView.setImageViewBitmap(R.id.iv_avatar, avatar)
            setCustomContentView(remoteView)
            setAutoCancel(true)

            setSmallIcon(R.drawable.ic_notification)
            val contentIntent = PendingIntent.getBroadcast(
                this@VideoCallService,
                0,
                Intent(RTCConst.ACTION_ENTER_CALL),
                PendingIntent.FLAG_UPDATE_CURRENT.immutableFlag
            )
            // 指定点击跳转页面
            setContentIntent(contentIntent)
            setCategory(NotificationCompat.CATEGORY_CALL)
            setPriority(NotificationCompat.PRIORITY_HIGH)
            if (fullScreen) {
                setFullScreenIntent(contentIntent, true)
            }
            startForeground(ID_INCOMING_CALL, this.build())
        }
    }

    /**
     * 显示来电界面或来电通知
     */
    private fun showIncomingCall() {
        if (!loginDelegate.preference.NEW_CALL_NOTIFY && ActivityUtils.isBackground)
            // 设置关闭了新通话提醒，并且app处于后台
            return
        if (wakeLock?.isHeld != true) {
            wakeLock?.acquire(5000L)
        }
        showIncomingCallInner()
    }

    private fun showIncomingCallInner() = launch {
        rtcCall.syncTask("showIncomingCallInner") {
            if (rtcCall.getCurrentTask().isBusy && rtcCall.getCurrentTask().id != taskId) {
                stopService()
                return@syncTask
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (startForeground) {
                    showIncomingNotification()
                } else {
                    openRTCCallPage()
                }
            } else {
                openRTCCallPage()
            }
            playIncomingSound()
        }
    }

    /**
     * 通话过程中常驻通知
     */
    private fun showCallingNotification() {
        stopForeground(true)
        NotificationCompat.Builder(this, "notification").apply {
            val type = if (rtcType == RTCCalling.TYPE_VIDEO_CALL) "视频" else "语音"
            setContentTitle(targetName)
            setContentText("${type}通话中，点击返回")
            setOngoing(true)
            setSmallIcon(R.drawable.ic_notification)
            setLargeIcon(avatar)
            val contentIntent = PendingIntent.getBroadcast(
                this@VideoCallService,
                0,
                Intent(RTCConst.ACTION_ENTER_CALL),
                PendingIntent.FLAG_UPDATE_CURRENT.immutableFlag
            )
            // 指定点击跳转页面
            setContentIntent(contentIntent)
            startForeground(ID_CALLING, this.build())
        }
    }

    private fun playIncomingSound() {
        incomingRingtone.play()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitudes = IntArray(timings.size)
            for (i in 0 until timings.size / 2) {
                amplitudes[i * 2 + 1] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    VibrationEffect.EFFECT_DOUBLE_CLICK
                } else {
                    128
                }
            }
            vibrator?.vibrate(
                VibrationEffect.createWaveform(timings, amplitudes, 0),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            setHapticChannelsMuted(true)
                            setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL)
                        }
                    }
                    .setLegacyStreamType(AudioManager.STREAM_RING)
                    .build()
            )
        } else {
            vibrator?.vibrate(timings, 0)
        }
    }

    private fun stopIncomingSound() {
        if (incomingRingtone.isPlaying) {
            incomingRingtone.stop()
        }
        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopIncomingSound()
        super.onDestroy()
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        coroutineContext.cancel()
        rtcCall.removeDelegate(delegate)
        ActivityUtils.removeOnAppStateChangedListener(appStatusChangeListener)
        if (wakeLock?.isHeld == true) {
            //释放电源锁
            wakeLock?.release()
        }
        avatar = null
    }

    private val delegate: RTCCallingDelegate = object : RTCCallingDelegate {
        override fun onError(code: Int, msg: String?) {
            stopService()
        }

        override fun onInvited(
            sponsor: String,
            userIdList: List<String>,
            isFromGroup: Boolean,
            callType: Int
        ) {

        }

        override fun onAccepted(calledId: String?, task: RTCTask, targetId: String?) {

        }

        override fun onGroupCallInviteeListUpdate(userIdList: List<String>) {

        }

        override fun onEnterRoom(cost: Long) {
            stopIncomingSound()
            // 进入房间后显示[正在通话中]的通知
            showCallingNotification()
        }

        override fun onUserEnter(userId: String?) {

        }

        override fun onUserLeave(userId: String?) {

        }

        override fun onUserVideoAvailable(userId: String?, isVideoAvailable: Boolean) {

        }

        override fun onUserAudioAvailable(userId: String?, isVideoAvailable: Boolean) {

        }

        override fun onReject(userId: String?) {

        }

        override fun onHangup() {

        }

        override fun onNoResp(userId: String?) {
            stopService()
        }

        override fun onLineBusy(userId: String?) {

        }

        override fun onCallingCancel() {

        }

        override fun onCallingTimeout() {
            stopService()
        }

        override fun onCallEnd() {
            stopService()
        }

        override fun onCallTimeTick(duration: Long) {

        }

        override fun onUserVoiceVolume(volumeMap: Map<String, Int>) {

        }

        override fun onSwitchToAudio(success: Boolean, message: String?) {

        }
    }

    private fun stopService() {
        stopIncomingSound()
        stopForeground(true)
        stopSelf()
    }

    private fun openRTCCallPage() {
        if (!ActivityUtils.isActivityExist("com.fzm.chat.app.MainActivity")) {
            val host = Uri.parse(DeepLinkHelper.APP_LINK)
            val route = host.buildUpon().apply {
                appendQueryParameter("type", "rtcCall")
                appendQueryParameter("targetId", targetId)
                appendQueryParameter("taskId", taskId.toString())
                appendQueryParameter("roomId", roomId.toString())
                appendQueryParameter("callType", callType.toString())
                appendQueryParameter("rtcType", rtcType.toString())
            }.build()
            ARouter.getInstance().build(AppModule.MAIN)
                .withParcelable("route", route)
                .withFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .navigation()
        } else {
            startActivity(getCallIntent())
        }
    }

    inner class CallServiceReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                RTCConst.ACTION_ENTER_CALL -> {
                    openRTCCallPage()
                }
                RTCConst.ACTION_ACCEPT -> {
                    stopForeground(true)
                    val acceptIntent = getCallIntent()
                    acceptIntent.putExtra("action", 1)
                    context?.startActivity(acceptIntent)
                }
                RTCConst.ACTION_REJECT -> rtcCall.reject()
                RTCConst.ACTION_INCOMING_BACKGROUND -> showIncomingNotification(false)
            }
        }
    }
}