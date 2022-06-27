package com.fzm.chat.biz.utils

import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Pattern

/**
 * @author zhengjy
 * @since 2022/02/17
 * Description:
 */
class MoneyInputFilter(private val decimal: Boolean) : InputFilter {

    private val pattern = if (decimal) {
        Pattern.compile("[1234567890.]").toRegex()
    } else {
        Pattern.compile("[1234567890]").toRegex()
    }

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        if (source == null) return ""
        for (i in start until end) {
            if (!pattern.matches(source[i].toString())) {
                return ""
            }
        }
        if (decimal) {
            if (source.filter { it == '.' }.length > 1) {
                // 输入超过一个小数点
                return ""
            }
            if (source.contains(".") && !dest.isNullOrEmpty()) {
                // 已经有小数点，再次输入小数点
                val change = dend > dstart
                if (change) {
                    // 被替换以外部分有没有包含小数点
                    for (i in 0 until dstart) {
                        if (dest[i] == '.') {
                            return ""
                        }
                    }
                    for (i in dend until dest.length - 1) {
                        if (dest[i] == '.') {
                            return ""
                        }
                    }
                } else {
                    if (dest.contains(".")) {
                        return ""
                    }
                }
            }
            if (dest.isNullOrEmpty() && source.startsWith(".")) {
                // 首次输入小数点，自动在前面补0
                return "0${source}"
            }
        }
        return null
    }
}