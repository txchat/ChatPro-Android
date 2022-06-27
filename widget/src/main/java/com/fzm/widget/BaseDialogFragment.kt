package com.fzm.widget

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment

/**
 * @author zhengjy
 * @since 2020/07/29
 * Description:
 */
abstract class BaseDialogFragment : DialogFragment() {

    @get:LayoutRes
    abstract val layoutId: Int

    lateinit var container: View

    /**
     * 设置Dialog内的View，以及监听事件
     */
    abstract fun setupDialog(dialog: Dialog)

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.window?.setBackgroundDrawableResource(R.color.widget_transparent)
        val lp = dialog.window?.attributes
        lp?.gravity = Gravity.CENTER
        lp?.width = WindowManager.LayoutParams.MATCH_PARENT
        lp?.height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.attributes = lp

        container = requireActivity().layoutInflater.inflate(layoutId, null)
        dialog.window?.setContentView(container)
        setupDialog(dialog)

        return dialog
    }
}