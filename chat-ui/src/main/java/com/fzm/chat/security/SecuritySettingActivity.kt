package com.fzm.chat.security

import android.content.Intent
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.databinding.ActivitySecuritySettingBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.route
import com.fzm.chat.vm.SecuritySettingViewModel
import com.zjy.architecture.ext.toast
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2020/12/16
 * Description:
 */
@Route(path = MainModule.SECURITY_SET)
class SecuritySettingActivity : BizActivity(), View.OnClickListener {

    companion object {
        const val BIND_PHONE = 100
        const val BIND_EMAIL = 200
    }

    @JvmField
    @Autowired
    var bindType = 0

    private val viewModel by viewModel<SecuritySettingViewModel>()
    private var phone = ""
    private var email = ""

    private val binding by init { ActivitySecuritySettingBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override val darkStatusColor: Boolean = true

    override fun initView() {
        ARouter.getInstance().inject(this)
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.refresh.observe(this) { refreshState() }
        viewModel.current.observe(this) {
            if (it.isLogin()) {
                phone = it.phone ?: ""
                email = it.email ?: ""
                refreshBindInfo()
            } else {
                phone = ""
                email = ""
            }
        }
        when (bindType) {
            ChatConst.PHONE -> root.post { binding.llPhone.performClick() }
            ChatConst.EMAIL -> root.post { binding.llEmail.performClick() }
        }
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.llMnem.setOnClickListener(this)
        binding.llPhone.setOnClickListener(this)
        binding.llEmail.setOnClickListener(this)
        binding.llEncryptedPassword.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        if (viewModel.preference.hasChatPassword()) {
            binding.tvEncryptedPasswordTips.setText(R.string.chat_tips_action1)
        } else {
            binding.tvEncryptedPasswordTips.setText(R.string.chat_tips_action2)
        }
    }

    private fun refreshBindInfo() {
        if (phone.isEmpty()) {
            binding.tvPhoneTips.setText(R.string.chat_tips_action3)
        } else {
            binding.tvPhoneTips.text = phone
        }
        if (email.isEmpty()) {
            binding.tvEmailTips.setText(R.string.chat_tips_action3)
        } else {
            binding.tvEmailTips.text = email
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ll_mnem -> {
                if (viewModel.preference.hasChatPassword()) {
                    val dialog by route<DialogFragment>(
                        MainModule.VERIFY_ENC_PWD,
                        bundleOf("type" to 1)
                    )
                    dialog?.show(supportFragmentManager, "VERIFY_ENC_PWD1")
                } else {
                    val dialog by route<DialogFragment>(
                        MainModule.SET_ENC_PWD,
                        bundleOf("showWords" to 1)
                    )
                    dialog?.show(supportFragmentManager, "SET_ENC_PWD1")
                }
            }
//            R.id.ll_private -> {
//                if (viewModel.preference.hasChatPassword()) {
//                    val dialog by route<DialogFragment>(
//                        MainModule.VERIFY_ENC_PWD,
//                        bundleOf("type" to 2)
//                    )
//                    dialog?.show(supportFragmentManager, "VERIFY_ENC_PWD2")
//                } else {
//                    val dialog by route<DialogFragment>(
//                        MainModule.SET_ENC_PWD,
//                        bundleOf("showWords" to 2)
//                    )
//                    dialog?.show(supportFragmentManager, "SET_ENC_PWD2")
//                }
//            }
            R.id.ll_phone -> {
                if (phone.isEmpty()) {
                    if (viewModel.preference.hasChatPassword()) {
                        bindPhone()
                    } else {
                        applicationContext?.toast(R.string.chat_set_chat_password)
                        ARouter.getInstance().build(MainModule.ENCRYPT_PWD)
                            .withBoolean("setMode", true)
                            .navigation(this, BIND_PHONE)
                    }
                }
            }
            R.id.ll_email -> {
                if (email.isEmpty()) {
                    if (viewModel.preference.hasChatPassword()) {
                        bindEmail()
                    } else {
                        applicationContext?.toast(R.string.chat_set_chat_password)
                        ARouter.getInstance().build(MainModule.ENCRYPT_PWD)
                            .withBoolean("setMode", true)
                            .navigation(this, BIND_EMAIL)
                    }
                }
            }
            R.id.ll_encrypted_password -> {
                ARouter.getInstance().build(MainModule.ENCRYPT_PWD)
                    .withBoolean("setMode", !viewModel.preference.hasChatPassword())
                    .navigation()
            }
        }
    }

    private fun bindPhone() {
        ARouter.getInstance().build(MainModule.BIND_ACCOUNT)
            .withInt("bindType", ChatConst.PHONE)
            .navigation()
    }

    private fun bindEmail() {
        ARouter.getInstance().build(MainModule.BIND_ACCOUNT)
            .withInt("bindType", ChatConst.EMAIL)
            .navigation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == BIND_PHONE) {
                bindPhone()
            } else if (requestCode == BIND_EMAIL) {
                bindEmail()
            }
        }
    }
}