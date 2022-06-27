package com.fzm.chat.conversation.adapter.msg

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.databinding.LayoutMsgImageBinding
import com.zjy.architecture.ext.dp
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/01/29
 * Description:
 */
class ChatImage(listener: ChatMessageClickListener) : ChatBaseItem(listener) {

    companion object {

        val MAX: Int = 180.dp

        val MIN: Int = 80.dp
    }

    private val options by lazy {
        RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.biz_image_placeholder)
            .error(R.drawable.biz_image_placeholder)
    }

    override val itemViewType: Int
        get() = Biz.MsgType.Image_VALUE

    override fun setupView(holder: BaseViewHolder, item: ChatMessage) {
        val binding = getOrCreateBinding<LayoutMsgImageBinding>(holder)
        val size = verifyImageSize(item.msg.height, item.msg.width)
        binding.ivImage.layoutParams = binding.ivImage.layoutParams.apply {
            height = size[0]
            width = size[1]
        }
        Glide.with(context).load(item)
            .thumbnail(0.1f)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(options)
            .into(binding.ivImage)
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutMsgImageBinding>(holder).ivImage
    }

    override fun onActionDown(view: View, item: ChatMessage) {

    }

    override fun onActionUp(view: View, item: ChatMessage) {

    }

    private fun verifyImageSize(bitmapHeight: Int, bitmapWidth: Int): IntArray {
        val result = IntArray(2)
        if (bitmapHeight == 0 || bitmapWidth == 0) {
            result[0] = (MAX + MIN) / 2
            result[1] = (MAX + MIN) / 2
            return result
        }
        val isWidthImage = bitmapWidth - bitmapHeight >= 0
        if (isWidthImage) {
            //宽图
            when {
                bitmapHeight < MIN -> {
                    val ratio = bitmapWidth.toFloat() / bitmapHeight
                    result[0] = MIN
                    result[1] = (ratio * MIN).toInt().coerceAtMost(MAX)
                }
                bitmapWidth > MAX -> {
                    val ratio = bitmapHeight.toFloat() / bitmapWidth
                    result[0] = (ratio * MAX).toInt().coerceAtLeast(MIN)
                    result[1] = MAX
                }
                else -> {
                    result[0] = bitmapHeight
                    result[1] = bitmapWidth
                }
            }
        } else {
            //长图
            when {
                bitmapWidth < MIN -> {
                    val ratio = bitmapHeight.toFloat() / bitmapWidth
                    result[0] = (ratio * MIN).toInt().coerceAtMost(MAX)
                    result[1] = MIN
                }
                bitmapHeight > MAX -> {
                    val ratio = bitmapWidth.toFloat() / bitmapHeight
                    result[0] = MAX
                    result[1] = (ratio * MAX).toInt().coerceAtLeast(MIN)
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