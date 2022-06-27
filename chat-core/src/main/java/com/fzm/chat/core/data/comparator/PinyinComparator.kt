package com.fzm.chat.core.data.comparator

import com.fzm.chat.core.data.bean.Sortable
import java.util.*

/**
 * @author zhengjy
 * @since 2020/12/22
 * Description:
 */
class PinyinComparator : Comparator<Sortable> {

    override fun compare(o1: Sortable, o2: Sortable): Int {
        if (o1.priority() == o2.priority()) {
            if (o1.getFirstLetter() == "#" && o2.getFirstLetter() == "#") {
                return o1.getLetters().compareTo(o2.getLetters())
            } else if (o2.getFirstLetter() == "#") {
                return -1
            } else if (o1.getFirstLetter() == "#") {
                return 1
            } else {
                if (o1.getFirstLetter() == o2.getFirstLetter()) {
                    if (o1.getFirstChar().codePointAt(0) == o2.getFirstChar().codePointAt(0)) {
                        val p1 = o1.getLetters()
                        val p2 = o2.getLetters()
                        var i = 0
                        while (true) {
                            if (p1.length <= i && p2.length <= i) {
                                return 0
                            }
                            if (p1.length <= i) {
                                return -1
                            }
                            if (p2.length <= i) {
                                return 1
                            }
                            if (p1[i] == p2[i]) {
                                i++
                                continue
                            }
                            return p1.substring(i, i + 1).compareTo(p2.substring(i, i + 1))
                        }
                    } else {
                        return o1.getFirstChar().codePointAt(0) - o2.getFirstChar().codePointAt(0)
                    }
                } else {
                    return o1.getFirstLetter().compareTo(o2.getFirstLetter())
                }
            }
        } else return if (o1.priority() > o2.priority()) {
            -1
        } else {
            1
        }
    }
}