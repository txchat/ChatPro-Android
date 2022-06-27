package com.fzm.chat.conversation.adapter.forward

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.conversation.adapter.msg.ChatVideo
import com.fzm.chat.core.data.model.ForwardMsg
import com.fzm.chat.core.data.po.getDisplayUrl
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.LayoutForwardVideoBinding
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/06/23
 * Description:
 */
open class ForwardVideo(listener: ForwardMessageClickListener) : ForwardBaseItem(listener) {

    override val itemViewType: Int = Biz.MsgType.Video_VALUE

    private val options by lazy {
        RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.biz_image_placeholder)
            .error(R.drawable.biz_image_placeholder)
    }

    override fun setupView(holder: BaseViewHolder, item: ForwardMsg) {
        val binding = getOrCreateBinding<LayoutForwardVideoBinding>(holder)
        val size = verifyThumbSize(item.msg.height, item.msg.width)
        binding.ivImage.layoutParams = binding.ivImage.layoutParams.apply {
            height = size[0]
            width = size[1]
        }
        Glide.with(context).load(item.msg.getDisplayUrl(context))
            .thumbnail(0.1f)
            .apply(options)
            .into(binding.ivImage)
        binding.tvDuration.text = Utils.formatVideoDuration(item.msg.duration)
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutForwardVideoBinding>(holder).root
    }

    override fun onActionDown(view: View, item: ForwardMsg) {

    }

    override fun onActionUp(view: View, item: ForwardMsg) {

    }

    private fun verifyThumbSize(bitmapHeight: Int, bitmapWidth: Int): IntArray {
        val result = IntArray(2)
        if (bitmapHeight == 0 || bitmapWidth == 0) {
            result[0] = (ChatVideo.MAX + ChatVideo.MIN) / 2
            result[1] = (ChatVideo.MAX + ChatVideo.MIN) / 2
            return result
        }
        val isWidthImage = bitmapWidth - bitmapHeight >= 0
        if (isWidthImage) {
            //宽图
            when {
                bitmapHeight < ChatVideo.MIN -> {
                    val ratio = bitmapWidth.toFloat() / bitmapHeight
                    result[0] = ChatVideo.MIN
                    result[1] = (ratio * ChatVideo.MIN).toInt().coerceAtMost(ChatVideo.MAX)
                }
                bitmapWidth > ChatVideo.MAX -> {
                    val ratio = bitmapHeight.toFloat() / bitmapWidth
                    result[0] = (ratio * ChatVideo.MAX).toInt().coerceAtLeast(ChatVideo.MIN)
                    result[1] = ChatVideo.MAX
                }
                else -> {
                    result[0] = bitmapHeight
                    result[1] = bitmapWidth
                }
            }
        } else {
            //长图
            when {
                bitmapWidth < ChatVideo.MIN -> {
                    val ratio = bitmapHeight.toFloat() / bitmapWidth
                    result[0] = (ratio * ChatVideo.MIN).toInt().coerceAtMost(ChatVideo.MAX)
                    result[1] = ChatVideo.MIN
                }
                bitmapHeight > ChatVideo.MAX -> {
                    val ratio = bitmapWidth.toFloat() / bitmapHeight
                    result[0] = ChatVideo.MAX
                    result[1] = (ratio * ChatVideo.MAX).toInt().coerceAtLeast(ChatVideo.MIN)
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