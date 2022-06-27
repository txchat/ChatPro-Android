package com.fzm.rtc.ui

import android.os.Bundle
import android.view.View
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.core.rtc.data.RTCTask
import com.fzm.rtc.R
import com.fzm.chat.core.rtc.calling.RTCCalling
import com.fzm.chat.core.rtc.calling.RTCCallingDelegate
import com.fzm.chat.core.rtc.data.isOnCalling
import com.fzm.widget.dialog.EasyDialog
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.getViewModel
import java.util.concurrent.TimeUnit

/**
 * @author zhengjy
 * @since 2021/07/09
 * Description:
 */
abstract class BaseCallFragment : BizFragment() {

    companion object {
        const val ACTION_NOTHING = 0
        const val ACTION_ACCEPT = 1
        const val ACTION_REJECT = 2

        const val END_CALL_DELAY = 1200L
    }

    protected val viewModel by lazy { requireActivity().getViewModel<RTCCallViewModel>() }

    protected val rtcCall by inject<RTCCalling>()

    /**
     * 操作类型
     * 0：不操作
     * 1：接听
     * 2：拒接
     */
    protected var action = 0

    protected val curTask: RTCTask
        get() = rtcCall.getCurrentTask()

    protected var targetId: String = ""
    protected var callType: Int = 0

    override fun initView(view: View, savedInstanceState: Bundle?) {
        rtcCall.addDelegate(delegate)
        action = arguments?.getInt("action", 0) ?: 0
    }

    abstract val rtcType: Int

    abstract val delegate: RTCCallingDelegate

    protected fun endCall() {
        try {
            if (rtcCall.getCurrentTask().isOnCalling) {
                rtcCall.hangup()
            } else {
                rtcCall.reject()
            }
        } catch (e: Exception) {
            viewModel.closeActivity()
        }
    }

    override fun onDestroy() {
        rtcCall.removeDelegate(delegate)
        super.onDestroy()
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

    protected fun formatTime(millis: Long): String {
        val hourStr = when (val hour = TimeUnit.SECONDS.toHours(millis)) {
            0L -> ""
            in 1..9 -> "0$hour:"
            else -> "$hour:"
        }

        val minStr = when (val min = TimeUnit.SECONDS.toMinutes(millis) % 60) {
            0L -> "00"
            in 1..9 -> "0$min"
            else -> min.toString()
        }

        val secStr = when (val sec = TimeUnit.SECONDS.toSeconds(millis) % 60) {
            0L -> "00"
            in 1..9 -> "0$sec"
            else -> sec.toString()
        }
        return "${hourStr}${minStr}:${secStr}"
    }
}