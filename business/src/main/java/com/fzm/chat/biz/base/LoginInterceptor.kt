package com.fzm.chat.biz.base

import android.content.Context
import com.alibaba.android.arouter.facade.Postcard
import com.alibaba.android.arouter.facade.annotation.Interceptor
import com.alibaba.android.arouter.facade.callback.InterceptorCallback
import com.alibaba.android.arouter.facade.template.IInterceptor
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.util.ActivityUtils

/**
 * @author zhengjy
 * @since 2018/11/01
 * Description:
 */
@Interceptor(priority = 10)
class LoginInterceptor : IInterceptor {

    override fun process(postcard: Postcard, callback: InterceptorCallback) {
        if (postcard.path == AppModule.MAIN) {
            // 进入登录页面是关闭选择登录页面
            if (ActivityUtils.isActivityExist("com.fzm.chat.login.ChooseLoginActivity")) {
                ActivityUtils.finish("com.fzm.chat.login.ChooseLoginActivity")
            }
        }
        if (postcard.extra == AppConst.NEED_LOGIN) {
            val address: String = AppPreference.ADDRESS
            if (address.isNotEmpty()) {
                callback.onContinue(postcard)
            } else {
                ARouter.getInstance().build(MainModule.CHOOSE_LOGIN).navigation()
                callback.onInterrupt(null)
            }
        } else {
            callback.onContinue(postcard)
        }
    }

    override fun init(context: Context) {

    }
}