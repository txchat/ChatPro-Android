package com.fzm.chat.core.data.comparator

import com.fzm.chat.core.data.model.RecentContactMsg

/**
 * @author zhengjy
 * @since 2021/01/11
 * Description:
 */
class RecentMsgComparator : Comparator<RecentContactMsg> {
    override fun compare(o1: RecentContactMsg, o2: RecentContactMsg): Int {
        val sticky1 = if (o1.isDeleted() || !o1.isStickTop()) 2 else 1
        val sticky2 = if (o2.isDeleted() || !o2.isStickTop()) 2 else 1
        // 根据置顶排序
        return if (sticky1 < sticky2) {
            -1
        } else if (sticky1 > sticky2) {
            1
        } else {
            // 根据最后消息时间排序
            if (o1.getPriority() > o2.getPriority()) {
                -1
            } else if (o1.getPriority() < o2.getPriority()) {
                1
            } else {
                0
            }
        }
    }
}