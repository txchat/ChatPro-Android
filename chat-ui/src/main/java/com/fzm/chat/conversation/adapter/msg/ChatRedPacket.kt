package com.fzm.chat.conversation.adapter.msg

import android.content.res.ColorStateList
import android.view.View
import androidx.core.view.ViewCompat
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.isSendType
import com.fzm.chat.databinding.LayoutMsgRedPacketBinding
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/08/30
 * Description:
 */
class ChatRedPacket(listener: ChatMessageClickListener) : ChatBaseItem(listener) {

    private val pressColor by lazy {
        ColorStateList.valueOf(context.resources.getColor(R.color.biz_red_tips_pressed))
    }
    private val colors by lazy {
        ColorStateList.valueOf(context.resources.getColor(R.color.biz_red_tips_light2))
    }

    override val itemViewType: Int
        get() = Biz.MsgType.RedPacket_VALUE

    override fun setupView(holder: BaseViewHolder, item: ChatMessage) {
        val binding = getOrCreateBinding<LayoutMsgRedPacketBinding>(holder)
        if (item.isSendType && item.channelType == ChatConst.PRIVATE_CHANNEL) {
            if (item.msg.state == 2) {
                ViewCompat.setBackgroundTintList(binding.root, colors)
                binding.tvPacketTips.text = "对方已领取"
                binding.ivPacket.setImageResource(R.drawable.ic_msg_red_packet_light)
            } else {
                ViewCompat.setBackgroundTintList(binding.root, null)
                binding.tvPacketTips.text = "查看红包"
                binding.ivPacket.setImageResource(R.drawable.ic_msg_red_packet)
            }
        } else {
            when (item.msg.state) {
                1 -> {
                    ViewCompat.setBackgroundTintList(binding.root, null)
                    binding.tvPacketTips.text = "查看红包"
                    binding.ivPacket.setImageResource(R.drawable.ic_msg_red_packet)
                }
                2 -> {
                    ViewCompat.setBackgroundTintList(binding.root, colors)
                    binding.tvPacketTips.text = "红包已领完"
                    binding.ivPacket.setImageResource(R.drawable.ic_msg_red_packet_light)
                }
                3 -> {
                    ViewCompat.setBackgroundTintList(binding.root, colors)
                    binding.tvPacketTips.text = "红包已退回"
                    binding.ivPacket.setImageResource(R.drawable.ic_msg_red_packet_light)
                }
                4 -> {
                    ViewCompat.setBackgroundTintList(binding.root, colors)
                    binding.tvPacketTips.text = "红包已过期"
                    binding.ivPacket.setImageResource(R.drawable.ic_msg_red_packet_light)
                }
                5 -> {
                    ViewCompat.setBackgroundTintList(binding.root, colors)
                    binding.tvPacketTips.text = "红包已领取"
                    binding.ivPacket.setImageResource(R.drawable.ic_msg_red_packet_light)
                }
                else ->{
                    ViewCompat.setBackgroundTintList(binding.root, null)
                    binding.tvPacketTips.text = "查看红包"
                    binding.ivPacket.setImageResource(R.drawable.ic_msg_red_packet)
                }
            }
        }
        binding.tvRemark.text = item.msg.remark
    }

    override fun onActionDown(view: View, item: ChatMessage) {
        ViewCompat.setBackgroundTintList(view, pressColor)
    }

    override fun onActionUp(view: View, item: ChatMessage) {
        if (item.isSendType && item.channelType == ChatConst.PRIVATE_CHANNEL) {
            if (item.msg.state == 2) {
                ViewCompat.setBackgroundTintList(view, colors)
            } else {
                ViewCompat.setBackgroundTintList(view, null)
            }
        } else {
            when (item.msg.state) {
                1 -> ViewCompat.setBackgroundTintList(view, null)
                2, 3, 4, 5 -> ViewCompat.setBackgroundTintList(view, colors)
                else -> ViewCompat.setBackgroundTintList(view, null)
            }
        }
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutMsgRedPacketBinding>(holder).root
    }
}