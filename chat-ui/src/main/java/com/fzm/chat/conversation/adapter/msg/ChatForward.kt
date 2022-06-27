package com.fzm.chat.conversation.adapter.msg

import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.isSendType
import com.fzm.chat.databinding.LayoutMsgForwardBinding
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/06/16
 * Description:
 */
class ChatForward(listener: ChatMessageClickListener) : ChatBaseItem(listener) {

    override val itemViewType: Int = Biz.MsgType.Forward_VALUE

    override fun setupView(holder: BaseViewHolder, item: ChatMessage) {
        val binding = getOrCreateBinding<LayoutMsgForwardBinding>(holder)
        if (item.isSendType) {
            binding.root.setBackgroundResource(R.drawable.img_chat_send)
            binding.divider.setBackgroundResource(R.color.biz_color_blue_divider)
            binding.tvMessage.setTextColor(ResourcesCompat.getColor(context.resources, R.color.biz_text_blue_grey_light, null))
            binding.tvSummary.setTextColor(ResourcesCompat.getColor(context.resources, R.color.biz_text_blue_grey_light, null))
        } else {
            binding.root.setBackgroundResource(R.drawable.img_chat_receive)
            binding.divider.setBackgroundResource(R.color.biz_color_divider)
            binding.tvMessage.setTextColor(ResourcesCompat.getColor(context.resources, R.color.biz_text_grey_light, null))
            binding.tvSummary.setTextColor(ResourcesCompat.getColor(context.resources, R.color.biz_text_grey_light, null))
        }
        item.source?.apply {
            val source = if (channelType == ChatConst.PRIVATE_CHANNEL) {
                context.getString(R.string.chat_forward_title_others_and_others, from.name, target.name)
            } else {
                context.getString(R.string.chat_forward_title_group)
            }
            binding.tvTitle.text = source
        } ?: binding.tvTitle.setText("")
        binding.tvMessage.layoutDirection = View.LAYOUT_DIRECTION_LTR
        binding.tvMessage.text = item.msg.content
        binding.tvSummary.text = context.getString(R.string.chat_forward_msg_summary, item.msg.forwardLogs.size)
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutMsgForwardBinding>(holder).root
    }
}