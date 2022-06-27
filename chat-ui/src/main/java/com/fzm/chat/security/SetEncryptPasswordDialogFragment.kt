package com.fzm.chat.security

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.ui.PasswordVerifyFragment
import com.fzm.chat.databinding.DialogSetEncryptPasswordBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.utils.StringUtils
import com.fzm.chat.vm.SecuritySettingViewModel
import com.fzm.chat.widget.ExportWordsDialog
import com.zjy.architecture.data.EventObserver
import com.zjy.architecture.ext.setupLoading
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * @author zhengjy
 * @since 2020/12/17
 * Description:
 */
@Route(path = MainModule.SET_ENC_PWD)
class SetEncryptPasswordDialogFragment : PasswordVerifyFragment() {

    /**
     * 0：不显示导出弹窗
     * 1：导出助记词
     * 2：导出私钥
     */
    @JvmField
    @Autowired
    var showWords: Int = 0

    private var password: String = ""

    private lateinit var binding: DialogSetEncryptPasswordBinding
    private val viewModel by lazy { requireActivity().getViewModel<SecuritySettingViewModel>() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        ARouter.getInstance().inject(this)
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.attributes = window?.attributes?.apply {
                gravity = Gravity.CENTER
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
            binding = DialogSetEncryptPasswordBinding.inflate(layoutInflater)
            window?.setContentView(binding.root)
            binding.confirm.setOnClickListener {
                password = binding.etPassword.text?.toString()?.trim() ?: ""
                if (password.length < 8 || password.length > 16) {
                    context.toast(R.string.chat_tips_update_encrypt_pwd3)
                    return@setOnClickListener
                } else if (!StringUtils.isEncryptPassword(password)) {
                    context.toast(R.string.chat_tips_update_encrypt_pwd4)
                    return@setOnClickListener
                }
                viewModel.changeDefaultPassword(password)
            }
            binding.ivClose.setOnClickListener {
                this@SetEncryptPasswordDialogFragment.dismiss()
            }
        }
        viewModel.setEncPwd.observe(this, EventObserver { words ->
            if (words.isNotEmpty()) {
                if (showWords != 0) {
                    ExportWordsDialog(requireContext(), words, showWords).show()
                } else {
                    toast(R.string.chat_tips_update_encrypt_pwd7)
                    listener?.onSuccess(password)
                }
                viewModel.refreshState()
                dismiss()
            } else {
                toast(R.string.chat_tips_chat_pwd_error)
                listener?.onFail()
            }
        })
        viewModel.loading.observe(this) { setupLoading(it) }
        dialog.setOnCancelListener { listener?.onCancel() }
        dialog.setOnShowListener {
            binding.etPassword.post { KeyboardUtils.showKeyboard(binding.etPassword) }
        }
        return dialog
    }
}