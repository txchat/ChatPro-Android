package com.fzm.chat.widget

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.view.isVisible
import com.fzm.chat.R
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.DialogAppUpdateBinding
import com.fzm.update.interfaces.IUpdateDialog
import com.fzm.update.interfaces.IUpdateInfo
import com.zjy.architecture.ext.*

/**
 * @author zhengjy
 * @since 2020/08/11
 * Description:
 */
class UpdateDialog(
    context: Context
) : Dialog(context), IUpdateDialog {

    private val container = DialogAppUpdateBinding.inflate(LayoutInflater.from(context))

    init {
        setContentView(container.root)
        window?.attributes = window?.attributes?.apply {
            gravity = Gravity.CENTER
            width = context.screenSize.x - 28.dp * 2
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun setupDialog(updateInfo: IUpdateInfo) {
        container.tvApkVersion.text = context.getString(R.string.chat_update_tips_version, updateInfo.getVersionName())
        container.tvApkSize.text = Utils.byteToSize(updateInfo.getApkSize())
        val desc = updateInfo.getDescription()
        val sb = StringBuilder()
        desc?.forEachIndexed { index, s ->
            sb.append("${index + 1}.$s")
            if (index != desc.size - 1) {
                sb.append("\n")
            }
        }
        container.updateDesc.text = sb.toString()
    }

    override fun getUpdateButton(): View? {
        return container.updateBtnUpdate
    }

    override fun getRefuseButton(): View? {
        return container.updateBtnCancel
    }

    override fun getInstallButton(): View? {
        return container.tvInstall
    }

    @SuppressLint("SetTextI18n")
    override fun onProgress(progress: Int) {
        if (container.updateAction.isVisible) {
            container.updateAction.gone()
        }
        if (container.tvInstall.isVisible) {
            container.tvInstall.gone()
        }
        if (!container.llUpdateProgress.isVisible) {
            container.llUpdateProgress.visible()
            container.tvUpdateTip.setText(R.string.chat_update_downloading)
        }
        container.tvUpdatePercent.text = "${progress}%"
        container.updateProgress.progress = progress
    }

    override fun onFinish(success: Boolean) {
        container.tvInstall.visible()
        container.updateAction.gone()
        container.llUpdateProgress.gone()
        if (success) {
            container.tvInstall.setText(R.string.chat_update_action_install)
        } else {
            container.tvInstall.setText(R.string.chat_update_action_retry)
        }
    }
}