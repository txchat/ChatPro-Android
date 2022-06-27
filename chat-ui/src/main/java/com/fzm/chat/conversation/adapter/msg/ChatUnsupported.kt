package com.fzm.chat.conversation.adapter.msg

import com.chad.library.adapter.base.provider.BaseItemProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.utils.StringUtils

/**
 * @author zhengjy
 * @since 2020/03/09
 * Description:
 */
class ChatUnsupported : BaseItemProvider<ChatMessage>() {

    override val itemViewType: Int = ChatConst.UNSUPPORTED_MSG_TYPE

    override val layoutId: Int
        get() = R.layout.item_msg_notification

    override fun convert(helper: BaseViewHolder, item: ChatMessage) {
        if (item.showTime) {
            helper.setGone(R.id.tv_message_time, false)
            helper.setText(R.id.tv_message_time, StringUtils.timeFormat(context, item.datetime))
        } else {
            helper.setGone(R.id.tv_message_time, true)
        }
        helper.setText(R.id.tv_content, R.string.chat_message_unsupported_msg_type)
    }
}