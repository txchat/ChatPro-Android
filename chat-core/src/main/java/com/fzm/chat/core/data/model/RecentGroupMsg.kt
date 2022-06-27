package com.fzm.chat.core.data.model

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.bean.Server
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/05/07
 * Description:
 */
data class RecentGroupMsg(
    // 会话对象部分
    @ColumnInfo(name = "id")
    val gid: String,
    val name: String?,
    val publicName: String?,
    val avatar: String?,
    val server: Server?,
    val flag: Int,
    val groupType: Int,
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

    override fun getId(): String {
        return gid
    }

    override fun getDisplayName(): String {
        return (name ?: "").ifEmpty { publicName ?: "" }
    }

    override fun getScopeName(): String {
        return (name ?: "").ifEmpty { publicName ?: "" }
    }

    override fun getRawName(): String {
        return publicName ?: ""
    }

    override fun getDisplayImage(): String {
        return avatar ?: ""
    }

    override fun getType(): Int {
        return ChatConst.GROUP_CHANNEL
    }

    override fun getServerList(): List<Server> {
        if (server == null) return emptyList()
        return listOf(server)
    }

    override fun getExtra(): Bundle {
        return bundleOf(Contact.GROUP_TYPE to groupType)
    }

    override fun getRecentLog(): RecentLog {
        return message
    }
}