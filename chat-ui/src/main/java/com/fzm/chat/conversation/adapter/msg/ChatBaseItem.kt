package com.fzm.chat.conversation.adapter.msg

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.chad.library.adapter.base.provider.BaseItemProvider
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.conversation.adapter.MessageAdapter
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.getGroupRole
import com.fzm.chat.core.data.model.*
import com.fzm.chat.core.data.po.MessageSource
import com.fzm.chat.core.data.po.Reference
import com.fzm.chat.utils.StringUtils
import com.fzm.chat.widget.ChatRelativeLayout
import com.fzm.chat.widget.MsgStateView
import com.fzm.chat.widget.VideoIconTransformation
import com.zjy.architecture.ext.*
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2020/12/25
 * Description:
 */
abstract class ChatBaseItem(
    protected val listener: ChatMessageClickListener
) : BaseItemProvider<ChatMessage>() {

    companion object {
        /**
         * 最大消息选择数目
         */
        const val MAX_SELECT_NUM = 50
    }

    /**
     * 达到最大选择消息数目
     */
    private var reachMaxSelected = false

    protected val adapter: MessageAdapter?
        get() = getAdapter() as? MessageAdapter

    override val layoutId: Int
        get() = R.layout.item_msg_normal

    protected open fun messageLayout(holder: BaseViewHolder): ViewGroup {
        return holder.getView<FrameLayout>(R.id.message_layout)
    }

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

    override fun convert(helper: BaseViewHolder, item: ChatMessage) {
        if (item.channelType == ChatConst.PRIVATE_CHANNEL) {
            helper.setGone(R.id.ll_name, true)
        } else {
            helper.setGone(R.id.ll_name, false)
            helper.setText(R.id.tv_name, item.sender?.getDisplayName())
        }
        helper.getView<ChatAvatarView>(R.id.iv_avatar).load(item.sender?.getDisplayImage(), R.mipmap.default_avatar_round)
        if (item.showTime) {
            helper.setGone(R.id.tv_message_time, false)
            helper.setText(R.id.tv_message_time, StringUtils.timeFormat(context, item.datetime))
        } else {
            helper.setGone(R.id.tv_message_time, true)
        }
        when (item.sender?.getGroupRole(GroupUser.LEVEL_USER)) {
            GroupUser.LEVEL_USER -> {
                helper.setGone(R.id.tv_tag, true)
            }
            GroupUser.LEVEL_ADMIN -> {
                helper.setGone(R.id.tv_tag, false)
                helper.setText(R.id.tv_tag, R.string.chat_tips_member_type_admin)
                helper.setTextColor(R.id.tv_tag, ResourcesCompat.getColor(context.resources, R.color.biz_orange_tips, null))
                helper.setBackgroundResource(R.id.tv_tag, R.drawable.shape_orange_r4)
            }
            GroupUser.LEVEL_OWNER -> {
                helper.setGone(R.id.tv_tag, false)
                helper.setText(R.id.tv_tag, R.string.chat_tips_member_type_owner)
                helper.setTextColor(R.id.tv_tag, ResourcesCompat.getColor(context.resources, R.color.biz_color_accent, null))
                helper.setBackgroundResource(R.id.tv_tag, R.drawable.shape_blue_r4)
            }
        }
        updateSource(helper, item)
        updateReference(helper, item)
        updateFocus(helper, item)

        val layoutRow = helper.getView<View>(R.id.layout_row)
        if (item.isSendType) {
            layoutRow.layoutDirection = View.LAYOUT_DIRECTION_LTR
        } else {
            layoutRow.layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        setupView(helper, item)
        setupItemClickListener(helper, item)
    }

    override fun convert(helper: BaseViewHolder, item: ChatMessage, payloads: List<Any>) {
        if (payloads.isEmpty()) {
            convert(helper, item)
        } else {
            val bundle = payloads[0] as Bundle
            bundle.getString(ChatMessage.MSG_TIME)?.also {
                if (item.showTime) {
                    helper.setGone(R.id.tv_message_time, false)
                    helper.setText(R.id.tv_message_time, StringUtils.timeFormat(context, item.datetime))
                } else {
                    helper.setGone(R.id.tv_message_time, true)
                }
            }
            bundle.getString(ChatMessage.MSG_AVATAR)?.also {
                helper.getView<ChatAvatarView>(R.id.iv_avatar).load(it, R.mipmap.default_avatar_round)
            }
            bundle.getString(ChatMessage.MSG_NICKNAME)?.also {
                if (item.channelType == ChatConst.PRIVATE_CHANNEL) {
                    helper.setGone(R.id.ll_name, true)
                } else {
                    helper.setGone(R.id.ll_name, false)
                    helper.setText(R.id.tv_name, it)
                }
            }
            bundle.getInt(ChatMessage.MSG_TAG).also {
                when (item.sender?.getGroupRole(GroupUser.LEVEL_USER)) {
                    GroupUser.LEVEL_USER -> {
                        helper.setGone(R.id.tv_tag, true)
                    }
                    GroupUser.LEVEL_ADMIN -> {
                        helper.setGone(R.id.tv_tag, false)
                        helper.setText(R.id.tv_tag, R.string.chat_tips_member_type_admin)
                        helper.setTextColor(R.id.tv_tag, ResourcesCompat.getColor(context.resources, R.color.biz_orange_tips, null))
                        helper.setBackgroundResource(R.id.tv_tag, R.drawable.shape_orange_r4)
                    }
                    GroupUser.LEVEL_OWNER -> {
                        helper.setGone(R.id.tv_tag, false)
                        helper.setText(R.id.tv_tag, R.string.chat_tips_member_type_owner)
                        helper.setTextColor(R.id.tv_tag, ResourcesCompat.getColor(context.resources, R.color.biz_color_accent, null))
                        helper.setBackgroundResource(R.id.tv_tag, R.drawable.shape_blue_r4)
                    }
                }
            }
            bundle.getInt(ChatMessage.MSG_STATE, -1).also {
                if (it != -1) {
                    val state = helper.getView<MsgStateView>(R.id.sent_state)
                    if (item.isSendType) {
                        state.visible()
                        state.setMsgState(disableAllState, item.showSentState, it) { v ->
                            listener.onResendClick(v, item)
                        }
                    } else {
                        state.gone()
                    }
                }
            }
            bundle.getSerializable(ChatMessage.MSG_SOURCE).also {
                if (it is MessageSource) {
                    updateSource(helper, item)
                }
            }
            bundle.getSerializable(ChatMessage.MSG_REFERENCE).also {
                if (it is Reference) {
                    updateReference(helper, item)
                }
            }
            bundle.getInt(ChatMessage.MSG_FOCUS, -1).also {
                if (it != -1) {
                    updateFocus(helper, item)
                }
            }

            setupView(helper, item, bundle)
            setupItemClickListener(helper, item)
        }
    }

    private fun updateSource(helper: BaseViewHolder, item: ChatMessage) {
        helper.getView<TextView>(R.id.tv_forward).textDirection = View.TEXT_DIRECTION_LTR
        if (item.isSendType && item.msgType != Biz.MsgType.Forward_VALUE) {
            // 合并转发的消息下方不用显示转发来源
            item.source?.apply {
                val source = if (channelType == ChatConst.PRIVATE_CHANNEL) {
                    when {
                        from.id == adapter?.delegate?.getAddress() -> {
                            context.getString(R.string.chat_forward_source_me_and_others, target.name)
                        }
                        target.id == adapter?.delegate?.getAddress() -> {
                            context.getString(R.string.chat_forward_source_me_and_others, from.name)
                        }
                        else -> {
                            context.getString(R.string.chat_forward_source_others_and_others, from.name, target.name)
                        }
                    }
                } else {
                    context.getString(R.string.chat_forward_source_group, target.name)
                }
                helper.setText(R.id.tv_forward, context.getString(R.string.chat_forward_source_tips, source))
                helper.setGone(R.id.tv_forward, false)
            } ?: helper.setGone(R.id.tv_forward, true)
        } else {
            helper.setGone(R.id.tv_forward, true)
        }
    }

    private fun updateReference(helper: BaseViewHolder, item: ChatMessage) {
        helper.getView<TextView>(R.id.tv_reference).textDirection = View.TEXT_DIRECTION_LTR
        val reference = item.reference
        val refMsg = item.reference?.refMsg
        if (reference == null) {
            helper.setGone(R.id.tv_reference, true)
        } else {
            if (refMsg != null) {
                when (Biz.MsgType.forNumber(refMsg.msgType)) {
                    Biz.MsgType.Text -> {
                        helper.setText(
                            R.id.tv_reference,
                            "${refMsg.sender?.getDisplayName()}：${refMsg.msg.content}"
                        )
                    }
                    Biz.MsgType.Image, Biz.MsgType.Video -> {
                        val isVideo = refMsg.msgType == Biz.MsgType.Video_VALUE
                        val pre = SpannableStringBuilder()
                        pre.append("${refMsg.sender?.getDisplayName()}：?")
                        val image0 = ImageSpan(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.biz_image_placeholder2,
                                null
                            )!!.apply { setBounds(0, 0, 35.dp, 35.dp) }
                        )
                        pre.setSpan(image0, pre.length - 1, pre.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                        helper.setText(R.id.tv_reference, pre)
                        Glide.with(context).asBitmap().load(refMsg)
                            .apply(
                                RequestOptions()
                                    .override(35.dp, 35.dp)
                                    .placeholder(R.drawable.biz_image_placeholder2)
                                    .transform(CenterCrop(), RoundedCorners(5.dp), VideoIconTransformation(context, isVideo))
                            )
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?
                                ) {
                                    val ssb = SpannableStringBuilder()
                                    ssb.append("${refMsg.sender?.getDisplayName()}：?")
                                    val length = ssb.length
                                    val image = ImageSpan(context, resource)
                                    ssb.setSpan(image, length - 1, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                                    helper.setText(R.id.tv_reference, ssb)
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    val ssb = SpannableStringBuilder()
                                    ssb.append("${refMsg.sender?.getDisplayName()}：?")
                                    val length = ssb.length
                                    val image = ImageSpan(placeholder!!)
                                    ssb.setSpan(image, length - 1, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                                    helper.setText(R.id.tv_reference, ssb)
                                }
                            })
                    }
                    Biz.MsgType.File -> {
                        val ssb = SpannableStringBuilder()
                        ssb.append("${refMsg.sender?.getDisplayName()}：?")
                        val length = ssb.length
                        ssb.append(refMsg.msg.fileName ?: "")
                        val image = ImageSpan(
                            ResourcesCompat.getDrawable(
                                context.resources,
                                R.drawable.ic_reference_file,
                                null
                            )!!.apply { setBounds(0, 0, 13.dp, 13.dp) }
                        )
                        ssb.setSpan(image, length - 1, length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                        helper.setText(R.id.tv_reference, ssb)
                    }
                    Biz.MsgType.Notification -> {
                        // 目前通知无法引用，如果被引用，则说明是消息撤回
                        helper.setText(R.id.tv_reference, "此消息已撤回")
                    }
                }
                helper.setGone(R.id.tv_reference, false)
            } else {
                helper.setText(R.id.tv_reference, "此消息已删除")
                helper.setGone(R.id.tv_reference, false)
            }
        }
    }

    private fun updateFocus(helper: BaseViewHolder, item: ChatMessage) {
        if (item.focusUserNum == 0) {
            helper.setGone(R.id.tv_focus, true)
        } else {
            if (item.isGroup) {
                helper.setText(R.id.tv_focus, "${item.focusUserNum}人已关注")
            } else {
                if (item.isSendType) {
                    helper.setText(R.id.tv_focus, "对方已关注")
                } else {
                    helper.setText(R.id.tv_focus, "我已关注")
                }
            }
            helper.setGone(R.id.tv_focus, false)
        }
    }

    private fun setMsgStateAttachListener(state: MsgStateView, item: ChatMessage) {
        if (state.tag != null) {
            state.removeOnAttachStateChangeListener(state.tag as? View.OnAttachStateChangeListener?)
        }
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View?) {
                if (item.isSendType) {
                    state.visible()
                    state.setMsgState(disableAllState, item.showSentState, item.state) { v ->
                        listener.onResendClick(v, item)
                    }
                } else {
                    state.gone()
                }
            }

            override fun onViewDetachedFromWindow(view: View?) {

            }
        }
        state.addOnAttachStateChangeListener(listener)
        state.tag = listener
    }

    abstract fun setupView(holder: BaseViewHolder, item: ChatMessage)

    open fun setupView(holder: BaseViewHolder, item: ChatMessage, bundle: Bundle) {}

    /**
     * 不显示发送成功和消息送达标记
     */
    open val ChatMessage.showSentState get() = channelType == ChatConst.PRIVATE_CHANNEL

    /**
     * 不显示任何消息状态标记（通常用于本地写入数据库的消息）
     */
    open val disableAllState: Boolean get() = false

    private var blockLongClick = false

    @SuppressLint("ClickableViewAccessibility")
    private fun setupItemClickListener(helper: BaseViewHolder, item: ChatMessage) {
        val layoutRow = helper.getView<ChatRelativeLayout>(R.id.layout_row)
        val rowContainer = helper.getView<View>(R.id.ll_row_container)
        val select = helper.getView<CheckBox>(R.id.cb_select)
        select.setVisible(adapter?.selectable ?: false)
        layoutRow.setSelectable(adapter?.selectable ?: false)
        if (adapter?.selectable == true) {
            select.tag = item
            select.isChecked = item.isSelected
            select.setOnCheckedChangeListener { _, isChecked ->
                if (select.tag == item) {
                    reachMaxSelected =
                        adapter?.data?.filter { it.isSelected }?.size ?: 0 >= MAX_SELECT_NUM
                    if (isChecked && reachMaxSelected) {
                        // 已经达到最大选择数目，则取消当前选择
                        select.isChecked = false
                        listener.onMessageSelectedChanged(item, true)
                        return@setOnCheckedChangeListener
                    }
                    item.isSelected = isChecked
                    listener.onMessageSelectedChanged(item, false)
                }
            }
        } else {
            select.setOnCheckedChangeListener(null)
        }
        rowContainer.isEnabled = adapter?.selectable ?: false
        rowContainer.setOnClickListener { select.performClick() }
        val chatLayout = chatLayout(helper)
        if (chatLayout != null) {
            val detector = GestureDetector(context, object : SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean {
                    return true
                }

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    chatLayout.performClick()
                    listener.onChatLayoutClick(chatLayout, item, this@ChatBaseItem)
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    listener.onChatLayoutDoubleTap(chatLayout, item)
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    chatLayout.haptic(HapticFeedbackConstants.LONG_PRESS)
                    blockLongClick = listener.onChatLayoutLongClick(chatLayout, item, this@ChatBaseItem)
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
        val state = helper.getView<MsgStateView>(R.id.sent_state)
        setMsgStateAttachListener(state, item)
        val ivAvatar = helper.getView<View>(R.id.iv_avatar)
        ivAvatar.singleClick {
            listener.onAvatarClick(it, item)
        }
        ivAvatar.setOnLongClickListener {
            ivAvatar.haptic(HapticFeedbackConstants.LONG_PRESS)
            return@setOnLongClickListener listener.onAvatarLongClick(it, item)
        }
        helper.getView<View>(R.id.tv_reference).singleClick {
            listener.onReferenceClick(it, item)
        }
        helper.getView<View>(R.id.tv_focus).singleClick {
            listener.onFocusClick(it, item)
        }
    }

    open fun onActionDown(view: View, item: ChatMessage) {
        val colors = if (item.isSendType) {
            ColorStateList.valueOf(context.resources.getColor(R.color.biz_color_accent_light_press))
        } else {
            ColorStateList.valueOf(context.resources.getColor(R.color.biz_color_divider))
        }
        ViewCompat.setBackgroundTintList(view, colors)
        ViewCompat.setBackgroundTintMode(view, PorterDuff.Mode.SRC_IN)
    }

    open fun onActionUp(view: View, item: ChatMessage) {
        ViewCompat.setBackgroundTintList(view, null)
    }

    open fun chatLayout(holder: BaseViewHolder): View? {
        return null
    }

    interface ChatMessageClickListener {

        /**
         * 触摸ChatMainView
         */
        fun onTouchChatMainView(view: View, event: MotionEvent?): Boolean

        /**
         * 点击重发按钮
         */
        fun onResendClick(view: View, message: ChatMessage)

        /**
         * 点击聊天布局
         */
        fun onChatLayoutClick(view: View, message: ChatMessage, item: ChatBaseItem)

        /**
         * 双击聊天布局
         */
        fun onChatLayoutDoubleTap(view: View, message: ChatMessage)

        /**
         * 长按聊天布局
         *
         * @return  是否阻塞长按事件，例如：显示弹窗
         *          如果返回true，消息背景保持按压时的状态
         *          返回false，消息背景恢复原状
         */
        fun onChatLayoutLongClick(view: View, message: ChatMessage, item: ChatBaseItem): Boolean

        /**
         * 点击用户头像
         */
        fun onAvatarClick(view: View, message: ChatMessage)

        /**
         * 长按用户头像
         */
        fun onAvatarLongClick(view: View, message: ChatMessage): Boolean

        /**
         * 通知点击事件
         */
        fun onNotificationClick(view: View, message: ChatMessage, type: Int, index: Int)

        /**
         * 引用消息点击事件
         */
        fun onReferenceClick(view: View, message: ChatMessage)

        /**
         * 消息关注点击事件
         */
        fun onFocusClick(view: View, message: ChatMessage)

        /**
         * 消息选择状态变化
         *
         * @param max 达到了最大消息选择数目
         */
        fun onMessageSelectedChanged(message: ChatMessage, max: Boolean)
    }
}