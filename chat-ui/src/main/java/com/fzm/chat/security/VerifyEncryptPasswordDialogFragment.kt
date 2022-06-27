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
import com.fzm.chat.databinding.DialogVerifyEncryptPasswordBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.utils.StringUtils
import com.fzm.chat.vm.SecuritySettingViewModel
import com.fzm.chat.widget.ExportWordsDialog
import com.zjy.architecture.data.EventObserver
import com.zjy.architecture.ext.setupLoading
import com.zjy.architecture.ext.toast
import com.zjy.architecture.mvvm.Loading
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * @author zhengjy
 * @since 2020/12/17
 * Description:
 */
@Route(path = MainModule.VERIFY_ENC_PWD)
class VerifyEncryptPasswordDialogFragment : PasswordVerifyFragment() {

    @JvmField
    @Autowired
    var type: Int = 0

    private var password: String = ""

    private lateinit var binding: DialogVerifyEncryptPasswordBinding
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
            binding = DialogVerifyEncryptPasswordBinding.inflate(layoutInflater)
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
                when (type) {
                    0 -> viewModel.checkPassword(password)
                    1 -> viewModel.getMnemonic(password)
                    2 -> viewModel.getPrivateKey(password)
                }
            }
            binding.ivClose.setOnClickListener {
                this@VerifyEncryptPasswordDialogFragment.dismiss()
            }
        }
        viewModel.mnemonic.observe(this, EventObserver {
            dismissLoading()
            if (it.isEmpty()) {
                toast(R.string.chat_tips_mnem_export_fail)
            } else {
                ExportWordsDialog(requireContext(), it, 1).show()
                dismiss()
            }
        })
        viewModel.privateKey.observe(this, EventObserver {
            dismissLoading()
            if (it.isEmpty()) {
                toast(R.string.chat_tips_private_export_fail)
            } else {
                ExportWordsDialog(requireContext(), it, 2).show()
                dismiss()
            }
        })
        viewModel.errorPwd.observe(this, EventObserver {
            dismissLoading()
            toast(R.string.chat_tips_chat_pwd_error)
            listener?.onFail()
        })
        viewModel.verified.observe(this, EventObserver {
            dismissLoading()
            dismiss()
            listener?.onSuccess(password)
        })
        viewModel.loading.observe(this) {
            if (it.loading) {
                setupLoading(it)
            }
        }
        dialog.setOnCancelListener { listener?.onCancel() }
        dialog.setOnShowListener {
            binding.etPassword.post { KeyboardUtils.showKeyboard(binding.etPassword) }
        }
        return dialog
    }

    /**
     * 提前关闭loading，防止后面误关其他viewModel开启的loading
     */
    private fun dismissLoading() {
        setupLoading(Loading(loading = false, cancelable = true))
    }
}