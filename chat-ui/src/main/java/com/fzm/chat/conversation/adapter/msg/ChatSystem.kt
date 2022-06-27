package com.fzm.chat.conversation.adapter.msg

import com.chad.library.adapter.base.provider.BaseItemProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.utils.StringUtils
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2020/03/09
 * Description:
 */
class ChatSystem : BaseItemProvider<ChatMessage>() {

    override val itemViewType: Int = Biz.MsgType.System_VALUE

    override val layoutId: Int
        get() = R.layout.item_msg_system

    override fun convert(helper: BaseViewHolder, item: ChatMessage) {
        if (item.showTime) {
            helper.setGone(R.id.tv_message_time, false)
            helper.setText(R.id.tv_message_time, StringUtils.timeFormat(context, item.datetime))
        } else {
            helper.setGone(R.id.tv_message_time, true)
        }
        helper.setText(R.id.tv_content, item.msg.content)
    }
}