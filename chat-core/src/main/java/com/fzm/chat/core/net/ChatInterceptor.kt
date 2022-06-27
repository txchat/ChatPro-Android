package com.fzm.chat.core.net

import android.content.Context
import android.os.Build
import com.fzm.chat.router.core.CoreModule
import com.fzm.chat.router.core.CoreService
import com.fzm.chat.router.route
import com.zjy.architecture.ext.versionName
import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author zhengjy
 * @since 2020/12/08
 * Description:
 */
class ChatInterceptor(private val context: Context) : Interceptor {

    private val coreService by route<CoreService>(CoreModule.SERVICE)

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder().apply {
            addHeader("FZM-UUID", "uuid")
            addHeader("Fzm-Request-Source", "chat")
            addHeader("FZM-VERSION", context.versionName)
            addHeader("FZM-DEVICE", "Android")
            addHeader("FZM-DEVICE-NAME", Build.MODEL)
            addHeader("FZM-SIGNATURE", coreService?.signAuth() ?: "")
        }
        return chain.proceed(requestBuilder.build())
    }
}