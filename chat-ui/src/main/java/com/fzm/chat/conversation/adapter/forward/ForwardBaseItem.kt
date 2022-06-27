package com.fzm.chat.conversation.adapter.forward

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.*
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.viewbinding.ViewBinding
import com.chad.library.adapter.base.provider.BaseItemProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.core.data.model.ForwardMsg
import com.fzm.chat.utils.StringUtils
import com.zjy.architecture.ext.haptic
import com.zjy.architecture.ext.layoutInflater
import com.zjy.architecture.ext.load

/**
 * @author zhengjy
 * @since 2021/06/23
 * Description:
 */
abstract class ForwardBaseItem(
    private val listener: ForwardMessageClickListener
) : BaseItemProvider<ForwardMsg>() {

    private var blockLongClick = false

    override val layoutId: Int
        get() = R.layout.item_forward_normal

    open fun messageLayout(holder: BaseViewHolder): ViewGroup = holder.getView<FrameLayout>(R.id.message_layout)

    protected inline fun <reified T : ViewBinding> getOrCreateBinding(holder: BaseViewHolder): T {
        val messageLayout = messageLayout(holder)
        return if (messageLayout.tag != null) {
            messageLayout.tag as T
        } else {
            (T::class.java.getMethod(
                "inflate",
                LayoutInflater::class.java,
                ViewGroup::class.java,
                Boolean::class.java
            )
                .invoke(null, context.layoutInflater, messageLayout, true) as T)
                .also { messageLayout.tag = it }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun convert(helper: BaseViewHolder, item: ForwardMsg) {
        helper.setText(R.id.tv_name, item.name)
        helper.getView<ChatAvatarView>(R.id.iv_avatar)
            .load(item.avatar, R.mipmap.default_avatar_round)
        if (item.showTime) {
            helper.setGone(R.id.tv_message_time, false)
            helper.setText(R.id.tv_message_time, StringUtils.timeFormat(context, item.datetime))
        } else {
            helper.setGone(R.id.tv_message_time, true)
        }
        setupView(helper, item)
        val chatLayout = chatLayout(helper)
        if (chatLayout != null) {
            val detector =
                GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDown(e: MotionEvent): Boolean {
                        return true
                    }

                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        chatLayout.performClick()
                        listener.onChatLayoutClick(chatLayout, item, this@ForwardBaseItem)
                        return true
                    }

                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        listener.onChatLayoutDoubleTap(chatLayout, item)
                        return true
                    }

                    override fun onLongPress(e: MotionEvent) {
                        chatLayout.haptic(HapticFeedbackConstants.LONG_PRESS)
                        blockLongClick = listener.onChatLayoutLongClick(chatLayout, item)
                    }
                })
            chatLayout.setOnTouchListener(View.OnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> onActionDown(view, item)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (!blockLongClick) {
                            onActionUp(view, item)
                        } else {
                            blockLongClick = false
                        }
                    }
                }
                val process = listener.onTouchChatMainView(chatLayout, event)
                if (!process) {
                    detector.onTouchEvent(event)
                } else {
                    true
                }
            })
        }
    }

    abstract fun setupView(holder: BaseViewHolder, item: ForwardMsg)

    open fun onActionDown(view: View, item: ForwardMsg) {
        ViewCompat.setBackgroundTintList(
            view,
            ColorStateList.valueOf(context.resources.getColor(R.color.biz_color_divider))
        )
        ViewCompat.setBackgroundTintMode(view, PorterDuff.Mode.SRC_IN)
    }

    open fun onActionUp(view: View, item: ForwardMsg) {
        ViewCompat.setBackgroundTintList(view, null)
    }

    open fun chatLayout(holder: BaseViewHolder): View? {
        return null
    }

    interface ForwardMessageClickListener {

        /**
         * 触摸ChatMainView
         */
        fun onTouchChatMainView(view: View, event: MotionEvent?): Boolean

        /**
         * 点击聊天布局
         */
        fun onChatLayoutClick(view: View, message: ForwardMsg, item: ForwardBaseItem)

        /**
         * 双击聊天布局
         */
        fun onChatLayoutDoubleTap(view: View, message: ForwardMsg)

        /**
         * 长按聊天布局
         *
         * @return  是否阻塞长按事件，例如：显示弹窗
         *          如果返回true，消息背景保持按压时的状态
         *          返回false，消息背景恢复原状
         */
        fun onChatLayoutLongClick(view: View, message: ForwardMsg): Boolean
    }
}