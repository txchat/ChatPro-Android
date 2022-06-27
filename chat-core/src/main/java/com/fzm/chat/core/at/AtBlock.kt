package com.fzm.chat.core.at

import java.io.Serializable
import java.util.ArrayList

/**
 * @author zhengjy
 * @since 2019/08/19
 * Description:
 */
class AtBlock(name: String) {

    /**
     * text = "@" + name
     */
    var text: String = "@$name"

    /**
     * 在文本中的位置
     */
    private val segments: MutableList<AitSegment> = ArrayList()

    /**
     * 新增 segment
     *
     * @param start
     * @return
     */
    fun addSegment(start: Int): AitSegment {
        val end = start + text.length - 1
        val segment = AitSegment(start, end)
        segments.add(segment)
        return segment
    }

    /**
     * 当进行插入操作时，移动@块的位置
     *
     * @param start      起始光标位置
     * @param changeText 插入文本
     */
    fun moveRight(start: Int, changeText: String?) {
        if (changeText == null) {
            return
        }
        val length = changeText.length
        for (segment in segments) {
            // 从已有的一个@块中插入
            if (start > segment.start && start <= segment.end) {
                segment.end += length
                segment.broken = true
            } else if (start <= segment.start) {
                segment.start += length
                segment.end += length
            }
        }
    }

    /**
     * 当进行删除操作时，移动@块的位置
     *
     * @param start  删除前光标位置
     * @param length 删除块的长度
     */
    fun moveLeft(start: Int, length: Int) {
        val after = start - length
        val iterator = segments.iterator()
        while (iterator.hasNext()) {
            val segment = iterator.next()
            // 从已有@块中删除
            if (start > segment.start) {
                // @被删除掉
                if (after <= segment.start) {
                    iterator.remove()
                } else if (after <= segment.end) {
                    segment.broken = true
                    segment.end -= length
                }
            } else {
                segment.start -= length
                segment.end -= length
            }
        }
    }

    /**
     * 获取该账号所有有效的@块最靠前的start
     *
     * @return
     */
    val firstSegmentStart: Int
        get() {
            var start = -1
            for (segment in segments) {
                if (segment.broken) continue
                if (start == -1 || segment.start < start) {
                    start = segment.start
                }
            }
            return start
        }

    /**
     * 获取该账号所有有效的@块最靠后的end
     *
     * @return
     */
    val lastSegmentEnd: Int
        get() {
            var end = -1
            for (segment in segments) {
                if (segment.broken) continue
                if (end == -1 || segment.end < end) {
                    end = segment.end
                }
            }
            return end
        }

    fun findLastSegmentByEnd(end: Int): AitSegment? {
        val pos = end - 1
        for (segment in segments) {
            if (!segment.broken && segment.end == pos) {
                return segment
            }
        }
        return null
    }

    fun valid(): Boolean {
        if (segments.size == 0) {
            return false
        }
        for (segment in segments) {
            if (segment.broken) {
                return false
            }
        }
        return true
    }

    data class AitSegment(
        /**
         * 位于文本起始位置(include)
         */
        var start: Int,
        /**
         * 位于文本结束位置(include)
         */
        var end: Int
    ): Serializable {
        /**
         * 是否坏掉
         */
        var broken = false
    }

}