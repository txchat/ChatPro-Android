package com.fzm.rtc.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.rtc.calling.RTCCalling
import com.fzm.chat.core.rtc.calling.RTCCallingDelegate
import com.fzm.chat.core.rtc.data.*
import com.fzm.rtc.R
import com.fzm.rtc.databinding.FragmentVideoCallBinding
import com.fzm.rtc.trtc.TRTCManager
import com.fzm.widget.dialog.EasyDialog
import com.tencent.trtc.TRTCCloudDef
import com.zjy.architecture.ext.*

/**
 * @author zhengjy
 * @since 2021/07/09
 * Description:
 */
class VideoCallFragment : BaseCallFragment(), View.OnClickListener {

    companion object {
        fun create(targetId: String?, callType: Int, action: Int): VideoCallFragment {
            return VideoCallFragment().apply {
                arguments = bundleOf(
                    "targetId" to targetId,
                    "callType" to callType,
                    "action" to action
                )
            }
        }
    }

    private val binding by init<FragmentVideoCallBinding>()
    private var mContact: Contact? = null

    /**
     * 视频通话转接为语音通话
     */
    private var switchToAudio = false

    override val root: View
        get() = binding.root

    override val rtcType: Int = RTCCalling.TYPE_VIDEO_CALL

    override fun initView(view: View, savedInstanceState: Bundle?) {
        targetId = arguments?.getString("targetId") ?: ""
        callType = arguments?.getInt("callType", 0) ?: 0
        if (callType == 0) {
            viewModel.closeActivity()
            return
        }
        super.initView(view, savedInstanceState)
        if (rtcCall.getCurrentTask().isOnCalling) {
            if (rtcCall.getCurrentTask().isVideo) {
                showCallingView()
            } else {
                showAudioView()
            }
            TRTCManager.client.startLocalPreview(true, binding.videoLocal)
            rtcCall.startRemoteView(targetId, binding.videoRemote)
        } else {
            if (callType == RTCCalling.TYPE_CALL) {
                TRTCManager.client.startLocalPreview(true, binding.videoLocal)
                TRTCManager.client.muteLocalVideo(true)
                binding.tvContactTips.text = getString(R.string.rtc_tips_waiting_for_answer)
                rtcCall.call(targetId, rtcType)
            } else {
                binding.tvContactTips.text = getString(R.string.rtc_tips_video_call_incoming)
                when (action) {
                    ACTION_NOTHING -> showIncomingView()
                    ACTION_ACCEPT -> rtcCall.accept()
                    ACTION_REJECT -> {
                        // 拒绝接听通常不会显示这个页面
                    }
                }
            }
        }
    }

    override fun initData() {
        viewModel.getContactInfo(targetId).observe(viewLifecycleOwner) {
            mContact = it
            binding.tvName.text = it.getDisplayName()
            binding.ivAvatar.load(it.getDisplayImage(), R.mipmap.default_avatar_round)
        }
    }

    override fun setEvent() {
        binding.ivHangup.singleClick(500, this)
        binding.ivAccept.singleClick(500, this)
        binding.ivSwitchAudio.singleClick(500, this)
        binding.ivSwitchCamera.singleClick(500, this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_hangup -> {
                if (rtcCall.getCurrentTask().isOnCalling) {
                    rtcCall.hangup()
                    enableAllViews(false)
                    showStatus("通话结束")
                } else {
                    rtcCall.reject()
                    viewModel.closeActivity()
                }
            }
            R.id.iv_accept -> {
                rtcCall.accept()
            }
            R.id.iv_switch_audio -> {
                if (switchToAudio) {
                    val micMute = !curTask.micMute
                    rtcCall.setMicMute(micMute)
                    binding.ivSwitchAudio.setImageResource(if (micMute) R.drawable.icon_rtc_mute_on else R.drawable.icon_rtc_mute)
                } else {
                    switchToAudio = true
                    showAudioView()
                    rtcCall.switchToAudioCall()
                }
            }
            R.id.iv_switch_camera -> {
                if (switchToAudio) {
                    val handsFree = !curTask.handsFree
                    rtcCall.setHandsFree(handsFree)
                    binding.ivSwitchCamera.setImageResource(if (handsFree) R.drawable.icon_rtc_hands_free_on else R.drawable.icon_rtc_hands_free)
                } else {
                    rtcCall.switchCamera()
                }
            }
        }
    }

    override fun onBackPressedSupport(): Boolean {
        EasyDialog.Builder()
            .setHeaderTitle(getString(R.string.biz_tips))
            .setContent(getString(R.string.rtc_dialog_confirm_hangup))
            .setBottomLeftText(getString(R.string.biz_cancel))
            .setBottomRightText(getString(R.string.biz_confirm))
            .setBottomRightClickListener {
                endCall()
            }
            .create(requireContext())
            .show()
        return true
    }

    /**
     * 显示正在通话中的布局，默认是拨打布局
     */
    private fun showCallingView() {
        if (callType == RTCCalling.TYPE_BEING_CALLED) {
            // 被叫方显示通话布局，需要移动挂断按钮到中间
            binding.llHangup.layoutParams = (binding.llHangup.layoutParams as ConstraintLayout.LayoutParams).apply {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }
        binding.llContact.gone()
        binding.llAccept.gone()
        binding.tvHangup.setText(R.string.rtc_action_hangup)
        // TODO: 暂不支持视频转接语音功能
        binding.llSwitchAudio.gone()
        binding.llSwitchCamera.visible()
        binding.tvTime.visible()
        binding.videoLocal.layoutParams = (binding.videoLocal.layoutParams as ViewGroup.MarginLayoutParams).apply {
            height = 160.dp
            width = 90.dp
            setMargins(0, 40.dp, 15.dp, 0)
        }
        binding.ivSwitchAudio.setImageResource(R.drawable.icon_rtc_switch_audio)
        binding.ivSwitchCamera.setImageResource(R.drawable.icon_rtc_switch_camera)
    }

    /**
     * 显示收到通话请求的布局，默认是拨打布局
     */
    private fun showIncomingView() {
        binding.llAccept.visible()
        binding.llHangup.layoutParams = (binding.llHangup.layoutParams as ConstraintLayout.LayoutParams).apply {
            endToEnd = binding.llAccept.id
        }
        binding.tvHangup.setText(R.string.rtc_action_reject)
    }

    /**
     * 视频通话转为语音电话之后的布局
     */
    private fun showAudioView() {
        if (callType == RTCCalling.TYPE_BEING_CALLED) {
            // 被叫方显示通话布局，需要移动挂断按钮到中间
            binding.llHangup.layoutParams = (binding.llHangup.layoutParams as ConstraintLayout.LayoutParams).apply {
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
        }
        binding.llContact.gone()
        binding.llAccept.gone()
        binding.tvHangup.setText(R.string.rtc_action_hangup)
        binding.llAudioContact.visible()
        if (mContact != null) {
            binding.audioName.text = mContact!!.getDisplayName()
            binding.audioAvatar.load(mContact!!.getDisplayImage(), R.mipmap.default_avatar_round)
        } else {
            viewModel.getContactInfo(targetId).observe(viewLifecycleOwner) {
                binding.audioName.text = it.getDisplayName()
                binding.audioAvatar.load(it.getDisplayImage(), R.mipmap.default_avatar_round)
            }
        }
        binding.llSwitchAudio.visible()
        binding.llSwitchCamera.visible()
        binding.ivSwitchAudio.setImageResource(if (curTask.micMute) R.drawable.icon_rtc_mute_on else R.drawable.icon_rtc_mute)
        binding.ivSwitchCamera.setImageResource(if (curTask.handsFree) R.drawable.icon_rtc_hands_free_on else R.drawable.icon_rtc_hands_free)
    }

    override val delegate = object : RTCCallingDelegate {
        override fun onError(code: Int, msg: String?) {
            msg?.also { toast(it) }
            endCall()
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
            enableAllViews(false)
            showStatus("连接中…", 5000L)
        }

        override fun onUserEnter(userId: String?) {
            showStatus("已接通")
            if (curTask.isVideo) {
                showCallingView()
                if (callType == RTCCalling.TYPE_BEING_CALLED) {
                    // 被叫方打开摄像头
                    TRTCManager.client.startLocalPreview(true, binding.videoLocal)
                }
                TRTCManager.client.muteLocalVideo(false)
            } else {
                showAudioView()
                TRTCManager.client.stopLocalPreview()

            }
            TRTCManager.client.startLocalAudio(TRTCCloudDef.TRTC_AUDIO_QUALITY_DEFAULT)
            enableAllViews(true)
        }

        override fun onUserLeave(userId: String?) {

        }

        override fun onUserVideoAvailable(userId: String?, isVideoAvailable: Boolean) {
            if (isVideoAvailable && curTask.isVideo) {
                rtcCall.startRemoteView(userId, binding.videoRemote)
            } else {
                rtcCall.stopRemoteView(userId)
            }
        }

        override fun onUserAudioAvailable(userId: String?, isVideoAvailable: Boolean) {

        }

        override fun onReject(userId: String?) {
            enableAllViews(false)
            showStatus("对方已拒绝")
        }

        override fun onHangup() {
            enableAllViews(false)
            showStatus("对方已挂断，通话结束")
        }

        override fun onNoResp(userId: String?) {
            enableAllViews(false)
            showStatus("对方未响应")
            viewModel.closeActivity(END_CALL_DELAY)
        }

        override fun onLineBusy(userId: String?) {
            enableAllViews(false)
            showStatus("对方正忙")
        }

        override fun onCallingCancel() {
            enableAllViews(false)
            showStatus("通话取消")
        }

        override fun onCallingTimeout() {
            enableAllViews(false)
            showStatus("通话超时")
            viewModel.closeActivity(END_CALL_DELAY)
        }

        override fun onCallEnd() {
            enableAllViews(false)
            viewModel.closeActivity(END_CALL_DELAY)
        }

        override fun onCallTimeTick(duration: Long) {
            if (!binding.tvTime.isVisible) {
                binding.tvTime.visible()
            }
            binding.tvTime.text = formatTime(duration)
        }

        override fun onUserVoiceVolume(volumeMap: Map<String, Int>) {

        }

        override fun onSwitchToAudio(success: Boolean, message: String?) {
            if (success) {
                switchToAudio = true
                showAudioView()
                TRTCManager.client.stopLocalPreview()
                TRTCManager.client.stopAllRemoteView()
            }
        }
    }

    private fun showStatus(tips: String, delay: Long = 2000) {
        viewModel.startHideStatus(delay).observe(viewLifecycleOwner) {
            binding.tvStatus.invisible()
        }
        binding.tvStatus.text = tips
        binding.tvStatus.visible()
    }

    private fun enableAllViews(clickable: Boolean) {
        binding.ivAccept.isClickable = clickable
        binding.ivHangup.isClickable = clickable
        binding.ivSwitchAudio.isClickable = clickable
        binding.ivSwitchCamera.isClickable = clickable
    }
}