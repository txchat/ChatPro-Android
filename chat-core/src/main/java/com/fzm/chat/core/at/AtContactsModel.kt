package com.fzm.chat.core.at

import com.fzm.chat.core.at.AtBlock.AitSegment
import java.util.HashMap

/**
 * @author zhengjy
 * @since 2019/08/19
 * Description:
 */
internal class AtContactsModel {

    /**
     * 保存已@的成员和对应的@块
     */
    private val atBlocks: MutableMap<String, AtBlock> = HashMap()

    /**
     * 清除所有的@块
     */
    fun reset() {
        atBlocks.clear()
    }

    fun addAitMember(account: String, name: String, start: Int) {
        atBlocks.getOrPut(account) { AtBlock(name) }.apply { addSegment(start) }
    }

    /**
     * 查所有被@的群成员
     *
     * @return  被@人的id列表
     */
    val aitMembers: List<String>?
        get() {
            if (atBlocks.isEmpty()) return null
            val members = mutableListOf<String>()
            for (account in atBlocks.keys) {
                atBlocks[account]?.also {
                    if (it.valid()) members.add(account)
                }
            }
            return members
        }

    /**
     * 对外暴露的@详细信息
     */
    internal val aitInfo: Map<String, AtBlock>
        get() = atBlocks

    internal fun restoreFrom(aitInfo: Map<String, AtBlock>) {
        atBlocks.putAll(aitInfo)
    }

    /**
     * 找到 curPos 恰好命中 end 的segment
     *
     * @param start
     * @return
     */
    fun findAitSegmentByEndPos(start: Int): AitSegment? {
        for (account in atBlocks.keys) {
            atBlocks[account]?.also {
                val segment = it.findLastSegmentByEnd(start)
                if (segment != null) {
                    return segment
                }
            }
        }
        return null
    }

    /**
     * 文本插入后更新@块的起止位置
     *
     * @param start         @块起始位置
     * @param changeText    插入文本的长度
     */
    fun onInsertText(start: Int, changeText: String?) {
        val iterator = atBlocks.keys.iterator()
        while (iterator.hasNext()) {
            val account = iterator.next()
            atBlocks[account]?.also {
                it.moveRight(start, changeText)
                if (!it.valid()) iterator.remove()
            }
        }
    }

    /**
     * 文本删除后更新@块的起止位置
     *
     * @param start     @块起始位置
     * @param length    @块的长度
     */
    fun onDeleteText(start: Int, length: Int) {
        val iterator = atBlocks.keys.iterator()
        while (iterator.hasNext()) {
            val account = iterator.next()
            atBlocks[account]?.also {
                it.moveLeft(start, length)
                if (!it.valid()) iterator.remove()
            }
        }
    }
}