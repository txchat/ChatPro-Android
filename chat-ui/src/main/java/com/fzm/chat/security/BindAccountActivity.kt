package com.fzm.chat.security

import android.content.Intent
import android.graphics.Typeface
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Patterns
import android.view.View
import androidx.core.widget.addTextChangedListener
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.backup.LocalAccountType
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.enum.CodeType
import com.fzm.chat.databinding.ActivityBindAccountBinding
import com.fzm.chat.login.LoginActivity
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.BackupViewModel
import com.fzm.widget.dialog.EasyDialog
import com.fzm.widget.verify.CodeLength
import com.fzm.widget.verify.VerifyCodeDialog
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/01/14
 * Description:
 */
@Route(path = MainModule.BIND_ACCOUNT)
class BindAccountActivity : BizActivity() {

    /**
     * 绑定账户类型
     */
    @JvmField
    @Autowired
    var bindType = ChatConst.PHONE

    private var verifyDialog: VerifyCodeDialog? = null
    private var account = ""

    private val viewModel by viewModel<BackupViewModel>()

    /**
     * 是否覆盖绑定
     */
    private var overrideBind = false

    private val binding by init<ActivityBindAccountBinding>()

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        if (bindType == ChatConst.PHONE) {
            binding.ctbTitle.setTitle(getString(R.string.chat_title_bind_phone))
            binding.tvTips.setText(R.string.chat_tips_bind_phone_tips)
            binding.etAccount.setHint(R.string.chat_hint_input_phone)
            binding.etAccount.inputType = InputType.TYPE_CLASS_NUMBER
        } else {
            binding.ctbTitle.setTitle(getString(R.string.chat_title_bind_email))
            binding.tvTips.setText(R.string.chat_tips_bind_email_tips)
            binding.etAccount.setHint(R.string.chat_hint_input_email)
            binding.etAccount.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.queryResult.observe(this) {
            if (it) {
                val warning = getString(R.string.chat_tips_override_bind2)
                val ssb = SpannableStringBuilder(getString(R.string.chat_tips_override_bind))
                ssb.append("\n")
                ssb.append(warning)
                ssb.setSpan(
                    ForegroundColorSpan(resources.getColor(R.color.biz_red_tips)),
                    ssb.length - warning.length,
                    ssb.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                ssb.setSpan(
                    StyleSpan(Typeface.BOLD),
                    ssb.length - warning.length,
                    ssb.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                EasyDialog.Builder()
                    .setHeaderTitle(getString(R.string.biz_tips))
                    .setBottomLeftText(getString(R.string.biz_cancel))
                    .setBottomRightText(getString(R.string.chat_action_unbind_and_rebind))
                    .setContent(ssb)
                    .setBottomLeftClickListener(null)
                    .setBottomRightClickListener { dialog ->
                        dialog.dismiss()
                        overrideBind = true
                        showSendCodeDialog()
                    }.create(this).show()
            } else {
                overrideBind = false
                showSendCodeDialog()
            }
        }
        viewModel.bindResult.observe(this) {
            verifyDialog?.dismiss()
            toast(R.string.chat_tips_bind_account_success)
            if (overrideBind) {
                val type = if (bindType == ChatConst.PHONE) {
                    LocalAccountType.BACKUP_PHONE
                } else {
                    LocalAccountType.BACKUP_EMAIL
                }
                viewModel.findLocalAccount(it, type).observe(this) { address ->
                    dismiss()
                    toMainPage(bindType, account, address)
                }
            } else {
                dismiss()
                toMainPage(bindType, account, null)
            }
        }
    }

    private fun toMainPage(bindType: Int, account: String, address: String?) {
        if (bindType == ChatConst.PHONE) {
            viewModel.updateInfo { phone = account }
            ARouter.getInstance().build(AppModule.MAIN)
                .apply {
                    if (overrideBind) {
                        withString("importAddress", address)
                    }
                }
                .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .navigation(this)
        } else {
            viewModel.updateInfo { email = account }
            ARouter.getInstance().build(AppModule.MAIN)
                .apply {
                    if (overrideBind) {
                        withString("importAddress", address)
                    }
                }
                .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .navigation(this)
        }
    }

    private fun showSendCodeDialog() {
        verifyDialog = VerifyCodeDialog.Builder(this, this, CodeLength.MEDIUM)
            .setPhone(account)
            .setAutoSend(true)
            .setOnSendCodeListenerSuspend {
                viewModel.sendCodeSuspend(account, bindType, CodeType.BIND)
            }
            .setOnCodeCompleteListener { _, _, code ->
                viewModel.bindAccount(account, code, bindType)
            }
            .build()
        verifyDialog?.show()
    }

    override fun initData() {
        binding.etAccount.post {
            KeyboardUtils.showKeyboard(binding.etAccount)
        }
        binding.etAccount.addTextChangedListener {
            it?.toString()?.trim()?.also { text ->
                account = text
            }
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.tvSend.setOnClickListener {
            if (checkAccount()) {
                viewModel.checkBind(account, bindType)
            }
        }
    }

    private fun checkAccount(): Boolean {
        if (account.isEmpty()) {
            if (bindType == LoginActivity.PHONE_LOGIN) {
                toast(R.string.login_tips_input_phone)
            } else {
                toast(R.string.login_tips_input_email)
            }
            return false
        }
        if (bindType == LoginActivity.PHONE_LOGIN) {
            if (account.length != 11) {
                toast(R.string.login_tips_correct_phone)
                return false
            }
        } else {
            if (!account.matches(Patterns.EMAIL_ADDRESS.toRegex())) {
                toast(R.string.login_tips_correct_email)
                return false
            }
        }
        return true
    }
}