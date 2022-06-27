package com.fzm.push

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.fzm.chat.biz.ui.DeepLinkHelper
import com.google.gson.Gson
import com.umeng.message.UmengNotifyClickActivity
import com.umeng.message.entity.UMessage
import com.zjy.architecture.util.other.BarUtils
import org.android.agoo.common.AgooConstants

/**
 * @author zhengjy
 * @since 2021/04/02
 * Description:厂商渠道通知
 */
class SystemPushActivity : UmengNotifyClickActivity() {

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.activity_system_push)
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.biz_color_primary), 0)
        BarUtils.setStatusBarLightMode(this, true)
    }

    override fun onMessage(intent: Intent) {
        super.onMessage(intent)
        val body = intent.getStringExtra(AgooConstants.MESSAGE_BODY)
        val message = Gson().fromJson(body, UMessage::class.java)
        val address = message.extra["address"]
        val channelType = message.extra["channelType"]
        startActivity(Intent().apply {
            val uri =
                Uri.parse(DeepLinkHelper.APP_LINK + "?type=chatNotification&address=$address&channelType=$channelType")
            component = ComponentName(packageName, "com.fzm.chat.app.MainActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("route", uri)
        })
    }

    override fun onRestart() {
        super.onRestart()
        finish()
    }
}