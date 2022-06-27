package com.fzm.chat.security

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.core.backup.LocalAccountType
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.enum.CodeType
import com.fzm.chat.databinding.FragmentSetEncryptPasswordBinding
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.vm.EncryptPasswordViewModel
import com.fzm.widget.verify.CodeLength
import com.fzm.widget.verify.VerifyCodeDialog
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2019/10/24
 * Description:
 */
class SetEncryptPasswordFragment : BizFragment() {

    companion object {
        fun create(bindPhone: String?): SetEncryptPasswordFragment {
            return SetEncryptPasswordFragment().apply {
                arguments = bundleOf("bindPhone" to bindPhone)
            }
        }
    }

    private val viewModel by viewModel<EncryptPasswordViewModel>()

    private val binding by init<FragmentSetEncryptPasswordBinding>()

    private var bindPhone: String? = null

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        bindPhone = arguments?.getString("bindPhone")
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.firstError.observe(this) {
            if (it == 0) {
                binding.tvFirstError.text = ""
            } else {
                binding.tvFirstError.text = getString(it)
            }
        }
        viewModel.secondError.observe(this) {
            if (it == 0) {
                binding.tvSecondError.text = ""
            } else {
                binding.tvSecondError.text = getString(it)
            }
        }
        viewModel.mnemonicResult.observe(this) {
            if (it == null) {
                toast("设置密聊密码失败")
                return@observe
            }
            if (!bindPhone.isNullOrEmpty()) {
                bindPhone?.also { phone ->
                    VerifyCodeDialog.Builder(requireContext(), this, CodeLength.MEDIUM)
                        .setPhone(phone)
                        .setAutoSend(true)
                        .setOnSendCodeListenerSuspend {
                            viewModel.sendCodeSuspend(phone, ChatConst.PHONE, CodeType.BIND)
                        }
                        .setOnCodeCompleteListener { _, _, code ->
                            viewModel.bindAccount(phone, code, ChatConst.PHONE)
                        }
                        .build().show()
                }
            } else {
                dismiss()
                toast(R.string.chat_tips_update_encrypt_pwd7)
                activity?.setResult(Activity.RESULT_OK)
                activity?.finish()
            }
        }
        viewModel.bindResult.observe(this) {
            dismiss()
            KeyboardUtils.hideKeyboard(binding.etFirstPwd)
            toast(R.string.chat_tips_bind_account_success)
            viewModel.findLocalAccount(it, LocalAccountType.BACKUP_PHONE).observe(this) { address ->
                dismiss()
                ARouter.getInstance().build(AppModule.MAIN)
                    .withString("importAddress", address)
                    .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .navigation(requireContext())
            }
        }
    }

    override fun initData() {
        binding.etFirstPwd.post {
            KeyboardUtils.showKeyboard(binding.etFirstPwd)
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener {
            activity?.setResult(Activity.RESULT_CANCELED)
            activity?.finish()
        }
        binding.tvSubmit.setOnClickListener {
            val first = binding.etFirstPwd.text.toString().trim()
            val second = binding.etSecondPwd.text.toString().trim()
            viewModel.changeDefaultPassword(first, second)
        }
        binding.etFirstPwd.setOnFocusChangeListener { _, hasFocus ->
            val password = binding.etFirstPwd.text.toString().trim()
            if (!hasFocus && password.isNotEmpty()) {
                viewModel.checkFirst(password)
            }
        }
        binding.etFirstPwd.addTextChangedListener {
            viewModel.resetFirstError()
        }
        binding.etSecondPwd.addTextChangedListener {
            viewModel.resetSecondError()
        }
    }
}