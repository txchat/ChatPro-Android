package com.fzm.chat.widget

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.fzm.chat.R
import com.fzm.chat.databinding.DialogBottomPhotoSelectBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * @author zhengjy
 * @since 2021/03/19
 * Description:
 */
class SelectPhotoDialog(context: Context) : BottomSheetDialog(context) {

    companion object {
        fun create(context: Context, listener: OnSelectListener): SelectPhotoDialog {
            val binding = DialogBottomPhotoSelectBinding.inflate(LayoutInflater.from(context))
            val dialog = SelectPhotoDialog(context)
            dialog.setContentView(binding.root)
            binding.rlCapture.setOnClickListener {
                listener.onTakePhoto()
                dialog.dismiss()
            }
            binding.rlSelect.setOnClickListener {
                listener.onSelectFromGallery()
                dialog.dismiss()
            }
            binding.rlCancel.setOnClickListener { dialog.dismiss() }
            val container = dialog.delegate.findViewById(R.id.design_bottom_sheet) as? FrameLayout
            container?.setBackgroundColor(context.resources.getColor(android.R.color.transparent))
            return dialog
        }
    }
}

interface OnSelectListener {
    fun onTakePhoto()
    fun onSelectFromGallery()
}