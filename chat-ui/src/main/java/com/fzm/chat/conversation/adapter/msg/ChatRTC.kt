package com.fzm.chat.conversation.adapter.msg

import android.view.View
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.isSendType
import com.fzm.chat.core.rtc.calling.RTCCalling
import com.fzm.chat.core.rtc.data.RTCEndStatus
import com.fzm.chat.databinding.LayoutMsgRtcBinding
import com.zjy.architecture.ext.formatDuration
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/07/22
 * Description:
 */
class ChatRTC(listener: ChatMessageClickListener) : ChatBaseItem(listener) {

    override val disableAllState: Boolean get() = true

    override val itemViewType: Int = Biz.MsgType.RTCCall_VALUE

    override fun setupView(holder: BaseViewHolder, item: ChatMessage) {
        val binding = getOrCreateBinding<LayoutMsgRtcBinding>(holder)
        val drawable = if (item.msg.rtcType == RTCCalling.TYPE_AUDIO_CALL) {
            ContextCompat.getDrawable(context, R.drawable.icon_msg_rtc_audio)?.apply {
                setBounds(0, 0, minimumWidth, minimumHeight)
            }
        } else {
            ContextCompat.getDrawable(context, R.drawable.icon_msg_rtc_video)?.apply {
                setBounds(0, 0, minimumWidth, minimumHeight)
            }
        }
        if (item.isSendType) {
            binding.root.setBackgroundResource(R.drawable.img_chat_send)
            binding.tvMessage.setCompoundDrawables(null, null, drawable, null)
        } else {
            binding.root.setBackgroundResource(R.drawable.img_chat_receive)
            binding.tvMessage.setCompoundDrawables(drawable, null, null, null)
        }
        binding.tvMessage.text = when (item.msg.rtcStatus) {
            RTCEndStatus.NORMAL -> "聊天时长: ${item.msg.duration.formatDuration()}"
            RTCEndStatus.REJECT -> if (item.isSendType) "对方已拒绝" else "已拒绝"
            RTCEndStatus.CANCEL -> if (item.isSendType) "已取消" else "对方已取消"
            RTCEndStatus.BUSY -> if (item.isSendType) "对方忙" else "忙线"
            RTCEndStatus.TIMEOUT -> "连接超时"
            RTCEndStatus.FAIL -> "通话失败"
            else -> "未知错误"
        }
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutMsgRtcBinding>(holder).root
    }
}