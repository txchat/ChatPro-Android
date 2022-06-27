package com.fzm.chat.login.words

import android.graphics.Color
import android.view.View
import androidx.fragment.app.commit
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.databinding.ActivityImportAccountBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.util.other.BarUtils

/**
 * @author zhengjy
 * @since 2020/12/11
 * Description:
 */
@Route(path = MainModule.IMPORT_ACCOUNT)
class ImportAccountActivity : BizActivity() {

    private val binding by init { ActivityImportAccountBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        BarUtils.setStatusBarColor(this, Color.WHITE, 0)
        BarUtils.setStatusBarLightMode(this, true)
        window?.navigationBarColor = Color.WHITE
    }

    override fun initView() {
        supportFragmentManager.commit { add(R.id.fl_container, ImportMnemFragment()) }
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
    }
}