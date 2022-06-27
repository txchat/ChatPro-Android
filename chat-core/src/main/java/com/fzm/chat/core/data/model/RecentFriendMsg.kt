package com.fzm.chat.core.data.model

import android.os.Bundle
import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.bean.Server
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/28
 * Description:
 */
data class RecentFriendMsg(
    // 会话对象部分
    @ColumnInfo(name = "id")
    val address: String,
    val nickname: String?,
    val avatar: String?,
    val remark: String?,
    val teamName: String?,
    val servers: MutableList<Server>?,
    val flag: Int,
    // 会话部分
    val channelType: Int,
    val unread: Int,
    val beAt: Boolean,
    @JvmField
    val draft: Draft?,
    // 消息部分
    @Embedded(prefix = "msg_")
    val message: RecentLog,
) : Serializable, RecentContactMsg {

    override fun getId(): String {
        return address
    }

    override fun getDisplayName(): String {
        return (remark ?: "").ifEmpty {
            (teamName ?: "").ifEmpty {
                (nickname ?: "").ifEmpty { address.toDisplay() }
            }
        }
    }

    override fun getScopeName(): String {
        return (teamName ?: "").ifEmpty {
            (nickname ?: "").ifEmpty { address.toDisplay() }
        }
    }

    override fun getRawName(): String {
        return (nickname ?: "").ifEmpty { address.toDisplay() }
    }

    override fun getDisplayImage(): String {
        return avatar ?: ""
    }

    override fun getType(): Int {
        return channelType
    }

    override fun getServerList(): List<Server> {
        return servers ?: emptyList()
    }

    override fun getExtra(): Bundle? {
        return null
    }

    override fun isStickTop(): Boolean {
        return flag and Contact.STICK_TOP == Contact.STICK_TOP
    }

    override fun isNoDisturb(): Boolean {
        return flag and Contact.NO_DISTURB == Contact.NO_DISTURB
    }

    override fun unreadNum(): Int {
        return unread
    }

    override fun beAtMsg(): Boolean {
        return beAt
    }

    override fun getDraft(): Draft? {
        return draft
    }

    override fun getContent(): String {
        if (message == RecentLog.EMPTY_LOG) {
            return ""
        }
        return message.getContent()
    }

    override fun getTime(): Long {
        return if (message.datetime == 0L) {
            draft?.time ?: 0L
        } else {
            message.datetime
        }
    }

    override fun getPriority(): Long {
        return draft?.time ?: message.datetime
    }

    override fun isDeleted(): Boolean {
        return flag == 0
    }

    override fun getFlags(): Int {
        return flag
    }

    override fun getRecentLog(): RecentLog {
        return message
    }
}