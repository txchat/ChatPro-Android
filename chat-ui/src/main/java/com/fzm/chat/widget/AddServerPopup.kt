package com.fzm.chat.widget

import android.content.Context
import android.graphics.Point
import android.view.View
import android.widget.FrameLayout
import com.fzm.chat.R
import com.fzm.chat.databinding.PopupwindowAddServerBinding
import com.qmuiteam.qmui.widget.popup.QMUIPopup
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.layoutInflater

/**
 * @author zhengjy
 * @since 2020/12/09
 * Description:
 */
class AddServerPopup(
    context: Context?
) : QMUIPopup(context, DIRECTION_BOTTOM) {

    companion object {
        fun create(context: Context, listener: View.OnClickListener): AddServerPopup {
            val popup = AddServerPopup(context)
            val binding = context.layoutInflater?.let { PopupwindowAddServerBinding.inflate(it) }
            binding?.addChatServer?.setOnClickListener {
                listener.onClick(it)
                popup.dismiss()
            }
            binding?.addContractServer?.setOnClickListener {
                listener.onClick(it)
                popup.dismiss()
            }

            binding?.root?.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            popup.setContentView(binding?.root)
            return popup
        }
    }

    override fun onShowBegin(parent: View, attachedView: View): Point {
        return super.onShowBegin(parent, attachedView).also {
            mArrowDown.gone()
            mArrowUp.gone()
        }
    }

    override fun getRootLayout(): Int {
        return R.layout.popup_layout_qmui_chat
    }
}