package com.fzm.chat.core.rtc

import com.fzm.chat.core.rtc.data.RTCTask

/**
 * @author zhengjy
 * @since 2021/07/08
 * Description:
 */
interface RTCSignalingListener {

    /**
     * 当收到音视频邀请时的回调
     *
     * @param caller        主叫者id
     * @param task          通话任务
     *
     */
    suspend fun onInvited(caller: String?, task: RTCTask)

    /**
     * 对方同意之后，主叫人收到的回调
     *
     * @param caller        主叫者id
     * @param task          通话任务
     * @param targetId      被邀请人id
     *
     */
    suspend fun onAccepted(caller: String?, task: RTCTask, targetId: String?)

    /**
     * 等待音视频连接期间，收到音视频取消时的回调
     *
     * @param caller    主叫者id
     * @param task      通话任务
     * @param reason    取消原因
     *
     */
    suspend fun onCallCanceled(caller: String?, task: RTCTask, reason: Int)
}