package com.fzm.chat.conversation.adapter.forward

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.conversation.adapter.msg.ChatImage
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.ForwardMsg
import com.fzm.chat.core.data.model.contact
import com.fzm.chat.core.glide.ForwardModel
import com.fzm.chat.databinding.LayoutForwardImageBinding
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/06/23
 * Description:
 */
open class ForwardImage(
    private val batchMsg: ChatMessage?,
    listener: ForwardMessageClickListener
) : ForwardBaseItem(listener) {

    override val itemViewType: Int = Biz.MsgType.Image_VALUE

    private val options by lazy {
        RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.biz_image_placeholder)
            .error(R.drawable.biz_image_placeholder)
    }

    override fun setupView(holder: BaseViewHolder, item: ForwardMsg) {
        val binding = getOrCreateBinding<LayoutForwardImageBinding>(holder)
        val size = verifyImageSize(item.msg.height, item.msg.width)
        binding.ivImage.layoutParams = binding.ivImage.layoutParams.apply {
            height = size[0]
            width = size[1]
        }
        Glide.with(context).load(ForwardModel(item, batchMsg?.contact ?: ""))
            .thumbnail(0.1f)
            .apply(options)
            .into(binding.ivImage)
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutForwardImageBinding>(holder).root
    }

    override fun onActionDown(view: View, item: ForwardMsg) {

    }

    override fun onActionUp(view: View, item: ForwardMsg) {

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