package com.fzm.chat.widget

import android.content.Context
import android.graphics.Point
import android.view.View
import android.widget.FrameLayout
import com.fzm.chat.R
import com.fzm.chat.biz.base.FunctionModules
import com.fzm.chat.biz.base.enableOA
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.databinding.PopupwindowHomeAddBinding
import com.qmuiteam.qmui.widget.popup.QMUIPopup
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.layoutInflater
import com.zjy.architecture.ext.setVisible

/**
 * @author zhengjy
 * @since 2020/12/09
 * Description:
 */
class HomeActionPopup(
    context: Context?
) : QMUIPopup(context, DIRECTION_BOTTOM) {

    companion object {
        fun create(context: Context, listener: View.OnClickListener): HomeActionPopup {
            val popup = HomeActionPopup(context)
            val binding = context.layoutInflater?.let { PopupwindowHomeAddBinding.inflate(it) }
            binding?.scan?.setOnClickListener {
                listener.onClick(it)
                popup.dismiss()
            }
            binding?.createGroup?.setOnClickListener {
                listener.onClick(it)
                popup.dismiss()
            }
            binding?.join?.setOnClickListener {
                listener.onClick(it)
                popup.dismiss()
            }
            binding?.qrCode?.setOnClickListener {
                listener.onClick(it)
                popup.dismiss()
            }
            if (FunctionModules.enableOA) {
                val delegate by rootScope.inject<LoginDelegate>()
                binding?.createCompany?.setVisible(!delegate.hasCompany())
                binding?.createCompany?.setOnClickListener {
                    listener.onClick(it)
                    popup.dismiss()
                }
                binding?.okr?.setVisible(delegate.hasCompany())
                binding?.okr?.setOnClickListener {
                    listener.onClick(it)
                    popup.dismiss()
                }
            } else {
                binding?.createCompany?.gone()
                binding?.okr?.gone()
            }

            binding?.root?.layoutParams = FrameLayout.LayoutParams(
                /*FrameLayout.LayoutParams.WRAP_CONTENT*/160.dp,
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