package com.fzm.chat.conversation.adapter.forward

import android.view.View
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.core.data.model.ForwardMsg
import com.fzm.chat.databinding.LayoutForwardTextBinding
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/06/23
 * Description:
 */
open class ForwardText(listener: ForwardMessageClickListener) : ForwardBaseItem(listener) {

    override val itemViewType: Int = Biz.MsgType.Text_VALUE

    override fun setupView(holder: BaseViewHolder, item: ForwardMsg) {
        val binding = getOrCreateBinding<LayoutForwardTextBinding>(holder)
        binding.tvMessage.setNeedForceEventToParent(true)
        binding.tvMessage.text = item.msg.content
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutForwardTextBinding>(holder).root
    }
}