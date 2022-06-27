package com.fzm.chat.login

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.logic.ServerManager
import com.fzm.chat.databinding.ActivityChooseLoginBinding
import com.fzm.chat.router.biz.BizModule
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.ChooseServerViewModel
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.ActivityUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2020/12/09
 * Description:
 */
@Route(path = MainModule.CHOOSE_LOGIN)
class ChooseLoginActivity : BizActivity(), View.OnClickListener {

    private val viewModel by viewModel<ChooseServerViewModel>()

    private val binding by init { ActivityChooseLoginBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        setAgreementText()
        viewModel.serverFetchFail.observe(this) { toast(it) }
    }

    override fun initData() {
        viewModel.fetchServerList(false)
    }

    override fun setEvent() {
        binding.chooseServer.setOnClickListener(this)
        binding.loginWords.setOnClickListener(this)
        binding.loginAccount.setOnClickListener(this)
        binding.tvCreate.setOnClickListener(this)
        binding.tvImport.setOnClickListener(this)
        binding.llLogin.setOnClickListener(this)
    }

    private fun setAgreementText() {
        val text = getString(R.string.chat_login_tips_agreement)
        val ssb = SpannableStringBuilder(text)
        val protocol: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                ARouter.getInstance().build(BizModule.WEB_ACTIVITY)
                    .withString("url", AppConfig.APP_LICENSE)
                    .withBoolean("showTitle", true)
                    .navigation()
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = ds.linkColor
                ds.isUnderlineText = false
            }
        }
        val start = text.indexOfFirst { it == '《' }
        val end = text.indexOfFirst { it == '》' } + 1
        val colorSpan = ForegroundColorSpan(resources.getColor(R.color.biz_color_accent))
        ssb.setSpan(protocol, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        ssb.setSpan(colorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvProtocol.movementMethod = LinkMovementMethod.getInstance()
        binding.tvProtocol.text = ssb
    }

    private fun checkPrepared(action: () -> Unit) {
        if (!binding.cbAgree.isChecked) {
            toast(R.string.chat_login_tips_agree_protocol)
            return
        }
        if (!ServerManager.isOnline()) {
            toast(R.string.chat_login_tips_choose_server)
            return
        }
        action.invoke()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.choose_server -> {
                ARouter.getInstance().build(MainModule.CHOOSE_SERVER).navigation()
            }
            R.id.tv_create -> {
                checkPrepared {
                    ARouter.getInstance().build(MainModule.CREATE_MNEM).navigation()
                }
            }
            R.id.tv_import -> {
                checkPrepared {
                    ARouter.getInstance().build(MainModule.IMPORT_ACCOUNT).navigation()
                }
            }
            R.id.ll_login -> {
                checkPrepared {
                    ARouter.getInstance().build(MainModule.LOGIN).navigation()
                }
            }
        }
    }

    private var lastExitTime = 0L

    override fun onBackPressedSupport() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastExitTime > 1500L) {
            toast(R.string.chat_click_again_to_exit)
            lastExitTime = currentTime
        } else {
            ActivityUtils.exitApp()
        }
    }
}