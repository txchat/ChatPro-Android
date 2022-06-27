package com.fzm.chat.conversation.adapter.msg

import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.view.View
import androidx.core.view.ViewCompat
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.LayoutMsgFileBinding
import com.zjy.architecture.util.FileUtils
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2020/03/15
 * Description:
 */
class ChatFile(listener: ChatMessageClickListener) : ChatBaseItem(listener) {

    override val itemViewType: Int = Biz.MsgType.File_VALUE

    override fun setupView(holder: BaseViewHolder, item: ChatMessage) {
        val binding = getOrCreateBinding<LayoutMsgFileBinding>(holder)
        binding.tvFileName.text = item.msg.fileName
        binding.tvFileSize.text = context.getString(R.string.chat_file_size, Utils.byteToSize(item.msg.size))
        when (FileUtils.getExtension(item.msg.fileName)) {
            "doc", "docx" -> binding.ivFileType.setImageResource(R.mipmap.icon_file_doc)
            "pdf" -> binding.ivFileType.setImageResource(R.mipmap.icon_file_pdf)
            "ppt", "pptx" -> binding.ivFileType.setImageResource(R.mipmap.icon_file_other)
            "xls", "xlsx" -> binding.ivFileType.setImageResource(R.mipmap.icon_file_xls)
            "mp3", "wma", "wav", "ogg" -> binding.ivFileType.setImageResource(R.mipmap.icon_file_music)
            "mp4", "avi", "rmvb", "flv", "f4v", "mpg", "mkv" -> binding.ivFileType.setImageResource(R.mipmap.icon_file_video)
            else -> binding.ivFileType.setImageResource(R.mipmap.icon_file_other)
        }

    }

    override fun chatLayout(holder: BaseViewHolder): View? {
        return getOrCreateBinding<LayoutMsgFileBinding>(holder).root
    }

    override fun onActionDown(view: View, item: ChatMessage) {
        val colors = ColorStateList.valueOf(context.resources.getColor(R.color.biz_color_divider))
        ViewCompat.setBackgroundTintList(view, colors)
        ViewCompat.setBackgroundTintMode(view, PorterDuff.Mode.SRC_IN)
    }
}