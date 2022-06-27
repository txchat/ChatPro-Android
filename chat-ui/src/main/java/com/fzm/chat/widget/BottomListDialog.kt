package com.fzm.chat.widget

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.fzm.chat.R
import com.fzm.chat.databinding.DialogBottomListBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.setVisible
import com.zjy.architecture.ext.visible

/**
 * @author zhengjy
 * @since 2021/03/24
 * Description:
 */
class BottomListDialog private constructor(
    context: Context,
) : BottomSheetDialog(context) {

    internal lateinit var binding: DialogBottomListBinding

    companion object {
        fun create(
            context: Context,
            adapter: BaseAdapter,
            maxHeight: Int
        ): BottomListDialog {
            val dialog = BottomListDialog(context)
            dialog.binding = DialogBottomListBinding.inflate(LayoutInflater.from(context))
            dialog.setContentView(dialog.binding.root)
            val container = dialog.delegate.findViewById(R.id.design_bottom_sheet) as? FrameLayout
            container?.setBackgroundColor(context.resources.getColor(android.R.color.transparent))
            dialog.binding.llCancel.setOnClickListener { dialog.dismiss() }
            dialog.binding.lvMenu.layoutParams =
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, maxHeight)
            dialog.binding.lvMenu.adapter = adapter
            dialog.binding.lvMenu.isVerticalScrollBarEnabled = false
            dialog.binding.lvMenu.divider = ColorDrawable(context.resources.getColor(R.color.biz_color_divider))
            dialog.binding.lvMenu.dividerHeight = 0.5f.dp

            dialog.binding.llCancel.setOnClickListener { dialog.cancel() }
            return dialog
        }
    }

    fun setTitle(title: String?): BottomListDialog {
        if (title.isNullOrEmpty()) {
            binding.llTitle.gone()
        } else {
            binding.llTitle.visible()
            binding.tvTitle.text = title
        }
        return this
    }

    fun setCancelVisible(visible: Boolean): BottomListDialog {
        binding.llCancel.setVisible(visible)
        return this
    }

    fun setOnItemClickListener(onItemClickListener: AdapterView.OnItemClickListener): BottomListDialog {
        binding.lvMenu.onItemClickListener = onItemClickListener
        return this
    }
}