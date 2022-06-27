package com.fzm.chat.core.data.model

import com.fzm.chat.core.data.bean.Contact

/**
 * @author zhengjy
 * @since 2020/12/28
 * Description:
 */
interface RecentContactMsg : Contact {

    companion object {
        const val AVATAR = "AVATAR"
        const val NAME = "NAME"
        const val AT = "AT"
        const val DRAFT = "DRAFT"
        const val UNREAD = "UNREAD"
        const val CONTENT = "CONTENT"
        const val TIME = "TIME"
        const val NO_DISTURB = "NO_DISTURB"
        const val STICK_TOP = "STICK_TOP"
        const val STATUS = "STATUS"
        const val TAG = "TAG"
    }

    /**
     * 消息置顶
     */
    fun isStickTop(): Boolean

    /**
     * 消息免打扰
     */
    fun isNoDisturb(): Boolean

    /**
     * 未读消息数
     */
    fun unreadNum(): Int

    /**
     * 是否有被@消息
     */
    fun beAtMsg(): Boolean

    /**
     * 获取会话的草稿
     */
    fun getDraft(): Draft?

    /**
     * 获取消息显示内容
     */
    fun getContent(): String

    /**
     * 获取消息时间
     */
    fun getTime(): Long

    /**
     * 获取消息显示优先级
     */
    fun getPriority(): Long

    /**
     * 是否已删除好友或退群
     */
    fun isDeleted(): Boolean

    /**
     * 获取标志位信息
     */
    fun getFlags(): Int

    /**
     * 获取最新一条消息对象
     */
    fun getRecentLog(): RecentLog

}

fun RecentContactMsg.getDraftText(): String? = getDraft()?.text

fun RecentContactMsg.getDraftTime(): Long = getDraft()?.time ?: 0L

val RecentContactMsg.hasDraft: Boolean get() = !getDraftText().isNullOrEmpty() || getDraft()?.reference != null