package com.fzm.oa

import android.app.Activity
import android.content.Intent
import android.webkit.JavascriptInterface
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.bus.BusEvent
import com.fzm.chat.biz.bus.LiveDataBus
import com.fzm.chat.biz.bus.event.ChangeTabEvent
import com.fzm.chat.biz.webview.BizWebBridge
import com.fzm.chat.biz.webview.BizWebView
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.data.bean.PreSendParams
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.repo.GroupRepository
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.core.CoreModule
import com.fzm.chat.router.core.CoreService
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.oa.OAModule
import com.fzm.chat.router.oa.OAService
import com.fzm.chat.router.oa.hasCompany
import com.fzm.chat.router.route
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.ext.toast
import dtalk.biz.Biz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import wendu.dsbridge.CompletionHandler

/**
 * @author zhengjy
 * @since 2021/09/04
 * Description:
 */
class OaJsBridge(webView: BizWebView) : BizWebBridge(webView) {

    private val gson by rootScope.inject<Gson>()
    private val delegate by rootScope.inject<LoginDelegate>()
    private val repository by rootScope.inject<GroupRepository>()
    private val coreService by route<CoreService>(CoreModule.SERVICE)
    private val oaService by route<OAService>(OAModule.SERVICE)

    @JavascriptInterface
    fun selectGroupMembers(params: Any, handler: CompletionHandler<String>) {
        val json = params as JSONArray
        val groupMembers = ArrayList<String>().apply {
            for (index in 0 until json.length()) {
                add(json[index].toString())
            }
        }
        webView.activity.setResult(Activity.RESULT_OK, Intent().putStringArrayListExtra("users", groupMembers))
        webView.activity.finish()
    }

    @JavascriptInterface
    fun openCompanyUserInfo(params: Any, handler: CompletionHandler<String>) {
        val json = params as JSONObject
        ARouter.getInstance().build(MainModule.CONTACT_INFO)
            .withString("address", json.optString("address"))
            .navigation()
    }

    @JavascriptInterface
    fun scanCode(params: Any, handler: CompletionHandler<String>) {
        ARouter.getInstance().build(MainModule.QR_SCAN).navigation()
    }

    @JavascriptInterface
    fun getUserInfo(params: Any): Any? {
        val info = delegate.companyUser.value ?: return null
        if (!info.hasCompany) return null
        return gson.toJson(info)
    }

    @JavascriptInterface
    fun getGroupMembers(params: Any, handler: CompletionHandler<String>) {
        val json = params as JSONObject
        GlobalScope.launch(Dispatchers.Main) {
            val users = repository.getGroupUserList(null, json.optLong("gid")).dataOrNull()?: emptyList()
            handler.complete(gson.toJson(users.map { it.address }))
        }
    }

    @JavascriptInterface
    fun getPublicKey(params: Any): String {
        return delegate.preference.PUB_KEY
    }

    @JavascriptInterface
    fun sign(params: Any): String {
        val map = gson.fromJson<Map<String, String>>(
            params.toString(),
            object : TypeToken<Map<String, String>>() {}.type
        )
        return coreService?.sign(map) ?: ""
    }

    @JavascriptInterface
    fun refreshCompanyState(params: Any): Any? {
        val map = gson.fromJson<Map<String, String>>(
            params.toString(),
            object : TypeToken<Map<String, String>>() {}.type
        )
        GlobalScope.launch(Dispatchers.Main) {
            oaService?.getCompanyUser(delegate.getAddress())
        }
        if (map["goHome"].toBoolean()) {
            ARouter.getInstance().build(AppModule.MAIN).withFlags(Intent.FLAG_ACTIVITY_NEW_TASK).navigation()
            LiveDataBus.of(BusEvent::class.java).changeTab().postValue(ChangeTabEvent(1, 0))
        }
        return null
    }

    @JavascriptInterface
    fun getOAServer(params: Any): String {
        return AppPreference.OA_URL
    }

    @JavascriptInterface
    fun getPhone(params: Any): String? {
        return delegate.current.value?.phone
    }

    @JavascriptInterface
    fun sendTeamCard(params: Any) {
        val map = gson.fromJson<Map<String, String>>(
            params.toString(),
            object : TypeToken<Map<String, String>>() {}.type
        )
        val id = map["id"]
        val name = map["name"]
        val avatar = map["avatar"] ?: ""
        val server = map["server"]
        if (id.isNullOrEmpty()) {
            webView.activity.toast("团队不能为空")
            return
        }
        if (name.isNullOrEmpty()) {
            webView.activity.toast("团队名称不能为空")
            return
        }
        if (server.isNullOrEmpty()) {
            webView.activity.toast("团队服务器不能为空")
            return
        }
        val content = MessageContent.contactCard(3, id, name, avatar, server, delegate.getAddress())
        val teamCard = PreSendParams(Biz.MsgType.ContactCard_VALUE, content, null)
        ARouter.getInstance().build(MainModule.CONTACT_SELECT)
            .withSerializable("preSend", teamCard)
            .navigation()
    }
}