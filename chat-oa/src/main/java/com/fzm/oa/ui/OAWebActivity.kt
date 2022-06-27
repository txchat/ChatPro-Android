package com.fzm.oa.ui

import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.biz.ui.WebContainerActivity
import com.fzm.chat.biz.webview.BizWebView
import com.fzm.chat.router.oa.OAModule
import com.fzm.oa.OaJsBridge

/**
 * @author zhengjy
 * @since 2020/08/28
 * Description:
 */
@Route(path = OAModule.WEB)
class OAWebActivity : WebContainerActivity() {

    override fun setupWebView(bizWebView: BizWebView) {
        bizWebView.mWebView.addJavascriptObject(OaJsBridge(bizWebView), null)
    }
}