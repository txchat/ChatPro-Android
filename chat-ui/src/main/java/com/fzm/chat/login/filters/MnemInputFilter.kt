package com.fzm.chat.login.filters

import android.text.InputFilter
import android.text.SpannableStringBuilder
import android.text.Spanned

/**
 * @author zhengjy
 * @since 2020/12/11
 * Description:
 */
class MnemInputFilter : InputFilter {

    companion object {
        const val REGEX_CHINESE = "[ \u4e00-\u9fa5]+"

        //A前面要有个空格，注意
        const val REGEX_ENGLISH = "^[ A-Za-z]*$"

        //A前面要有个空格，注意
        const val REGEX_CHINESE_ENGLISH = "^[\u4e00-\u9fa5 A-Za-z]*$"
    }

    private var mRegex: String = ""

    override fun filter(
        source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int
    ): CharSequence? {
        var resetRegex = false
        if (source.isNullOrEmpty()) {
            return null
        }
        if (dend - dstart == dest?.length) {
            // 整体替换则重置mRegex
            resetRegex = true
            mRegex = ""
        }
        if (!source.toString().matches(REGEX_CHINESE.toRegex())
            && !source.toString().matches(REGEX_ENGLISH.toRegex())
        ) {
            // 既不是中文也不是英文则过滤
            return ""
        }
        if (mRegex.isNotEmpty()) {
            if (!source.toString().matches(mRegex.toRegex())) {
                if (dend - dstart > 0) {
                    return dest?.subSequence(dstart, dend)
                } else {
                    return ""
                }
            }
        }
        val first = if (resetRegex) {
            source[0].toString()
        } else {
            dest?.get(0)?.toString() ?: ""
        }
        if (first.isNotEmpty()) {
            if (first.matches(REGEX_CHINESE.toRegex())) {
                mRegex = REGEX_CHINESE
            } else {
                mRegex = REGEX_ENGLISH
            }
        } else {
            mRegex = ""
        }
        if (mRegex == REGEX_CHINESE) {
            val sb = SpannableStringBuilder(source, start, end)
            // 输入的每个中文字符之间加入空格
            val length = start + 2 * source.length - 1
            for (i in start + 1 until length step 2) {
                sb.insert(i, " ")
            }
            if (dest != null) {
                if (dstart - 1 >= 0 && dest[dstart - 1] != ' ') {
                    sb.insert(0, " ")
                }
                if (dend < dest.length && dest[dend] != ' ') {
                    sb.insert(sb.length, " ")
                }
            }
            return sb
        }

        return null
    }
}