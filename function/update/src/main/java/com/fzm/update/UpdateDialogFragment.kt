package com.fzm.update

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.zjy.architecture.util.InstallUtil
import com.fzm.update.interfaces.IUpdateDialog
import com.fzm.update.interfaces.IUpdateInfo
import com.fzm.update.interfaces.OnAppDownloadListener
import com.fzm.update.provider.UpdateProvider
import com.fzm.update.utils.alreadyDownloaded
import com.zjy.architecture.ext.setVisible
import java.io.File

/**
 * @author zhengjy
 * @since 2020/08/11
 * Description:
 */
abstract class UpdateDialogFragment : DialogFragment() {

    private var title = ""
    private var icon = 0
    private var apkFile: File? = null

    private lateinit var updateDialog: IUpdateDialog
    private lateinit var updateInfo: IUpdateInfo

    private var mDismissListener: DialogInterface.OnDismissListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = arguments?.getString("title", "") ?: ""
        icon = arguments?.getInt("icon") ?: 0
        updateInfo = arguments?.getSerializable("info") as IUpdateInfo
    }

    abstract fun createDialog(): IUpdateDialog

    fun setOnDismissListener(listener: DialogInterface.OnDismissListener) {
        this.mDismissListener = listener
    }

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        updateDialog = createDialog()
        updateDialog.setupDialog(updateInfo)
        updateDialog.getUpdateButton()?.setOnClickListener { confirmUpdate() }
        updateDialog.getRefuseButton()?.setVisible(!updateInfo.isForceUpdate())
        updateDialog.getRefuseButton()?.setOnClickListener { dismiss() }
        updateDialog.getInstallButton()?.setOnClickListener {
            if (apkFile != null) {
                try {
                    InstallUtil.install(requireContext(), apkFile!!, UpdateProvider.authority(requireContext()))
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            } else {
                startDownload()
            }
        }
        if (alreadyDownloaded(requireContext(), updateInfo)) {
            // 如果已经下载好了，则直接显示安装按钮
            updateDialog.onFinish(true)
        }
        val dialog = updateDialog as Dialog
        isCancelable = !updateInfo.isForceUpdate()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mDismissListener?.onDismiss(dialog)
    }

    private fun confirmUpdate() {
        startDownload()
    }

    private fun startDownload() {
        UpdateManager.with(updateInfo)
            .setTitle(title)
            .setIcon(icon)
            .start(requireActivity(), object : OnAppDownloadListener {
                override fun onStart() {

                }

                override fun onProgress(progress: Float) {
                    updateDialog.onProgress((progress * 100).toInt())
                }

                override fun onSuccess(force: Boolean, file: File) {
                    apkFile = file
                    updateDialog.onFinish(true)
                    try {
                        InstallUtil.install(
                            requireContext(),
                            file,
                            UpdateProvider.authority(requireContext())
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFail(force: Boolean, e: Throwable?) {
                    updateDialog.onFinish(false)
                    e?.printStackTrace()
                }
            })
    }
}