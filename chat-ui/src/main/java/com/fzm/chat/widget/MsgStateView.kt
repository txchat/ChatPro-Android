package com.fzm.chat.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import androidx.core.view.isGone
import com.fzm.chat.R
import com.fzm.chat.core.data.msg.MsgState
import com.fzm.chat.databinding.ViewMsgStateBinding
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.setVisible
import com.zjy.architecture.ext.visible

/**
 * @author zhengjy
 * @since 2021/04/25
 * Description:
 */
class MsgStateView : FrameLayout {

    private lateinit var binding: ViewMsgStateBinding
    private val sendingAnimation = RotateAnimation(0f, 360f,
        RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f).apply {
        interpolator = LinearInterpolator()
        repeatMode = Animation.INFINITE
        repeatCount = Animation.INFINITE
        duration = 1600
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        binding = ViewMsgStateBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun setMsgState(disableAllState: Boolean, showSentState: Boolean, state: Int, onFailClickListener: OnClickListener? = null) {
        if (disableAllState) {
            binding.msgState.animation?.cancel()
            gone()
            return
        }
        if (isGone) visible()
        when (state) {
            MsgState.SENDING -> {
                binding.msgState.setImageResource(R.drawable.ic_msg_state_sending)
                binding.msgState.layoutParams = binding.msgState.layoutParams.apply {
                    height = 15.dp
                    width = 15.dp
                }
                setOnClickListener(null)
                binding.msgState.startAnimation(sendingAnimation)
            }
            MsgState.FAIL -> {
                binding.msgState.animation?.cancel()
                binding.msgState.setImageResource(R.drawable.ic_msg_state_fail)
                binding.msgState.layoutParams = binding.msgState.layoutParams.apply {
                    height = 15.dp
                    width = 15.dp
                }
                setOnClickListener(onFailClickListener)
            }
            MsgState.SENT -> {
                if (showSentState) {
                    binding.msgState.animation?.cancel()
                    binding.msgState.setImageResource(R.drawable.ic_msg_state_sent)
                    binding.msgState.layoutParams = binding.msgState.layoutParams.apply {
                        height = 22.dp
                        width = 22.dp
                    }
                    setOnClickListener(null)
                } else {
                    binding.msgState.animation?.cancel()
                    setOnClickListener(null)
                }
                setVisible(showSentState)
            }
            MsgState.SENT_AND_RECEIVE -> {
                if (showSentState) {
                    binding.msgState.animation?.cancel()
                    binding.msgState.setImageResource(R.drawable.ic_msg_state_sent_and_receive)
                    binding.msgState.layoutParams = binding.msgState.layoutParams.apply {
                        height = 22.dp
                        width = 22.dp
                    }
                    setOnClickListener(null)
                } else {
                    binding.msgState.animation?.cancel()
                    setOnClickListener(null)
                }
                setVisible(showSentState)
            }
            else -> {
                binding.msgState.setImageResource(R.drawable.ic_msg_state_sending)
                binding.msgState.layoutParams = binding.msgState.layoutParams.apply {
                    height = 15.dp
                    width = 15.dp
                }
                setOnClickListener(null)
                binding.msgState.startAnimation(sendingAnimation)
            }
        }
    }
}