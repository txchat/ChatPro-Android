package com.fzm.chat.biz.ui

import android.content.Intent
import android.net.Uri
import com.alibaba.android.arouter.facade.Postcard
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.biz.BizModule
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.rtc.RtcModule
import com.zjy.architecture.base.BaseActivity
import com.zjy.architecture.util.ActivityUtils
import java.util.HashMap

/**
 * @author zhengjy
 * @since 2020/08/28
 * Description:
 */
@Route(path = BizModule.DEEP_LINK)
class DeepLinkActivity : BaseActivity() {

    @Autowired
    @JvmField
    var route: Uri? = null

    override val layoutId: Int = 0

    override fun initView() {
        ARouter.getInstance().inject(this)
        // deep link可能来源于系统[intent.data]或者应用内浏览器[route]
        (intent?.data ?: route)?.also {
            if (!ActivityUtils.isActivityExist("com.fzm.chat.app.MainActivity")) {
                ARouter.getInstance().build(AppModule.MAIN).withParcelable("route", it).navigation()
            } else {
                val type = it.getQueryParameter("type")
                if (type != null) {
                    val path = DeepLinkHelper.routeMap[type]
                    val uri = it.buildUpon().appendEncodedPath(path).build()
                    ARouter.getInstance().build(uri).navigation()
                }
            }
        }
        window?.decorView?.postDelayed({
            finish()
        }, 500)
    }

    override fun onRestart() {
        super.onRestart()
        finish()
    }

    override fun initData() {

    }

    override fun setEvent() {

    }
}

/**
 * Deep Link配置处理类
 */
object DeepLinkHelper {

    val APP_SCHEME = AppConfig.APP_SCHEME
    val APP_HOST = AppConfig.APP_HOST
    val APP_LINK = "$APP_SCHEME://$APP_HOST"
    val routeMap: MutableMap<String, String> = HashMap()

    private val webUrlRegistry: MutableMap<String, Postcard> = HashMap()

    init {
        // substring是为了去掉第一位的'/'
        // 应用内浏览器
        routeMap["appWebBrowser"] = BizModule.WEB_ACTIVITY.substring(1)
        // 聊天页面
        routeMap["chatNotification"] = MainModule.CHAT.substring(1)
        // 音视频页面
        routeMap["rtcCall"] = RtcModule.VIDEO_CALL.substring(1)
        // 设置密聊密码页面
        routeMap["setEncPwd"] = MainModule.ENCRYPT_PWD.substring(1)
    }

    /**
     * 分发Deep Link，跳转具体页面
     */
    fun dispatchDeepLink(intent: Intent?) {
        val route = intent?.getParcelableExtra("route") as? Uri?
        route?.apply {
            // [scheme]://[authority]?k=v&k=v
            val type = getQueryParameter("type")
            if (type != null) {
                val path = routeMap[type]
                val uri = buildUpon().appendEncodedPath(path).build()
                // [scheme]://[authority]/[path]?k=v&k=v
                ARouter.getInstance().build(uri).navigation()
            }
        }
    }

    fun registerWebUrl(url: String, postcard: Postcard) {
        if (url.isNotEmpty()) {
            webUrlRegistry[url] = postcard
        }
    }

    fun findWebUrlRoute(url: String?): Postcard? {
        if (url == null) return null
        webUrlRegistry.forEach {
            if (url.startsWith(it.key)) {
                return it.value
            }
        }
        return null
    }
}