package com.fzm.push

import android.annotation.SuppressLint
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.ui.DeepLinkHelper
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.session.LoginDelegate
import com.umeng.commonsdk.UMConfigure
import com.umeng.message.IUmengCallback
import com.umeng.message.IUmengRegisterCallback
import com.umeng.message.PushAgent
import com.umeng.message.UmengMessageHandler
import com.umeng.message.entity.UMessage
import com.zjy.architecture.Arch
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.util.immutableFlag
import com.zjy.architecture.util.logD
import com.zjy.architecture.util.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.android.agoo.huawei.HuaWeiRegister
import org.android.agoo.xiaomi.MiPushRegistar

/**
 * @author zhengjy
 * @since 2019/08/12
 * Description:
 */
object PushManager {

    private val delegate by rootScope.inject<LoginDelegate>()

    @SuppressLint("StaticFieldLeak")
    private lateinit var mPushAgent: PushAgent
    private var manager: NotificationManager? = null

    private var lastNotificationTime = 0L

    fun init(context: Context) {
        UMConfigure.init(
            context, PushConfig.UMENG_APP_KEY, AppConfig.APP_NAME_EN,
            UMConfigure.DEVICE_TYPE_PHONE, PushConfig.UMENG_MESSAGE_SECRET
        )
        UMConfigure.setLogEnabled(Arch.debug)
        mPushAgent = PushAgent.getInstance(context)

        manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        mPushAgent.resourcePackageName = "com.fzm.chat"
        mPushAgent.messageHandler = object : UmengMessageHandler() {
            override fun getNotification(context: Context, uMessage: UMessage): Notification {
                val builder = NotificationCompat.Builder(context, "chatMessage")
                builder.setSmallIcon(R.drawable.ic_notification)
                builder.setContentTitle(uMessage.title)
                builder.setContentText(uMessage.text)
                builder.setAutoCancel(true)

                val address = uMessage.extra["address"]
                val channelType = uMessage.extra["channelType"]
                val intent = Intent().apply {
                    val uri =
                        Uri.parse("${DeepLinkHelper.APP_LINK}?type=chatNotification&address=$address&channelType=$channelType")
                    component = ComponentName(
                        context.packageName,
                        "com.fzm.chat.app.MainActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("route", uri)
                }
                val contentIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT.immutableFlag
                )
                // 指定点击跳转页面
                builder.setContentIntent(contentIntent)
                return builder.build()
            }

            override fun dealWithNotificationMessage(context: Context, uMessage: UMessage) {
                logD("PushAgent", "notification title:${uMessage.title}  text:${uMessage.text}")
                if (!delegate.isLogin() || !delegate.preference.NEW_MSG_NOTIFY) {
                    // 没有登录则不显示通知
                    return
                }
                if (System.currentTimeMillis() - lastNotificationTime > 1000L) {
                    try {
                        // 消息通知间隔大于1s，才发出提示音
                        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        RingtoneManager.getRingtone(context, uri).play()
                    } catch (e: Exception) {
                        logD("PushAgent", "sound error: ${e.message}")
                    }
                }
                lastNotificationTime = System.currentTimeMillis()
                try {
                    val notification = getNotification(context, uMessage)
                    val address = uMessage.extra["address"]!!
                    manager?.notify(address.hashCode(), notification)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun dealWithCustomMessage(context: Context, uMessage: UMessage) {
                logD("PushAgent", "custom:${uMessage.custom}")
            }
        }
        mPushAgent.displayNotificationNumber = 5
        mPushAgent.notificationOnForeground = true

        mPushAgent.register(object : IUmengRegisterCallback {
            override fun onSuccess(deviceToken: String) {
                if (AppPreference.deviceToken.isEmpty()) {
                    AppPreference.deviceToken = deviceToken
                }
                //注册成功会返回deviceToken deviceToken是推送消息的唯一标志
                logD("PushAgent", "注册成功：deviceToken：-------->  $deviceToken")
            }

            override fun onFailure(s: String, s1: String) {
                logE("PushAgent", "注册失败：-------->  s:$s,s1:$s1")
            }
        })

        MiPushRegistar.register(context, PushConfig.MI_PUSH_ID, PushConfig.MI_PUSH_KEY)
        HuaWeiRegister.register(context as Application?)
//        MeizuRegister.register(context, PushConfig.MEIZU_PUSH_ID, PushConfig.MEIZU_PUSH_KEY)
//        OppoRegister.register(this, "appkey", "appSecret");
//        VivoRegister.register(this);

        GlobalScope.launch(Dispatchers.Main) {
            delegate.loginEvent.observeForever { login ->
                if (login) {
                    enablePush()
                } else {
                    disablePush()
                }
            }
        }
    }

    internal fun enablePush(callback: ((Boolean) -> Unit)? = null) {
        mPushAgent.enable(object : IUmengCallback {
            override fun onSuccess() {
                logD("PushAgent", "启用成功")
                callback?.invoke(true)
            }

            override fun onFailure(s: String, s1: String) {
                logE("PushAgent", "启用失败：-------->  s:$s,s1:$s1")
                callback?.invoke(false)
            }
        })
    }

    internal fun disablePush(callback: ((Boolean) -> Unit)? = null) {
        mPushAgent.disable(object : IUmengCallback {
            override fun onSuccess() {
                logD("PushAgent", "停用成功")
                callback?.invoke(true)
            }

            override fun onFailure(s: String, s1: String) {
                logE("PushAgent", "停用失败：-------->  s:$s,s1:$s1")
                callback?.invoke(false)
            }
        })
    }
}
