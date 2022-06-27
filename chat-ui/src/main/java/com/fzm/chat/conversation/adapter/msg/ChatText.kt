package com.fzm.chat.conversation.adapter.msg

import android.view.View
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.isSendType
import com.fzm.chat.databinding.LayoutMsgTextBinding
import com.fzm.chat.router.biz.BizModule
import com.qmuiteam.qmui.widget.textview.QMUILinkTextView
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2020/12/25
 * Description:
 */
class ChatText(listener: ChatMessageClickListener) : ChatBaseItem(listener) {

    override val itemViewType: Int = Biz.MsgType.Text_VALUE

    override fun setupView(holder: BaseViewHolder, item: ChatMessage) {
        val binding = getOrCreateBinding<LayoutMsgTextBinding>(holder)
        if (item.isSendType) {
            binding.root.setBackgroundResource(R.drawable.img_chat_send)
        } else {
            binding.root.setBackgroundResource(R.drawable.img_chat_receive)
        }
        binding.tvMessage.setNeedForceEventToParent(true)
        binding.tvMessage.setOnLinkClickListener(object : QMUILinkTextView.OnLinkClickListener {
            override fun onTelLinkClick(phoneNumber: String?) {
                
            }

            override fun onMailLinkClick(mailAddress: String?) {
                
            }

            override fun onWebUrlLinkClick(url: String?) {
                ARouter.getInstance().build(BizModule.WEB_ACTIVITY)
                    .withString("url", url)
                    .withBoolean("showTitle", true)
                    .navigation()
            }
        })
        binding.tvMessage.text = item.msg.content
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutMsgTextBinding>(holder).root
    }
}