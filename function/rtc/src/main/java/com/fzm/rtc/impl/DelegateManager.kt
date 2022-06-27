package com.fzm.rtc.impl

import com.fzm.chat.core.rtc.calling.RTCCallingDelegate
import com.fzm.chat.core.rtc.data.RTCTask
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @author zhengjy
 * @since 2021/07/07
 * Description:
 */
internal class DelegateManager : RTCCallingDelegate {

    private val delegates = ConcurrentLinkedQueue<RTCCallingDelegate>()

    fun addDelegate(delegate: RTCCallingDelegate) {
        if (!delegates.contains(delegate)) {
            delegates.add(delegate)
        }
    }

    fun removeDelegate(delegate: RTCCallingDelegate) {
        delegates.remove(delegate)
    }

    fun removeAllDelegate() {
        delegates.clear()
    }

    override fun onError(code: Int, msg: String?) {
        delegates.forEach { it.onError(code, msg) }
    }

    override fun onInvited(
        sponsor: String,
        userIdList: List<String>,
        isFromGroup: Boolean,
        callType: Int
    ) {
        delegates.forEach { it.onInvited(sponsor, userIdList, isFromGroup, callType) }
    }

    override fun onAccepted(calledId: String?, task: RTCTask, targetId: String?) {
        delegates.forEach { it.onAccepted(calledId, task, targetId) }
    }

    override fun onGroupCallInviteeListUpdate(userIdList: List<String>) {
        delegates.forEach { it.onGroupCallInviteeListUpdate(userIdList) }
    }

    override fun onEnterRoom(cost: Long) {
        delegates.forEach { it.onEnterRoom(cost) }
    }

    override fun onUserEnter(userId: String?) {
        delegates.forEach { it.onUserEnter(userId) }
    }

    override fun onUserLeave(userId: String?) {
        delegates.forEach { it.onUserLeave(userId) }
    }

    override fun onUserVideoAvailable(userId: String?, isVideoAvailable: Boolean) {
        delegates.forEach { it.onUserVideoAvailable(userId, isVideoAvailable) }
    }

    override fun onUserAudioAvailable(userId: String?, isVideoAvailable: Boolean) {
        delegates.forEach { it.onUserAudioAvailable(userId, isVideoAvailable) }
    }

    override fun onReject(userId: String?) {
        delegates.forEach { it.onReject(userId) }
    }

    override fun onHangup() {
        delegates.forEach { it.onHangup() }
    }

    override fun onNoResp(userId: String?) {
        delegates.forEach { it.onNoResp(userId) }
    }

    override fun onLineBusy(userId: String?) {
        delegates.forEach { it.onLineBusy(userId) }
    }

    override fun onCallingCancel() {
        delegates.forEach { it.onCallingCancel() }
    }

    override fun onCallingTimeout() {
        delegates.forEach { it.onCallingTimeout() }
    }

    override fun onCallEnd() {
        delegates.forEach { it.onCallEnd() }
        removeAllDelegate()
    }

    override fun onCallTimeTick(duration: Long) {
        delegates.forEach { it.onCallTimeTick(duration) }
    }

    override fun onUserVoiceVolume(volumeMap: Map<String, Int>) {
        delegates.forEach { it.onUserVoiceVolume(volumeMap) }
    }

    override fun onSwitchToAudio(success: Boolean, message: String?) {
        delegates.forEach { it.onSwitchToAudio(success, message) }
    }
}