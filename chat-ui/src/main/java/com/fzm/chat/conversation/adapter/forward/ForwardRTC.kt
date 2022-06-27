package com.fzm.chat.conversation.adapter.forward

import android.view.View
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.model.ForwardMsg
import com.fzm.chat.databinding.LayoutForwardTextBinding
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/07/23
 * Description:
 */
open class ForwardRTC(listener: ForwardMessageClickListener) : ForwardBaseItem(listener) {

    override val itemViewType: Int = Biz.MsgType.RTCCall_VALUE

    override fun setupView(holder: BaseViewHolder, item: ForwardMsg) {
        val binding = getOrCreateBinding<LayoutForwardTextBinding>(holder)
        binding.tvMessage.setNeedForceEventToParent(true)
        binding.tvMessage.text = context.getString(R.string.core_msg_type_special)
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutForwardTextBinding>(holder).root
    }
}