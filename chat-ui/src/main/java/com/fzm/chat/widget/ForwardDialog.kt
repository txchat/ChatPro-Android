package com.fzm.chat.widget

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import com.fzm.chat.R
import com.fzm.chat.biz.utils.EllipsizeUtils
import com.fzm.chat.conversation.adapter.msg.ChatImage
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.getSourceText
import com.fzm.chat.core.data.po.getDisplayUrl
import com.fzm.chat.databinding.DialogForwardBinding
import com.fzm.chat.databinding.LayoutDialogForwardMediaBinding
import com.fzm.chat.databinding.LayoutDialogForwardTextBinding
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.visible
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/06/24
 * Description:消息转发确认对话框
 */
class ForwardDialog(
    context: Context,
    private val messages: List<ChatMessage>,
    contact: Contact,
    onSend: (messages: List<ChatMessage>) -> Unit
) : Dialog(context) {

    private val binding: DialogForwardBinding

    init {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.attributes = window?.attributes?.apply {
            gravity = Gravity.CENTER
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        binding = DialogForwardBinding.inflate(LayoutInflater.from(context))
        window?.setContentView(binding.root)

        if (contact.getType() == ChatConst.PRIVATE_CHANNEL) {
            binding.ivAvatar.load(contact.getDisplayImage(), R.mipmap.default_avatar_round)
        } else {
            binding.ivAvatar.load(contact.getDisplayImage(), R.mipmap.default_avatar_room)
        }
        binding.tvName.text = contact.getDisplayName()
        setupMessageView()
        binding.tvCancel.setOnClickListener { cancel() }
        binding.tvSend.setOnClickListener {
            dismiss()
            onSend.invoke(messages)
        }
    }

    private fun setupMessageView() {
        if (messages.size > 1) {
            val layout = LayoutDialogForwardTextBinding.inflate(LayoutInflater.from(context))
            EllipsizeUtils.ellipsize(layout.tvText, "[逐条转发]共${messages.size}条消息")
            binding.flMessage.addView(layout.root)
        } else {
            val item = messages[0]
            when (Biz.MsgType.forNumber(item.msgType)) {
                Biz.MsgType.Text -> {
                    val layout =
                        LayoutDialogForwardTextBinding.inflate(LayoutInflater.from(context))
                    EllipsizeUtils.ellipsize(layout.tvText, item.msg.content ?: "")
                    binding.flMessage.addView(layout.root)
                }
                Biz.MsgType.Image -> {
                    val layout =
                        LayoutDialogForwardMediaBinding.inflate(LayoutInflater.from(context))
                    val size = verifyImageSize(item.msg.height, item.msg.width)
                    layout.ivImage.layoutParams = layout.ivImage.layoutParams.apply {
                        height = size[0]
                        width = size[1]
                    }
                    layout.ivImage.load(
                        item.msg.getDisplayUrl(context),
                        R.drawable.biz_image_placeholder
                    )
                    layout.ivState.gone()
                    binding.flMessage.addView(layout.root)
                }
                Biz.MsgType.Video -> {
                    val layout =
                        LayoutDialogForwardMediaBinding.inflate(LayoutInflater.from(context))
                    val size = verifyImageSize(item.msg.height, item.msg.width)
                    layout.ivImage.layoutParams = layout.ivImage.layoutParams.apply {
                        height = size[0]
                        width = size[1]
                    }
                    layout.ivImage.load(
                        item.msg.getDisplayUrl(context),
                        R.drawable.biz_image_placeholder
                    )
                    layout.ivState.visible()
                    binding.flMessage.addView(layout.root)
                }
                Biz.MsgType.File -> {
                    val layout =
                        LayoutDialogForwardTextBinding.inflate(LayoutInflater.from(context))
                    EllipsizeUtils.ellipsize(layout.tvText, "${context.getString(R.string.core_msg_type6)}${item.msg.fileName}")
                    binding.flMessage.addView(layout.root)
                }
                Biz.MsgType.Forward -> {
                    val layout =
                        LayoutDialogForwardTextBinding.inflate(LayoutInflater.from(context))
                    EllipsizeUtils.ellipsize(layout.tvText, "${context.getString(R.string.core_msg_type8)}${item.getSourceText(context)}")
                    binding.flMessage.addView(layout.root)
                }
                Biz.MsgType.ContactCard -> {
                    val type = when (item.msg.contactType) {
                        1 -> context.getString(R.string.core_msg_type13_1)
                        2 -> context.getString(R.string.core_msg_type13_2)
                        3 -> context.getString(R.string.core_msg_type13_3)
                        else -> return
                    }
                    val layout =
                        LayoutDialogForwardTextBinding.inflate(LayoutInflater.from(context))
                    EllipsizeUtils.ellipsize(layout.tvText, "${type}${item.msg.contactName}")
                    binding.flMessage.addView(layout.root)
                }
                // 其他类型消息忽略
                else -> binding.flMessage.layoutParams = binding.flMessage.layoutParams.apply {
                    // 调整高度，避免界面看起来别扭
                    height = 15.dp
                }
            }
        }
    }

    private fun verifyImageSize(bitmapHeight: Int, bitmapWidth: Int): IntArray {
        val result = IntArray(2)
        if (bitmapHeight == 0 || bitmapWidth == 0) {
            result[0] = (ChatImage.MAX + ChatImage.MIN) / 2
            result[1] = (ChatImage.MAX + ChatImage.MIN) / 2
            return result
        }
        val isWidthImage = bitmapWidth - bitmapHeight >= 0
        if (isWidthImage) {
            //宽图
            when {
                bitmapHeight < ChatImage.MIN -> {
                    val ratio = bitmapWidth.toFloat() / bitmapHeight
                    result[0] = ChatImage.MIN
                    result[1] = (ratio * ChatImage.MIN).toInt().coerceAtMost(ChatImage.MAX)
                }
                bitmapWidth > ChatImage.MAX -> {
                    val ratio = bitmapHeight.toFloat() / bitmapWidth
                    result[0] = (ratio * ChatImage.MAX).toInt().coerceAtLeast(ChatImage.MIN)
                    result[1] = ChatImage.MAX
                }
                else -> {
                    result[0] = bitmapHeight
                    result[1] = bitmapWidth
                }
            }
        } else {
            //长图
            when {
                bitmapWidth < ChatImage.MIN -> {
                    val ratio = bitmapHeight.toFloat() / bitmapWidth
                    result[0] = (ratio * ChatImage.MIN).toInt().coerceAtMost(ChatImage.MAX)
                    result[1] = ChatImage.MIN
                }
                bitmapHeight > ChatImage.MAX -> {
                    val ratio = bitmapWidth.toFloat() / bitmapHeight
                    result[0] = ChatImage.MAX
                    result[1] = (ratio * ChatImage.MAX).toInt().coerceAtLeast(ChatImage.MIN)
                }
                else -> {
                    result[0] = bitmapHeight
                    result[1] = bitmapWidth
                }
            }
        }
        return result
    }
}