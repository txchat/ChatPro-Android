package com.fzm.chat.wallet.ui

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.widget.addTextChangedListener
import com.fzm.chat.wallet.databinding.DialogLocalNoteBinding
import com.zjy.architecture.util.KeyboardUtils

/**
 * @author zhengjy
 * @since 2021/08/16
 * Description:
 */
class LocalNoteDialog(
    context: Context,
    oldNote: CharSequence?,
    onSaveNote: (String) -> Unit
) : Dialog(context) {

    private val binding: DialogLocalNoteBinding
    private var localNote: String = ""

    init {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.attributes = window?.attributes?.apply {
            gravity = Gravity.CENTER
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        binding = DialogLocalNoteBinding.inflate(LayoutInflater.from(context))
        window?.setContentView(binding.root)

        binding.etNote.addTextChangedListener {
            val text = it?.toString() ?: ""
            binding.tvCount.text = "${text.length}/60"
            localNote = text
        }
        binding.etNote.setText(oldNote)
        if (!oldNote.isNullOrEmpty()) {
            binding.etNote.setSelection(0, oldNote.length)
        }
        binding.etNote.postDelayed({ KeyboardUtils.showKeyboard(binding.etNote) }, 100)
        binding.tvCancel.setOnClickListener { cancel() }
        binding.tvConfirm.setOnClickListener {
            onSaveNote(localNote)
            dismiss()
        }
    }
}