package com.fzm.chat.widget

import android.content.Context
import android.graphics.Point
import android.view.View
import android.widget.FrameLayout
import com.fzm.chat.R
import com.fzm.chat.databinding.PopupwindowGroupUserAddBinding
import com.qmuiteam.qmui.widget.popup.QMUIPopup
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.layoutInflater
import com.zjy.architecture.ext.setVisible

/**
 * @author zhengjy
 * @since 2021/05/20
 * Description:
 */
class GroupUserActionPopup(
    context: Context?
) : QMUIPopup(context, DIRECTION_BOTTOM) {

    companion object {
        fun create(context: Context, listener: View.OnClickListener, showAdd: Boolean, showDelete: Boolean, showAdmin: Boolean): GroupUserActionPopup {
            val popup = GroupUserActionPopup(context)
            val binding = context.layoutInflater?.let { PopupwindowGroupUserAddBinding.inflate(it) }
            binding?.addGroupUser?.setVisible(showAdd)
            binding?.addGroupUser?.setOnClickListener {
                listener.onClick(it)
                popup.dismiss()
            }
            binding?.removeGroupUser?.setVisible(showDelete)
            binding?.removeGroupUser?.setOnClickListener {
                listener.onClick(it)
                popup.dismiss()
            }
            binding?.setAdmin?.setVisible(showAdmin)
            binding?.setAdmin?.setOnClickListener {
                listener.onClick(it)
                popup.dismiss()
            }
            binding?.root?.layoutParams = FrameLayout.LayoutParams(
                /*FrameLayout.LayoutParams.WRAP_CONTENT*/120.dp,
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