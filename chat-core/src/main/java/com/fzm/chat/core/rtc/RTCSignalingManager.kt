package com.fzm.chat.core.rtc

import android.os.Handler
import android.os.Looper
import com.fzm.chat.core.data.bean.isBlock
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.repo.RTCRepository
import com.fzm.chat.core.rtc.data.*
import com.fzm.chat.core.session.LoginDelegate
import com.zjy.architecture.di.rootScope
import dtalk.biz.signal.Signaling
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2021/07/07
 * Description:实时音视频连接建立所需的信令
 */
object RTCSignalingManager {

    private val delegate by rootScope.inject<LoginDelegate>()
    private val repository by rootScope.inject<RTCRepository>()
    private val contactManager by rootScope.inject<ContactManager>()

    private val listeners = mutableListOf<RTCSignalingListener>()
    internal val signalingChannel = Channel<RTCProto>(Channel.UNLIMITED)

    private var currentTask = RTCTask.EMPTY

    private val handler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * 本地设置通话未接听超时任务
     */
    private val timeoutRunnable = Runnable {
        GlobalScope.launch(Dispatchers.Main.immediate) {
            listeners.forEach {
                it.onCallCanceled(
                    currentTask.caller,
                    currentTask,
                    Signaling.StopCallReason.Timeout_VALUE
                )
            }
        }
    }

    init {
        GlobalScope.launch(Dispatchers.Main.immediate) {
            for (signal in signalingChannel) {
                when (Signaling.ActionType.forNumber(signal.actionType)) {
                    // 收到通话请求
                    Signaling.ActionType.startCall -> {
                        repository.checkCall(signal.traceId).dataOrNull()?.apply {
                            val user = contactManager.getUserInfo(caller ?: "")
                            if (user.isBlock) {
                                // 如果是黑名单用户则忽略对方的通话请求
                                return@apply
                            }
                            val task = this.toRTCTask()
                            if (!currentTask.isBusy) {
                                // 不是忙线
                                currentTask = task
                            }
                            listeners.forEach { it.onInvited(caller, task) }
                            handler.postDelayed(timeoutRunnable, timeout)
                        }
                    }
                    // 通话请求被对面接受
                    Signaling.ActionType.acceptCall -> {
                        if (signal.traceId == currentTask.id) {
                            removeCallbacks()
                            // 被接受的通话是当前正在发起的通话
                            listeners.forEach {
                                it.onAccepted(
                                    delegate.getAddress(),
                                    currentTask.apply {
                                        sdkAppId = signal.sdkAppId
                                        roomId = signal.roomId
                                        signature = signal.signature
                                        privateMapKey = signal.privateMapKey
                                    },
                                    ""
                                )
                            }
                        }
                    }
                    Signaling.ActionType.stopCall -> {
                        if (signal.traceId == currentTask.id) {
                            removeCallbacks()
                            listeners.forEach {
                                it.onCallCanceled(currentTask.caller, currentTask, signal.reason)
                            }
                        }
                        currentTask = RTCTask.EMPTY
                    }
                    else -> {
                    }
                }
            }
        }
    }

    fun addSignalingListener(listener: RTCSignalingListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeSignalingListener(listener: RTCSignalingListener) {
        listeners.remove(listener)
    }

    suspend fun call(target: String, type: Int): RTCTask? {
        return repository.call(target, type).dataOrNull()?.let {
            currentTask = it.toRTCTask()
            handler.postDelayed(timeoutRunnable, currentTask.timeout)
            currentTask
        }
    }

    suspend fun accept(
        taskId: Long,
        onSuccess: ((RTCRoomParams) -> Unit)? = null,
        onFail: ((Throwable?) -> Unit)? = null
    ) {
        val result = repository.acceptCall(taskId)
        removeCallbacks()
        if (result.isSucceed()) {
            onSuccess?.invoke(result.data())
        } else {
            onFail?.invoke(result.error())
        }
    }

    suspend fun reject(taskId: Long) {
        removeCallbacks()
        if (taskId == 0L) return
        repository.stopCall(taskId)
    }

    suspend fun busy(taskId: Long) {
        removeCallbacks()
        if (taskId == 0L) return
        repository.lineBusy(taskId)
    }

    suspend fun hangup(taskId: Long) {
        removeCallbacks()
        if (taskId == 0L) return
        repository.stopCall(taskId)
    }

    suspend fun switchToAudioCall() {

    }

    private fun removeCallbacks() {
        handler.removeCallbacks(timeoutRunnable)
    }
}