package com.fzm.chat.wallet.ui

import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.biz.ui.WebContainerActivity
import com.fzm.chat.biz.webview.BizWebView
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.wallet.js.WalletJsBridge

/**
 * @author zhengjy
 * @since 2021/10/26
 * Description:
 */
@Route(path = WalletModule.WEB)
class WalletWebActivity : WebContainerActivity() {

    override fun setupWebView(bizWebView: BizWebView) {
        bizWebView.mWebView.addJavascriptObject(WalletJsBridge(bizWebView), null)
    }
}