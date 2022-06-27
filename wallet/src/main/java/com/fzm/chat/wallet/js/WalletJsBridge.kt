package com.fzm.chat.wallet.js

import android.webkit.JavascriptInterface
import com.fzm.chat.biz.webview.BizWebBridge
import com.fzm.chat.biz.webview.BizWebView
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.net.source.TransactionSource
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.route
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.router.wallet.WalletService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zjy.architecture.data.Result
import com.zjy.architecture.di.rootScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.qualifier.named
import wendu.dsbridge.CompletionHandler

/**
 * @author zhengjy
 * @since 2021/10/26
 * Description:
 */
class WalletJsBridge(webView: BizWebView) : BizWebBridge(webView) {

    private val gson by rootScope.inject<Gson>()
    private val delegate by rootScope.inject<LoginDelegate>()
    private val transaction by rootScope.inject<TransactionSource?>(named("Wallet"))
    private val walletService by route<WalletService>(WalletModule.SERVICE)

    @JavascriptInterface
    fun getNodeServer(params: Any): String {
        return AppPreference.CONTRACT_URL
    }

    @JavascriptInterface
    fun sendTransaction(params: Any, handler: CompletionHandler<String>) {
        val json = gson.fromJson<Map<String, Any?>>(params.toString(), object : TypeToken<Map<String, Any?>>(){}.type)
        val txHex = json["txHex"] as? String?
        val execer = json["execer"] as? String?
        if (txHex.isNullOrEmpty()) {
            handler.complete("")
            return
        }
        GlobalScope.launch(Dispatchers.Main) {
            val result = if (execer.isNullOrEmpty()) {
                transaction?.handle { Result.Success(txHex) }
            } else {
                transaction?.handleForRawHash(execer) { Result.Success(txHex) }
            }
            handler.complete(result?.dataOrNull() ?: "")
        }
    }

    @JavascriptInterface
    fun getCoins(params: Any, handler: CompletionHandler<String>) {
        GlobalScope.launch {
            val list = walletService?.getModuleAssets(delegate.getAddress() ?: "")
            handler.complete(gson.toJson(list))
        }
    }
}