package com.fzm.chat.login

import android.util.Patterns
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.ui.PasswordVerifyFragment
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.enum.CodeType
import com.fzm.chat.core.logic.ServerManager
import com.fzm.chat.databinding.ActivityLoginBinding
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.biz.PasswordVerifier
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.route
import com.fzm.chat.vm.BackupViewModel
import com.fzm.widget.dialog.EasyDialog
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.toast
import com.zjy.architecture.ext.visible
import com.zjy.architecture.util.KeyboardUtils
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/01/12
 * Description:
 */
@Route(path = MainModule.LOGIN)
class LoginActivity : BizActivity() {

    companion object {
        const val PHONE_LOGIN = ChatConst.PHONE
        const val EMAIL_LOGIN = ChatConst.EMAIL
    }

    private var mAccount: String = ""
        get() = field.replace(" ".toRegex(), "")

    /**
     * 登录类型
     * 0：手机
     * 1：邮箱
     */
    private var loginType = PHONE_LOGIN

    private val viewModel by viewModel<BackupViewModel>()

    private val binding by init { ActivityLoginBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.code.observe(this) {
            if (it > 0) {
                binding.tvGetCode.text = getString(R.string.login_tips_code_count, it)
                binding.tvGetCode.isClickable = false
            } else {
                binding.tvGetCode.setTextColor(resources.getColor(R.color.biz_color_accent))
                binding.tvGetCode.setText(R.string.login_action_send_code)
                binding.tvGetCode.isClickable = true
            }
        }
        viewModel.sendResult.observe(this) {
            if (it != null) {
                binding.tvGetCode.setTextColor(resources.getColor(R.color.biz_text_grey_light))
                viewModel.startCount()
                toast(R.string.login_tips_code_sent)
            }
        }
        viewModel.fetchResult.observe(this) {
            if (it.mnemonic.isNullOrEmpty()) {
                // 正常情况下不会进入这里，因为fetch之前已经确认过存在备份的助记词
                val dialog = EasyDialog.Builder()
                    .setHeaderTitle(getString(R.string.biz_tips))
                    .setBottomLeftText(getString(R.string.chat_login_create_account))
                    .setBottomRightText(getString(R.string.chat_login_import_account))
                    .setCancelable(false)
                    .setContent(getString(R.string.login_tips_unbind_yet))
                    .setBottomLeftClickListener { dialog ->
                        dialog.dismiss()
                        ARouter.getInstance().build(MainModule.CREATE_MNEM).navigation()
                        finish()
                    }
                    .setBottomRightClickListener { dialog ->
                        dialog.dismiss()
                        ARouter.getInstance().build(MainModule.IMPORT_ACCOUNT).navigation()
                        finish()
                    }.create(this)
                dialog.show()
            } else {
                lifecycleScope.launch {
                    if (viewModel.autoImportMnemonic(it.mnemonic ?: "", "hz123456")) {
                        // 先尝试使用固定密码解密助记词
                        ARouter.getInstance().build(AppModule.MAIN).navigation()
                        dismiss()
                        finish()
                    } else {
                        val dialog by route<DialogFragment>(
                            MainModule.DECRYPT_MNEMONIC,
                            bundleOf(
                                "encMnemonic" to it.mnemonic,
                                "account" to mAccount,
                                "loginType" to loginType
                            )
                        )
                        // 用户手动输入密码解密助记词
                        dialog?.show(supportFragmentManager, "DECRYPT_MNEMONIC")
                    }
                }
            }
        }
        viewModel.needCreate.observe(this) { params ->
            // 首次登录自动创建助记词，弹窗设置密聊密码
            val dialog by route<PasswordVerifyFragment>(
                MainModule.SET_ENC_PWD,
                bundleOf("showWords" to 0)
            )
            dialog?.setOnPasswordVerifyListener(object : PasswordVerifier.OnPasswordVerifyListener {
                override fun onSuccess(password: String) {
                    // 将手机号与加密助记词绑定
                    viewModel.bindAutoCreateAccount(params.first, params.second, params.third)
                }
            })
            dialog?.show(supportFragmentManager, "SET_ENC_PWD")
        }
        viewModel.bindResult.observe(this) {
            ARouter.getInstance().build(AppModule.MAIN).navigation()
            dismiss()
            finish()
        }
        viewModel.error.observe(this) { toast(it) }
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.ctbTitle.setOnRightClickListener {
            viewModel.cancelCount()
            if (loginType == PHONE_LOGIN) {
                loginType = EMAIL_LOGIN
                binding.ctbTitle.setRightText(getString(R.string.login_use_phone))
                binding.tvTitle.setText(R.string.login_use_email)
                binding.tvSubTitle.setText(R.string.login_use_email_tips)
                binding.etPhone.gone()
                binding.llArea.gone()
                binding.etEmail.visible()
                binding.etPhone.setText("")
                binding.etEmail.setText("")
                binding.etCode.setText("")
                binding.etEmail.post {
                    binding.etEmail.requestFocus()
                }
            } else {
                loginType = PHONE_LOGIN
                binding.ctbTitle.setRightText(getString(R.string.login_use_email))
                binding.tvTitle.setText(R.string.login_use_phone)
                binding.tvSubTitle.setText(R.string.login_use_phone_tips)
                binding.etPhone.visible()
                binding.llArea.visible()
                binding.etEmail.gone()
                binding.etPhone.setText("")
                binding.etEmail.setText("")
                binding.etCode.setText("")
                binding.etPhone.post {
                    binding.etPhone.requestFocus()
                }
            }
        }
        binding.etPhone.doOnTextChanged { text, start, count, after ->
            if (after == 1) {
                val length = text.toString().length
                if (length == 3 || length == 8) {
                    binding.etPhone.setText("$text ")
                    binding.etPhone.setSelection(binding.etPhone.text.toString().length)
                }
            }
            if (start == 3 || start == 8) {
                if (count == 1 && after == 0) {
                    binding.etPhone.setText(text?.subSequence(0, text.length - 1))
                    binding.etPhone.setSelection(binding.etPhone.text.toString().length)
                }
            }
            mAccount = text.toString()
        }
        binding.etEmail.addTextChangedListener {
            mAccount = it?.toString()?.trim() ?: ""
        }
        binding.tvGetCode.setOnClickListener {
            if (checkAccount()) {
                viewModel.sendCode(mAccount, loginType, CodeType.QUICK)
            }
        }
        binding.btnLogin.setOnClickListener {
            if (checkAccount() && checkCode() && checkServer()) {
                KeyboardUtils.hideKeyboard(binding.btnLogin)
                val code = binding.etCode.text.toString().trim()
                viewModel.verifyCompanyUser(mAccount, code, loginType)
            }
        }
    }

    private fun checkAccount(): Boolean {
        if (mAccount.isEmpty()) {
            if (loginType == PHONE_LOGIN) {
                toast(R.string.login_tips_input_phone)
            } else {
                toast(R.string.login_tips_input_email)
            }
            return false
        }
        if (loginType == PHONE_LOGIN) {
            if (mAccount.length != 11) {
                toast(R.string.login_tips_correct_phone)
                return false
            }
        } else {
            if (!mAccount.matches(Patterns.EMAIL_ADDRESS.toRegex())) {
                toast(R.string.login_tips_correct_email)
                return false
            }
        }
        return true
    }

    private fun checkCode(): Boolean {
        if (binding.etCode.text.toString().trim().isEmpty()) {
            toast(R.string.login_tips_input_code)
            return false
        }
        if (binding.etCode.text.toString().trim().length < 5) {
            toast(R.string.login_tips_correct_code)
            return false
        }
        return true
    }

    private fun checkServer(): Boolean {
        if (!ServerManager.isOnline()) {
            toast(R.string.chat_login_tips_choose_server)
            return false
        }
        return true
    }
}