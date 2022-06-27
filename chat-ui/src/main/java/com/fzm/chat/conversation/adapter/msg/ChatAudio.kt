package com.fzm.chat.conversation.adapter.msg

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.isSendType
import com.fzm.chat.databinding.LayoutMsgAudioBinding
import com.fzm.chat.media.manager.MediaManager
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.setVisible
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2020/03/09
 * Description:
 */
class ChatAudio(listener: ChatMessageClickListener) : ChatBaseItem(listener) {

    companion object {
        private val left = intArrayOf(
            R.string.icon_yuyin_left_vol1,
            R.string.icon_yuyin_left_vol2,
            R.string.icon_yuyin_left_vol3
        )
        private val right = intArrayOf(
            R.string.icon_yuyin_right_vol1,
            R.string.icon_yuyin_right_vol2,
            R.string.icon_yuyin_right_vol3
        )

        private val maxWidth = 240.dp
        private val minWidth = 60.dp
    }

    override val itemViewType: Int = Biz.MsgType.Audio_VALUE

    override fun setupView(holder: BaseViewHolder, item: ChatMessage) {
        val binding = getOrCreateBinding<LayoutMsgAudioBinding>(holder)
        if (item.isSendType) {
            binding.unread.gone()
            binding.container.setBackgroundResource(R.drawable.img_chat_send)
            binding.iconVoice.setIconText(R.string.icon_yuyin_right_vol3)
        } else {
            binding.unread.setVisible(!item.msg.isRead)
            binding.container.setBackgroundResource(R.drawable.img_chat_receive)
            binding.iconVoice.setIconText(R.string.icon_yuyin_left_vol3)
        }
        binding.iconVoice.setAnimResource(300, if (item.isSendType) right else left)
        if (MediaManager.isPlaying() && MediaManager.currentAudio() == item.msg.mediaUrl) {
            binding.iconVoice.play()
        } else {
            binding.iconVoice.stop()
        }
        setViewWidthByDuration(binding.container, item.msg.duration)
        @SuppressLint("SetTextI18n")
        binding.tvDuration.text = "${item.msg.duration}\""
    }

    override fun setupView(holder: BaseViewHolder, item: ChatMessage, bundle: Bundle) {
        val binding = getOrCreateBinding<LayoutMsgAudioBinding>(holder)
        bundle.getInt(ChatMessage.MSG_READ_STATE, -1).also {
            if (it != -1) {
                if (item.isSendType) {
                    binding.unread.gone()
                } else {
                    binding.unread.setVisible(it != 1)
                }
            }
        }
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutMsgAudioBinding>(holder).container
    }

    /**
     * 根据播放时长设置语音条长度
     */
    private fun setViewWidthByDuration(container: LinearLayout, duration: Int) {
        if (duration == 0) {
            container.layoutParams = container.layoutParams.apply {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        } else {
            val timePercent: Float = duration.toFloat() / ChatConfig.MAX_AUDIO_DURATION
            container.layoutParams = container.layoutParams.apply {
                width = (minWidth + (maxWidth - minWidth) * timePercent).toInt()
            }
        }
    }
}