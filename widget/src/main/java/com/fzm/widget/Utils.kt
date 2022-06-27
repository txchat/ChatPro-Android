package com.fzm.widget

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.text.TextUtils
import android.util.Patterns
import android.util.TypedValue
import android.view.WindowManager
import kotlin.math.roundToInt

/**
 * @author zhengjy
 * @since 2020/07/31
 * Description:
 */

// 将dp转换为px
internal val Float.dp
    get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this,
            Resources.getSystem().displayMetrics
    ).roundToInt()

internal val Int.dp
    get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
    ).roundToInt()

/**
 * 将px转换为dp
 */
internal val Int.px
    get() = this / Resources.getSystem().displayMetrics.density

/**
 * 获取屏幕的宽高
 */
val Context.screenSize: Point
    get() {
        val point = Point()
        (getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.getSize(point)
        return point
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
    if (TextUtils.isEmpty(str)) {
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
        account.length > 8 -> encryptPhone(account)
        else -> account
    }
}
