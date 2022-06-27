package com.fzm.chat.app

import android.view.View
import android.view.ViewGroup
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.main.MainService
import com.fzm.chat.router.route
import com.zjy.architecture.base.BaseActivity
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.notchSupport
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.ActivityUtils

/**
 * @author zhengjy
 * @since 2020/08/31
 * Description:
 */
class SplashActivity : BaseActivity() {

    private val mainService by route<MainService>(MainModule.SERVICE)

    override val layoutId: Int = R.layout.activity_splash

    override fun initView() {

    }

    override fun setSystemBar() {
        notchSupport(window)
        val logo = findViewById<View>(R.id.iv_logo)
        // val statusHeight = BarUtils.getStatusBarHeight(this)
        logo.layoutParams = (logo.layoutParams as ViewGroup.MarginLayoutParams).apply {
            topMargin -= 90
        }
    }

    override fun initData() {
        val start = System.currentTimeMillis()
        mainService?.checkUpdate(this) { success, msg ->
            if (!success) toast(msg)
            val cost = System.currentTimeMillis() - start
            window?.decorView?.postDelayed({
                if (!ActivityUtils.isActivityExist("com.fzm.chat.app.MainActivity")) {
                    ARouter.getInstance().build(AppModule.MAIN)
                        .withTransition(R.anim.biz_fade_in, R.anim.biz_fade_out)
                        .navigation(instance)
                }
                window?.decorView?.postDelayed({
                    finish()
                }, (1000 - cost).coerceAtLeast(200))
            }, (1000 - cost).coerceAtLeast(200))
        }
    }

    override fun setEvent() {

    }
}