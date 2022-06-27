package com.fzm.chat.biz.utils

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Patterns
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.res.ResourcesCompat
import com.fzm.chat.biz.R
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.utils.AddressValidationUtils
import com.zjy.architecture.ext.tryWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author zhengjy
 * @since 2021/04/27
 * Description:
 */

fun defaultCustomTabIntent(context: Context? = null): CustomTabsIntent {
    return CustomTabsIntent.Builder()
        .apply {
            if (context != null) {
                setShowTitle(true)
                setUrlBarHidingEnabled(true)
                setDefaultColorSchemeParams(defaultCustomTabColorScheme(context.resources))
            }
        }
        .build()
}

fun defaultCustomTabColorScheme(resources: Resources) = CustomTabColorSchemeParams.Builder()
    .setToolbarColor(ResourcesCompat.getColor(resources, R.color.biz_color_accent_bubble, null))
    .setSecondaryToolbarColor(
        ResourcesCompat.getColor(
            resources,
            R.color.biz_color_accent_bubble,
            null
        )
    )
    .build()

/**
 * 获取群或者好友的默认头像
 */
@DrawableRes
fun defaultAvatar(channelType: Int): Int {
    return if (channelType == ChatConst.PRIVATE_CHANNEL) {
        R.mipmap.default_avatar_round
    } else {
        R.mipmap.default_avatar_room
    }
}

fun Context.callPhone(number: String?) {
    if (number.isNullOrEmpty()) return
    tryWith {
        startActivity(Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
        })
    }
}

/**
 * ping指定主机
 */
suspend fun ping(host: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // -c：代表发送数据包的个数
            // -w：超时时间，单位秒
            val process = Runtime.getRuntime().exec("ping -c 2 -w 3 $host")
            val result = process.waitFor() == 0
            result
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 将一个字符串按要求用'*'加密
 *
 * @param str  要加密的字符串
 * @param from from之后的字符加密
 * @param to   to之前的字符加密
 * @return 加密后的字符串
 */
fun encryptString(str: String, from: Int, to: Int): String {
    if (str.isEmpty()) {
        return ""
    }
    val length = str.length
    if (length < from || length < to || from < 0 || to < 0) {
        return str
    }
    val sb = StringBuilder()
    if (from <= to) {
        for (i in 0 until from) {
            sb.append(str[i])
        }
        for (i in from until to) {
            sb.append("*")
        }
        for (i in to until length) {
            sb.append(str[i])
        }
    } else {
        for (i in 0 until to) {
            sb.append("*")
        }
        for (i in to until from) {
            sb.append(str[i])
        }
        for (i in from until length) {
            sb.append("*")
        }
    }
    return sb.toString()
}

/**
 * 隐藏手机号
 */
fun encryptPhone(phoneNumber: String): String {
    return encryptString(phoneNumber, phoneNumber.length - 8, phoneNumber.length - 4)
}

fun encryptAddress(address: String): String {
    if (address.length <= 8) {
        return address
    }
    val start: String = address.substring(0, 4)
    val end: String = address.substring(address.length - 4)
    return "$start****$end"
}

/**
 * 隐藏手机号或邮箱
 */
fun encryptAccount(account: String): String {
    return when {
        account.matches(Patterns.EMAIL_ADDRESS.toRegex()) -> {
            val sb = StringBuilder()
            val at = account.indexOf("@")
            when {
                at == 1 -> sb.append("*")
                at <= 3 -> {
                    sb.append(account[0])
                    for (i in 0 until at - 1) {
                        sb.append("*")
                    }
                }
                else -> {
                    sb.append(account[0])
                    sb.append(account[1])
                    sb.append(account[2])
                    for (i in 0 until at - 3) {
                        sb.append("*")
                    }
                }
            }
            sb.append(account.substring(at))
            sb.toString()
        }
        account.length == 11 -> encryptPhone(account)
        AddressValidationUtils.bitCoinAddressValidate(account) -> encryptAddress(account)
        else -> account
    }
}

/**
 * 高亮地址后size位
 */
fun highlightAddress(address: CharSequence?, @ColorInt color: Int, size: Int = 4): CharSequence? {
    if (address == null || address.length <= size) {
        return address
    }
    val ssb = SpannableString(address)
    val span = ForegroundColorSpan(color)
    ssb.setSpan(span, address.length - size, address.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
    return ssb
}