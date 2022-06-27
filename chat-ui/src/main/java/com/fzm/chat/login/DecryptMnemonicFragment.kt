package com.fzm.chat.login

import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.ui.DeepLinkHelper
import com.fzm.chat.biz.ui.PasswordVerifyFragment
import com.fzm.chat.core.backup.LocalAccountType
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.databinding.DialogDecryptMnemonicBinding
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.utils.StringUtils
import com.fzm.chat.vm.BackupViewModel
import com.fzm.widget.dialog.EasyDialog
import com.zjy.architecture.data.EventObserver
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.KeyboardUtils
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * @author zhengjy
 * @since 2021/01/13
 * Description:
 */
@Route(path = MainModule.DECRYPT_MNEMONIC)
class DecryptMnemonicFragment : PasswordVerifyFragment() {

    @JvmField
    @Autowired
    var encMnemonic: String? = null

    @JvmField
    @Autowired
    var account: String? = null

    @JvmField
    @Autowired
    var loginType: Int = 0

    private var password: String = ""

    private lateinit var binding: DialogDecryptMnemonicBinding
    private val viewModel by lazy { requireActivity().getViewModel<BackupViewModel>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ARouter.getInstance().inject(this)

        viewModel.checkError.observe(this, EventObserver {
            if (it == 0) {
                context?.applicationContext?.toast(R.string.chat_login_mnem_import_success)
                if (listener != null) {
                    listener?.onSuccess(password)
                } else {
                    ARouter.getInstance().build(AppModule.MAIN).navigation()
                    binding.root.postDelayed({ activity?.finish() }, 500)
                }
            } else {
                context?.applicationContext?.toast(it)
                listener?.onFail()
            }
        })

        viewModel.importResult.observe(this) {
            if (it != null) {
                ARouter.getInstance().build(AppModule.MAIN)
                    .withParcelable("route", Uri.parse("${DeepLinkHelper.APP_LINK}?type=setEncPwd&setMode=true&bindPhone=$it"))
                    .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .navigation(requireContext())
                binding.root.postDelayed({ activity?.finish() }, 500)
            } else {
                context?.applicationContext?.toast(R.string.chat_login_mnem_import_fail)
            }
        }

        viewModel.recoverResult.observe(this) {
            context?.applicationContext?.toast(R.string.chat_login_mnem_recover_success)
            if (listener != null) {
                listener?.onSuccess(password)
            } else {
                ARouter.getInstance().build(AppModule.MAIN).navigation()
                binding.root.postDelayed({ activity?.finish() }, 500)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.attributes = window?.attributes?.apply {
                gravity = Gravity.CENTER
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
            binding = DialogDecryptMnemonicBinding.inflate(layoutInflater)
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
                viewModel.decryptAndReImportMnemonic(encMnemonic ?: "", password)
            }
            binding.tvForget.setOnClickListener {
                lifecycleScope.launch { forgetPassword() }
            }
            binding.ivClose.setOnClickListener {
                this@DecryptMnemonicFragment.dismiss()
            }
            setOnCancelListener { listener?.onCancel() }
            setOnShowListener {
                binding.etPassword.post { KeyboardUtils.showKeyboard(binding.etPassword) }
            }
        }
        return dialog
    }

    /**
     * 忘记密码流程
     */
    private suspend fun forgetPassword() {
        KeyboardUtils.hideKeyboard(binding.etPassword)
        val type = if (loginType == ChatConst.PHONE) {
            LocalAccountType.BACKUP_PHONE
        } else {
            LocalAccountType.BACKUP_EMAIL
        }
        if (viewModel.tryRecoverMnemonic(account!!, type)) {
            // 首先尝试自动恢复助记词
            return
        }
        if (loginType == ChatConst.PHONE) {
            val ssb = SpannableStringBuilder(getString(R.string.chat_tips_phone_forget_password))
            ssb.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.biz_red_tips)),
                36,
                43,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            val warning = getString(R.string.chat_tips_override_forget_password2)
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
                .setBottomLeftText(getString(R.string.chat_action_unbind_and_rebind))
                .setBottomRightText(getString(R.string.chat_action_unbind_import_mnem))
                .setContent(ssb)
                .setBottomLeftClickListener {
                    it.dismiss()
                    viewModel.autoCreateAndImportMnemonic(account!!)
                }
                .setBottomRightClickListener {
                    it.dismiss()
                    ARouter.getInstance().build(MainModule.IMPORT_ACCOUNT).navigation(requireContext())
                    activity?.finish()
                }.create(context).show()
        } else {
            val ssb = SpannableStringBuilder(getString(R.string.chat_tips_email_forget_password))
            ssb.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.biz_red_tips)),
                35,
                42,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            EasyDialog.Builder()
                .setHeaderTitle(getString(R.string.biz_tips))
                .setBottomLeftText(getString(R.string.chat_action_unbind_phone_login))
                .setBottomRightText(getString(R.string.chat_action_unbind_import_mnem))
                .setContent(ssb)
                .setBottomLeftClickListener {
                    it.dismiss()
                    ARouter.getInstance().build(MainModule.LOGIN).navigation(requireContext())
                    activity?.finish()
                }
                .setBottomRightClickListener {
                    it.dismiss()
                    ARouter.getInstance().build(MainModule.IMPORT_ACCOUNT).navigation(requireContext())
                    activity?.finish()
                }
                .create(context).show()
        }
    }
}