package com.fzm.chat.wallet.ui

import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.wallet.R
import com.fzm.chat.wallet.databinding.ActivityWalletIndexBinding
import com.zjy.architecture.ext.notchSupport
import com.zjy.architecture.util.other.BarUtils

/**
 * @author zhengjy
 * @since 2021/08/04
 * Description:
 */
@Route(path = WalletModule.WALLET_INDEX)
class WalletIndexActivity : BizActivity() {

    private val binding by init { ActivityWalletIndexBinding.inflate(layoutInflater) }

    override fun setSystemBar() {
        notchSupport(window)
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, android.R.color.transparent), 0)
        BarUtils.setStatusBarLightMode(this, true)
        window?.navigationBarColor = ContextCompat.getColor(this, R.color.biz_color_primary_dark)
    }

    override val root: View
        get() = binding.root

    override fun initView() {
        supportFragmentManager.commit {
            add(R.id.fcv_container, WalletIndexFragment.create(true, true))
        }
    }

    override fun initData() {

    }

    override fun setEvent() {

    }
}