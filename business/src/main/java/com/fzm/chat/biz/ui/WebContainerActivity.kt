package com.fzm.chat.biz.ui

import android.content.Intent
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.RelativeLayout
import android.widget.TextView
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.biz.R
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.base.GlobalConfig
import com.fzm.chat.biz.databinding.ActivityWebContainerBinding
import com.fzm.chat.biz.databinding.ActivityWebMiniAppBinding
import com.fzm.chat.biz.webview.BizWebBridge
import com.fzm.chat.biz.webview.BizWebView
import com.fzm.chat.biz.webview.BizWebViewClient
import com.fzm.chat.router.biz.BizModule
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.setVisible

/**
 * @author zhengjy
 * @since 2020/08/28
 * Description:
 */
@Route(path = BizModule.WEB_ACTIVITY)
open class WebContainerActivity : BizActivity() {

    protected var url: String? = ""

    protected var showTitle = false

    protected var title: String? = null

    protected var miniAppStyle: Boolean = false

    private val _root by lazy {
        url = intent.getStringExtra("url") ?: ""
        showTitle = intent.getBooleanExtra("showTitle", showTitle)
        title = intent.getStringExtra("title") ?: ""
        miniAppStyle = intent.getBooleanExtra("miniAppStyle", miniAppStyle)

        if (this.javaClass.simpleName == "WebContainerActivity") {
            // 只有父类执行这个判断
            val postcard = DeepLinkHelper.findWebUrlRoute(url)
            if (postcard != null) {
                postcard.withString("url", url).navigation(this)
                finish()
            }
        }

        if (miniAppStyle) {
            ActivityWebMiniAppBinding.inflate(layoutInflater).root
        } else {
            ActivityWebContainerBinding.inflate(layoutInflater).root
        }
    }

    protected lateinit var bizWeb: BizWebView
    protected lateinit var tvTitle: TextView
    protected lateinit var rlTitle: RelativeLayout
    protected lateinit var ivBack: View
    protected var reload: View? = null

    override val root: View
        get() = _root

    override fun initView() {
        bizWeb = findViewById(R.id.biz_web)
        tvTitle = findViewById(R.id.tv_title)
        rlTitle = findViewById(R.id.rl_title)
        ivBack = findViewById(R.id.iv_back)
        if (miniAppStyle) {
            reload = findViewById(R.id.iv_reload)
        }

        bizWeb.setup(url ?: "") {
            setActivity(instance)
            mWebView.settings.cacheMode = if (GlobalConfig.DEBUG) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
            mWebView.settings.userAgentString = "${mWebView.settings.userAgentString}${AppConfig.USER_AGENT}"
            mWebView.webViewClient = object : BizWebViewClient(this@WebContainerActivity) {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (title.isNullOrEmpty()) {
                        tvTitle.text = view?.title
                    }
                }
            }
            mWebView.addJavascriptObject(BizWebBridge(this), null)
            mWebView.loadUrl(url ?: "")
            setupWebView(this)
        }
        rlTitle.setVisible(showTitle || !title.isNullOrEmpty())
        if (!title.isNullOrEmpty()) {
            tvTitle.text = title
        }
    }

    open fun setupWebView(bizWebView: BizWebView) {

    }

    override fun initData() {

    }

    override fun setEvent() {
        ivBack.setOnClickListener { finish() }
        reload?.setOnClickListener { bizWeb.mWebView.reload() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        bizWeb.onActivityResult(requestCode, resultCode, data)
    }

    override fun finish() {
        super.finish()
        if (miniAppStyle) {
            overridePendingTransition(0, R.anim.biz_slide_bottom_out)
        }
    }

    override fun onBackPressedSupport() {
        if (bizWeb.mWebView.canGoBack()) {
            bizWeb.mWebView.goBack()
        } else {
            super.onBackPressedSupport()
        }
    }
}