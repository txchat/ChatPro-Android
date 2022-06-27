package com.fzm.chat.biz.webview

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.router.biz.BizModule

/**
 * @author zhengjy
 * @since 2020/09/24
 * Description:
 */
open class BizWebViewClient(private val activity: Activity) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        request?.url?.toString()?.also {
            when {
                it.startsWith(AppConfig.APP_SCHEME) -> {
                    ARouter.getInstance().build(BizModule.DEEP_LINK)
                        .withParcelable("route", Uri.parse(it))
                        .navigation()
                }
                it.startsWith("http") -> return false
                else -> {
                    try {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return true
    }
}