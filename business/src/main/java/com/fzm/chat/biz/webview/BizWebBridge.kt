package com.fzm.chat.biz.webview

import android.content.ClipData
import android.webkit.JavascriptInterface
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.ui.UserInfoAuthDialogFragment
import com.fzm.chat.core.data.bean.PreSendParams
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.ifNullOrEmpty
import com.fzm.chat.router.core.CoreModule
import com.fzm.chat.router.core.CoreService
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.route
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.ext.clipboardManager
import dtalk.biz.Biz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import wendu.dsbridge.CompletionHandler

/**
 * @author zhengjy
 * @since 2020/07/31
 * Description:
 */
open class BizWebBridge(protected val webView: BizWebView) {

    private val coreService by route<CoreService>(CoreModule.SERVICE)
    private val gson by rootScope.inject<Gson>()
    private val delegate by rootScope.inject<LoginDelegate>()

    /**
     * 按下返回键
     */
    @JavascriptInterface
    fun back(params: Any, handler: CompletionHandler<String>) {
        webView.post {
            webView.activity.onBackPressed()
        }
    }

    /**
     * 关闭当前activity
     */
    @JavascriptInterface
    fun close(params: Any, handler: CompletionHandler<String>) {
        webView.post {
            webView.activity.finish()
        }
    }

    @JavascriptInterface
    fun signAuth(params: Any): String {
        return coreService?.signAuth() ?: ""
    }

    @JavascriptInterface
    fun copy(params: Any) {
        val json = params as JSONObject
        val data = ClipData.newPlainText("Web", json.optString("text"))
        webView.activity.clipboardManager?.setPrimaryClip(data)
    }

    @JavascriptInterface
    fun openUserInfo(params: Any, handler: CompletionHandler<String>) {
        val json = params as JSONObject
        ARouter.getInstance().build(MainModule.CONTACT_INFO)
            .withString("address", json.optString("address"))
            .navigation()
    }

    @JavascriptInterface
    fun sendMessage(params: Any) {
        val map = gson.fromJson<Map<String, String>>(
            params.toString(),
            object : TypeToken<Map<String, String>>() {}.type
        )
        val msgType = map["msgType"]?.toInt() ?: return
        val preSend = when (Biz.MsgType.forNumber(msgType)) {
            Biz.MsgType.Text -> {
                val content = map["content"] ?: return
                val msg = MessageContent.text(content)
                PreSendParams(Biz.MsgType.Text_VALUE, msg, null)
            }
            Biz.MsgType.Image -> {
                null
            }
            Biz.MsgType.Video -> {
                null
            }
            Biz.MsgType.File -> {
                null
            }
            Biz.MsgType.ContactCard -> {
                val contactType = map["contactType"]?.toInt() ?: return
                val id = map["id"] ?: return
                val name = map["name"] ?: return
                val avatar = map["avatar"] ?: ""
                val server = map["server"] ?: ""
                val msg = MessageContent.contactCard(contactType, id, name, avatar, server, delegate.getAddress())
                PreSendParams(Biz.MsgType.ContactCard_VALUE, msg, null)
            }
            else -> null
        }

        if (preSend != null) {
            ARouter.getInstance().build(MainModule.CONTACT_SELECT)
                .withSerializable("preSend", preSend)
                .navigation()
        }
    }

    /**
     * 获取用户信息
     */
    @JavascriptInterface
    fun getCurrentUser(params: Any, handler: CompletionHandler<String>) {
        val map = gson.fromJson<Map<String, String>>(
            params.toString(),
            object : TypeToken<Map<String, String>>() {}.type
        )
        val name = map["name"].ifNullOrEmpty {
            handler.complete("name must be not empty")
            return
        }
        val avatar = map["avatar"] ?: ""
        val reason = map["reason"] ?: ""
        webView.activity.lifecycleScope.launch(Dispatchers.Main) {
            UserInfoAuthDialogFragment.create()
                .setName(name)
                .setAvatar(avatar)
                .setAuthReason(reason)
                .setOnAuthorizationListener {
                    handler.complete(it)
                }
                .setOnCancelListener {
                    handler.complete("cancel")
                }
                .show(webView.activity.supportFragmentManager, "USER_INFO_AUTH")
        }
    }
}