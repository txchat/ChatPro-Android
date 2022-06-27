package com.fzm.chat.core.utils

import com.fzm.chat.core.data.ChatConst

/**
 * @author zhengjy
 * @since 2021/11/29
 * Description:
 */
private fun getChineseMnem(mnem: String): String {
    return mnem.replace(" ", "")
        .replace("\u200b", "")
        .replace("\n", "")
        .replace("", " ")
        .trim()
}

fun String?.formatMnemonic(): String {
    if (isNullOrEmpty()) return ""
    val first = replace("\u200b", "").substring(0, 1)
    val mnemWithSpace = if (first.matches(ChatConst.REGEX_CHINESE.toRegex())) {
        getChineseMnem(this)
    } else {
        this
    }
    return mnemWithSpace
}