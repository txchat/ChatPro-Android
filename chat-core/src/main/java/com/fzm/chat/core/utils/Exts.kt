package com.fzm.chat.core.utils

import android.text.TextUtils
import com.zjy.architecture.data.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.*

/**
 * @author zhengjy
 * @since 2021/01/05
 * Description:
 */
fun String.toHttpUrl(): String {
    return this
}

fun uuid() = UUID.randomUUID().toString()

/**
 * 中文获取首字母缩写
 */
fun getSearchKey(displayName: String): String {
    if (!PinyinUtils.isContainChinese(displayName)) return displayName
    val firstSpell = StringBuilder()
    val pingYin = StringBuilder()
    if (displayName.isNotEmpty()) {
        var index = 0
        while (index < displayName.length) {
            firstSpell.append(" ")
            pingYin.append(" ")
            val subName = displayName.substring(index)
            for (element in subName) {
                val charPingYin = PinyinUtils.getCharPingYin(element)
                if (!TextUtils.equals("#", charPingYin)) {
                    firstSpell.append(charPingYin.substring(0, 1))
                    pingYin.append(charPingYin)
                }
            }
            index++
        }
    }
    return firstSpell.append(pingYin).toString()
}

inline fun <T, R> Flow<Result<T>>.mapResult(crossinline transform: suspend (value: T) -> R) = map {
    if (it.isSucceed()) {
        transform(it.data())
    } else {
        throw it.error()
    }
}

inline fun <T, R> Flow<Result<T>>.mapResult(
    crossinline mapError: ((Throwable) -> Throwable),
    crossinline transform: suspend (value: T) -> R
) = map {
    if (it.isSucceed()) {
        transform(it.data())
    } else {
        throw mapError(it.error())
    }
}

/**
 * 转义特殊字符，防止SQL注入
 */
fun String.toLikeKey(): String {
    return replace("[", "/[")
        .replace("]", "/]")
        .replace("^", "/^")
        .replace("%", "/%")
        .replace("_", "/_")
        .replace("/", "//")
        .replace("'", "/'")
        .replace("&", "/&")
        .replace("(", "/(")
        .replace(")", "/)")
}

/**
 * 转义特殊字符，防止SQL注入
 */
fun String.toMatchKey(): String {
    return replace("\"", "")
        .replace("'", "\"'\"")
        .replace("*", "\"*\"")
        .replace("(", "\"(\"")
        .replace(")", "\")\"")
        .toLowerCase(Locale.CHINESE)
}

inline fun <T> T?.ifNull(block: () -> T): T = this ?: block()

inline fun <T : CharSequence> T?.ifNullOrEmpty(block: () -> T): T =
    if (isNullOrEmpty()) block() else this

fun <T> kotlin.Result<T>.toResult(): Result<T> = try {
    Result.Success(getOrThrow())
} catch (e: Exception) {
    Result.Error(e)
}

private val format = DecimalFormat("#0.##########")

fun Double.mul(num: Int): Long {
    return BigDecimal(format.format(this)).multiply(BigDecimal(format.format(num))).toLong()
}

fun String.mul(num: Int): Long {
    return BigDecimal(this).multiply(BigDecimal(format.format(num))).toLong()
}