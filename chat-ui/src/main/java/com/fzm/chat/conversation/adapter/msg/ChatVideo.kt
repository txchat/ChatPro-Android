package com.fzm.chat.conversation.adapter.msg

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.localExists
import com.fzm.chat.core.data.po.getDisplayUrl
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.LayoutMsgVideoBinding
import com.qmuiteam.qmui.widget.QMUIProgressBar
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.visible
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2020/03/15
 * Description:
 */
class ChatVideo(listener: ChatMessageClickListener) : ChatBaseItem(listener) {

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
        get() = Biz.MsgType.Video_VALUE

    override val layoutId: Int
        get() = R.layout.item_msg_normal

    override fun setupView(holder: BaseViewHolder, item: ChatMessage) {
        val binding = getOrCreateBinding<LayoutMsgVideoBinding>(holder)
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

        setProgress(binding.pbVideo, binding.ivState, item, item.progress)
    }

    override fun setupView(holder: BaseViewHolder, item: ChatMessage, bundle: Bundle) {
        val binding = getOrCreateBinding<LayoutMsgVideoBinding>(holder)
        bundle.getFloat(ChatMessage.MSG_PROGRESS).also {
            setProgress(binding.pbVideo, binding.ivState, item, it)
        }
    }

    private fun setProgress(pbVideo: QMUIProgressBar, ivState: ImageView, item: ChatMessage, progress: Float) {
        if (progress == 0f || progress == 1f) {
            pbVideo.gone()
            ivState.visible()
        } else {
            if (!item.localExists()) {
                if (pbVideo.isGone) {
                    pbVideo.visible()
                }
                if (ivState.isVisible) {
                    ivState.gone()
                }
                pbVideo.progress = (progress * 100).toInt()
            } else {
                pbVideo.gone()
                ivState.visible()
            }
        }
    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutMsgVideoBinding>(holder).ivImage
    }

    override fun onActionDown(view: View, item: ChatMessage) {

    }

    override fun onActionUp(view: View, item: ChatMessage) {

    }

    private fun verifyThumbSize(bitmapHeight: Int, bitmapWidth: Int): IntArray {
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