package com.fzm.chat.login.words

import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.bean.mnem.PWallet
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.utils.CipherUtils
import com.fzm.chat.databinding.ActivityCreateMnemBinding
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.toast
import com.zjy.architecture.ext.visible
import com.zjy.architecture.util.other.BarUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2020/12/09
 * Description:
 */
@Route(path = MainModule.CREATE_MNEM)
class CreateMnemActivity : BizActivity(), View.OnClickListener {

    private var mEnglishMnem: String? = null
    private var mChineseMnem: String? = null

    private val wallet: PWallet = PWallet()

    private val viewModel by viewModel<ImportAccountViewModel>()

    private val binding by init { ActivityCreateMnemBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        BarUtils.setStatusBarColor(this, Color.parseColor("#333649"), 0)
        BarUtils.setStatusBarLightMode(this, false)
        window?.navigationBarColor = Color.parseColor("#2B292F")
    }

    override fun initView() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.importResult.observe(this) {
            if (it != null) {
                applicationContext?.toast(R.string.chat_login_mnem_import_success)
                ARouter.getInstance().build(AppModule.MAIN).navigation()
                binding.root.postDelayed({ finish() }, 500)
            } else {
                toast(R.string.chat_login_mnem_import_fail)
            }
        }
    }

    override fun initData() {
        mChineseMnem = CipherUtils.createMnemonicString(1, 160)
        mEnglishMnem = CipherUtils.createMnemonicString(0, 128)

        binding.tvMnem.text = configSpace(mChineseMnem)
        wallet.mnemType = PWallet.TYPE_CHINESE
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.ctbTitle.setOnRightClickListener {
            gotoBackUpMnemActivity()
        }
        binding.lvChinese.setOnClickListener(this)
        binding.lvEnglish.setOnClickListener(this)
        binding.btnChangeMnem.setOnClickListener(this)
        binding.btnOk.setOnClickListener(this)
    }

    private fun configSpace(mnem: String?): String? {
        if (mnem == null) return null
        val chineseWords = mnem.replace(" ", "")
        val chinese = StringBuilder()
        for (i in chineseWords.indices) {
            val value = chineseWords[i].toString()
            val j = i + 1
            chinese.append(
                if (j % 3 == 0) {
                    "$value  "
                } else {
                    value
                }
            )
        }
        return chinese.toString()
    }

    private fun gotoBackUpMnemActivity() {
        if (binding.viewChinese.isVisible) {
            wallet.mnem = mChineseMnem
        } else if (binding.viewEnglish.isVisible) {
            wallet.mnem = mEnglishMnem
        }
        ARouter.getInstance().build(MainModule.BACKUP_MNEM)
            .withSerializable("wallet", wallet)
            .navigation()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.lv_chinese -> {
                binding.tvChinese.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                binding.tvEnglish.setTextColor(Color.parseColor("#8E92A3"))
                binding.viewChinese.visible()
                binding.viewEnglish.gone()
                binding.tvMnem.text = configSpace(mChineseMnem)
                wallet.mnemType = PWallet.TYPE_CHINESE
            }
            R.id.lv_english -> {
                binding.tvChinese.setTextColor(Color.parseColor("#8E92A3"))
                binding.tvEnglish.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                binding.viewChinese.gone()
                binding.viewEnglish.visible()
                binding.tvMnem.text = mEnglishMnem
                wallet.mnemType = PWallet.TYPE_ENGLISH
            }
            R.id.btn_change_mnem -> {
                val mnem = if (binding.viewChinese.isVisible) {
                    mChineseMnem = CipherUtils.createMnemonicString(1, 160)
                    configSpace(mChineseMnem)
                } else {
                    mEnglishMnem = CipherUtils.createMnemonicString(0, 128)
                    mEnglishMnem
                }
                binding.tvMnem.text = mnem
            }
            R.id.btn_ok -> {
                viewModel.importMnemonic(binding.tvMnem.text.toString())
            }
        }
    }
}