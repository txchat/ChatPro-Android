package com.fzm.chat.security

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.databinding.FragmentUpdateEncryptPasswordBinding
import com.fzm.chat.vm.EncryptPasswordViewModel
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2019/10/24
 * Description:
 */
class UpdateEncryptPasswordFragment : BizFragment() {

    private val viewModel by viewModel<EncryptPasswordViewModel>()

    private val binding by init<FragmentUpdateEncryptPasswordBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.oldError.observe(this) {
            if (it == 0) {
                binding.tvPasswordError.text = ""
            } else {
                binding.tvPasswordError.text = getString(it)
            }
        }
        viewModel.newError.observe(this) {
            if (it == 0) {
                binding.tvNewPasswordError.text = ""
            } else {
                binding.tvNewPasswordError.text = getString(it)
            }
        }
        viewModel.newSecondError.observe(this) {
            if (it == 0) {
                binding.tvNewPasswordErrorAgain.text = ""
            } else {
                binding.tvNewPasswordErrorAgain.text = getString(it)
            }
        }
        viewModel.changeResult.observe(this) {
            dismiss()
            toast(R.string.chat_tips_update_encrypt_pwd1)
            activity?.setResult(Activity.RESULT_OK)
            activity?.finish()
        }
    }

    override fun initData() {
        binding.etOldPwd.post { KeyboardUtils.showKeyboard(binding.etOldPwd) }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener {
            activity?.setResult(Activity.RESULT_CANCELED)
            activity?.finish()
        }
        binding.tvSubmit.setOnClickListener {
            val old = binding.etOldPwd.text.toString().trim()
            val new = binding.etNewPwd.text.toString().trim()
            val newAgain = binding.etNewPwdAgain.text.toString().trim()
            viewModel.changePassword(old, new, newAgain)
        }
        binding.etOldPwd.setOnFocusChangeListener { _, hasFocus ->
            val old = binding.etOldPwd.text.toString().trim()
            if (!hasFocus && old.isNotEmpty()) {
                viewModel.checkOldPassword(old)
            }
        }
        binding.etNewPwd.setOnFocusChangeListener { _, hasFocus ->
            val new = binding.etNewPwd.text.toString().trim()
            if (!hasFocus && new.isNotEmpty()) {
                viewModel.checkNew(new)
            }
            viewModel.resetSecondError()
        }
        binding.etNewPwd.addTextChangedListener {
            viewModel.resetFirstError()
        }
        binding.etNewPwdAgain.addTextChangedListener {
            viewModel.resetSecondError()
        }
    }
}