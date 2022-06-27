package com.fzm.chat.conversation.adapter.forward

import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.model.ForwardMsg
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/06/23
 * Description:
 */
class ForwardAudio(listener: ForwardMessageClickListener) : ForwardText(listener) {

    override val itemViewType: Int = Biz.MsgType.Audio_VALUE

    override fun setupView(holder: BaseViewHolder, item: ForwardMsg) {
        super.setupView(holder, item)
        holder.setText(R.id.tv_message, context.getString(R.string.core_msg_type2))
    }
}