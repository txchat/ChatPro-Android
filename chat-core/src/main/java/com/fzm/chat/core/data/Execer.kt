package com.fzm.chat.core.data

import android.util.LruCache
import com.fzm.chat.core.utils.ifNull
import java.util.regex.Pattern

/**
 * @author zhengjy
 * @since 2022/02/24
 * Description:
 */
private val cache: LruCache<String, Pattern> = LruCache(5)

fun execerPattern(name: String): Pattern {
    return cache[name].ifNull {
        Pattern.compile("^user\\.p\\.[\\w]+\\.${name}").also { cache.put(name, it) }
    }
}

private val pattern = Pattern.compile("^user\\.p\\.([\\w]+)[.\\w+]?")

/**
 * 从BTY主链以及平行链的执行器中获取[platform]
 */
val String.platform: String
    get() {
        val matcher = pattern.matcher(this)
        if (matcher.find()) {
            return matcher.group(1) ?: "bty"
        }
        // 匹配不到则默认为主链:bty
        return "bty"
    }

/**
 * 根据链名获取指定执行器
 */
fun String.execer(name: String): String {
    return if (startsWith("user.p.")) {
        "${this}.$name"
    } else {
        name
    }
}