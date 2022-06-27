package com.fzm.chat.core.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.data.contract.sign
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.CipherUtils
import com.fzm.chat.router.core.CoreModule
import com.fzm.chat.router.core.CoreService
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.main.MainService
import com.fzm.chat.router.route
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.ext.sha256Bytes
import com.zjy.architecture.util.ActivityUtils
import com.zjy.architecture.util.logV
import dtalk.biz.Biz
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * @author zhengjy
 * @since 2020/12/08
 * Description:
 */
@Route(path = CoreModule.SERVICE)
class CoreServiceImpl : CoreService {

    private var signData = ""
    private var lastSign = 0L
    private lateinit var mContext: Context

    private val delegate by rootScope.inject<LoginDelegate>()
    private val mainService by route<MainService>(MainModule.SERVICE)

    init {
        Handler(Looper.getMainLooper()).post {
            delegate.loginEvent.observeForever {
                // 退出登录时，重置签名
                if (!it) signData = ""
            }
        }
    }

    override fun sign(map: Map<String, String>): String {
        if (!delegate.isLogin()) return ""
        return map.sign(delegate.preference.PRI_KEY)
    }

    override fun sign(data: String): ByteArray {
        if (!delegate.isLogin()) {
            return ByteArray(0)
        }
        return CipherUtils.sign(data.toByteArray().sha256Bytes(), delegate.preference.PRI_KEY)
    }

    override fun signAuth(): String {
        if (!delegate.isLogin()) {
            return ""
        }
        if (System.currentTimeMillis() - lastSign < 10_000L && signData.isNotEmpty()) {
            return signData
        }
        val msg = "${System.currentTimeMillis()}*${generateRandomString(8)}"
        val random = sign(msg)
        val encoded = Base64.encode(random, Base64.NO_WRAP)
        signData = "${String(encoded, StandardCharsets.US_ASCII)}#$msg#${delegate.preference.PUB_KEY}"
        lastSign = System.currentTimeMillis()
        return signData
    }

    override fun authPayload(firstConnect: Boolean): ByteArray {
        val info = delegate.current.value
        return Biz.AuthExt.newBuilder().setDevice(Biz.Device.Android)
            .setNickname(info?.nickname?.ifEmpty { info.address })
            .setDeviceToken(AppPreference.deviceToken)
            .setConnType(if (!delegate.isAutoLogin() && firstConnect) Biz.AuthExt.ConnType.Connect else Biz.AuthExt.ConnType.Reconnect)
            .setDeviceName("${Build.MANUFACTURER} ${Build.MODEL}")
            .setUuid(AppPreference.uuid)
            .build()
            .toByteArray()
    }

    override fun parseAuthReply(payload: ByteArray) {
        if (payload.isEmpty()) return
        GlobalScope.launch {
            val reply = Biz.AuthReply.parseFrom(payload)
            if (reply.uuid != AppPreference.uuid) {
                mainService?.onOtherEndPointLogin(reply.deviceName, reply.datetime, reply.deviceValue)
            }
        }
    }

    override fun checkService() {
        if (!this::mContext.isInitialized) {
            return
        }
        if (isServiceWorked()) {
            return
        }
        try {
            if (ActivityUtils.isBackground) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mContext.startForegroundService(Intent(mContext, MessageService::class.java).apply {
                        putExtra("startForeground", true)
                    })
                    logV("MessageService", "startForegroundService")
                }
            } else {
                mContext.startService(Intent(mContext, MessageService::class.java))
                logV("MessageService", "startService")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isServiceWorked(): Boolean {
        try {
            val manager = mContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
            val runningService = manager?.getRunningServices(Integer.MAX_VALUE) ?: return false
            for (i in runningService.indices) {
                if (runningService[i].service.className == "com.fzm.chat.core.service.MessageService") {
                    if (runningService[i].process.startsWith(mContext.packageName ?: "")) {
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun generateRandomString(length: Int): String {
        val str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = Random()
        val sb = StringBuilder()
        for (i in 0 until length) {
            val number = random.nextInt(62)
            sb.append(str[number])
        }
        return sb.toString()
    }

    override fun init(context: Context) {
        mContext = context
    }
}