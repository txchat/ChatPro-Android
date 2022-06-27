package com.fzm.chat.conversation.adapter.msg

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.View
import androidx.core.view.ViewCompat
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.databinding.LayoutMsgContactCardBinding
import com.zjy.architecture.ext.load
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/10/03
 * Description:
 */
class ChatContactCard(listener: ChatMessageClickListener) : ChatBaseItem(listener) {

    override val itemViewType: Int = Biz.MsgType.ContactCard_VALUE

    override fun setupView(holder: BaseViewHolder, item: ChatMessage) {
        val binding = getOrCreateBinding<LayoutMsgContactCardBinding>(holder)
        binding.tvName.text = item.msg.contactName
        when (item.msg.contactType) {
            1 -> {
                binding.tvType.text = "个人名片"
                binding.ivAvatar.load(item.msg.contactAvatar, R.mipmap.default_avatar_round)
            }
            2 -> {
                binding.tvType.text = "群名片"
                binding.ivAvatar.load(item.msg.contactAvatar, R.mipmap.default_avatar_room)
            }
            3 -> {
                binding.tvType.text = "团队名片"
                binding.ivAvatar.load(item.msg.contactAvatar, R.mipmap.default_avatar_ep)
            }
        }
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutMsgContactCardBinding>(holder).root
    }

    override fun onActionDown(view: View, item: ChatMessage) {
        val colors = ColorStateList.valueOf(context.resources.getColor(R.color.biz_color_divider))
        ViewCompat.setBackgroundTintList(view, colors)
        ViewCompat.setBackgroundTintMode(view, PorterDuff.Mode.SRC_IN)
    }
}