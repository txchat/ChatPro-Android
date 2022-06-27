package com.fzm.rtc.ui

import android.Manifest
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.utils.onExplain
import com.fzm.chat.core.rtc.calling.RTCCalling
import com.fzm.chat.core.rtc.data.isBusy
import com.fzm.chat.core.rtc.data.isOnCalling
import com.fzm.chat.core.rtc.data.isWaiting
import com.fzm.chat.router.rtc.RtcModule
import com.fzm.rtc.*
import com.fzm.rtc.databinding.ActivityRtcCallBinding
import com.permissionx.guolindev.PermissionX
import com.zjy.architecture.ext.notchSupport
import com.zjy.architecture.ext.powerManager
import com.zjy.architecture.ext.sensorManager
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.ActivityUtils
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/07/06
 * Description:
 */
@Route(path = RtcModule.VIDEO_CALL)
class RTCCallActivity : BizActivity(), SensorEventListener {

    @JvmField
    @Autowired
    var targetId: String? = null

    @JvmField
    @Autowired
    var callType: Int = 0

    @JvmField
    @Autowired
    var taskId: Long = 0L

    @JvmField
    @Autowired
    var roomId: Int = 0

    @JvmField
    @Autowired
    var rtcType: Int = 0

    @JvmField
    @Autowired
    var action: Int = 0

    private var currentFragment: Fragment? = null

    private val serviceIntent by lazy {
        Intent(this, VideoCallService::class.java).apply {
            putExtra("targetId", targetId)
            putExtra("taskId", taskId)
            putExtra("roomId", roomId)
            putExtra("callType", RTCCalling.TYPE_CALL)
            putExtra("rtcType", rtcType)
        }
    }

    private val viewModel by viewModel<RTCCallViewModel>()
    private val rtcCall by inject<RTCCalling>()

    private val proximitySensor by lazy(LazyThreadSafetyMode.NONE) {
        sensorManager?.getDefaultSensor(
            Sensor.TYPE_PROXIMITY
        )
    }
    private val wakeLock by lazy(LazyThreadSafetyMode.NONE) {
        powerManager?.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
            "RTCCallActivity:WakeLock"
        )
    }
    private val enableSensor: Boolean
        get() = powerManager?.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)
            ?: false

    private val binding by init { ActivityRtcCallBinding.inflate(layoutInflater) }
    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        notchSupport(window)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        ensureNotBusy(intent) {
            setIntent(intent)
            ARouter.getInstance().inject(this)
            if (!checkArguments()) return@ensureNotBusy
            viewModel.cancelClose()
            showCallFragment()
        }
    }

    /**
     * 防止接听和拨打同时进行出现错乱
     */
    private fun ensureNotBusy(intent: Intent? = null, block: () -> Unit) {
        lifecycleScope.launch {
            rtcCall.syncTask {
                val task = rtcCall.getCurrentTask()
                if (task.isOnCalling) return@syncTask
                intent?.also { taskId = it.getLongExtra("taskId", 0L) }
                if (!task.isWaiting || task.id == taskId) {
                    block()
                }
            }
        }
    }

    private fun checkArguments(): Boolean {
        if (callType == 0 || rtcType == 0) {
            toast("通话参数错误")
            finish()
            return false
        }
        if (callType == RTCCalling.TYPE_BEING_CALLED) {

        } else {
            if (targetId.isNullOrEmpty()) {
                toast("被叫人为空")
                finish()
                return false
            }
        }
        return true
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        if (!checkArguments()) return
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window?.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        lifecycleScope.launch {
            rtcCall.syncTask {
                val task = rtcCall.getCurrentTask()
                if (task.isBusy) {
                    showCallFragment()
                } else {
                    if (callType == RTCCalling.TYPE_CALL) {
                        // 作为主叫方时，主动启动service
                        startService(serviceIntent)
                    }
                    showCallFragment()
                }
            }
        }
    }

    private fun showCallFragment() {
        PermissionX.init(this)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .onExplain(this)
            .request { allGranted, _, _ ->
                if (allGranted) {
                    if (rtcType == RTCCalling.TYPE_VIDEO_CALL) {
                        supportFragmentManager.commit {
                            currentFragment?.also { remove(it) }
                            add(R.id.fcv_container, VideoCallFragment.create(targetId, callType, action).also { currentFragment = it })
                        }
                    } else {
                        supportFragmentManager.commit {
                            currentFragment?.also { remove(it) }
                            add(R.id.fcv_container, AudioCallFragment.create(targetId, callType, action).also { currentFragment = it })
                        }
                    }
                } else {
                    toast(R.string.permission_not_granted)
                    finish()
                }
            }
    }

    override fun initData() {
        viewModel.closePage.observe(this) {
            finish()
        }
    }

    override fun setEvent() {

    }

    override fun onResume() {
        super.onResume()
        if (rtcType == RTCCalling.TYPE_AUDIO_CALL && enableSensor) {
            sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (rtcType == RTCCalling.TYPE_AUDIO_CALL && enableSensor) {
            sensorManager?.unregisterListener(this)
            if (wakeLock?.isHeld == true) {
                //释放电源锁
                wakeLock?.release()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (rtcCall.getCurrentTask().handsFree) {
            // 免提的时候不响应传感器的变化
            return
        }
        if (callType == RTCCalling.TYPE_BEING_CALLED && !rtcCall.getCurrentTask().isOnCalling) {
            // 被叫方还没接听电话的时候不响应传感器变化
            return
        }
        val its = event.values
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            if (its[0] == 0.0f) {
                // 贴近手机
                if (wakeLock?.isHeld == true) {
                    return
                } else {
                    wakeLock?.acquire(5000L)
                }
            } else {
                // 远离手机
                if (wakeLock?.isHeld == true) {
                    return
                } else {
                    wakeLock?.setReferenceCounted(false)
                    wakeLock?.release()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}