package com.fzm.chat.conversation.adapter.msg

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.provider.BaseItemProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.utils.StringUtils
import dtalk.biz.Biz
import dtalk.biz.msg.Msg

/**
 * @author zhengjy
 * @since 2021/06/16
 * Description:
 */
class ChatNotification(
    private val listener: ChatBaseItem.ChatMessageClickListener
) : BaseItemProvider<ChatMessage>() {

    override val itemViewType: Int = Biz.MsgType.Notification_VALUE

    override val layoutId: Int
        get() = R.layout.item_msg_notification

    override fun convert(helper: BaseViewHolder, item: ChatMessage) {
        if (item.showTime) {
            helper.setGone(R.id.tv_message_time, false)
            helper.setText(R.id.tv_message_time, StringUtils.timeFormat(context, item.datetime))
        } else {
            helper.setGone(R.id.tv_message_time, true)
        }
        val ssb = SpannableStringBuilder()
        when (Msg.NotificationType.forNumber(item.msg.notificationType)) {
            Msg.NotificationType.RevokeMessage -> {
                ssb.append(item.msg.content ?: "")
                if (!item.msg.reedit.isNullOrEmpty()) {
                    ssb.append(" 重新编辑")
                    val clickSpan = object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            listener.onNotificationClick(widget, item, item.msg.notificationType, 0)
                        }

                        override fun updateDrawState(ds: TextPaint) {
                            ds.color = ds.linkColor
                            ds.isUnderlineText = false
                        }
                    }
                    val colorSpan = ForegroundColorSpan(context.resources.getColor(R.color.biz_color_accent))
                    ssb.setSpan(clickSpan, ssb.length - 4, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    ssb.setSpan(colorSpan, ssb.length - 4, ssb.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            else -> ssb.append(item.msg.content ?: "")
        }
        val tvContent = helper.getView<TextView>(R.id.tv_content)
        tvContent.movementMethod = LinkMovementMethod.getInstance()
        tvContent.text = ssb
    }
}