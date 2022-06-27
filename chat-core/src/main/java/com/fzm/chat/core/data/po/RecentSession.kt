package com.fzm.chat.core.data.po

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fzm.chat.core.data.model.Draft
import com.fzm.chat.core.data.model.RecentLog
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/28
 * Description:
 */
@Entity(tableName = "recent_session")
data class RecentSession(
    /**
     * 会话id
     */
    @PrimaryKey
    val id: String,
    /**
     * 会话类型
     */
    val channelType: Int,
    /**
     * 未读消息数
     */
    var unread: Int,
    /**
     * 有人@你
     */
    var beAt: Boolean,
    /**
     * 被人@的消息id
     */
    var atMessages: List<String>,
    /**
     * 会话草稿
     */
    var draft: Draft?,
    /**
     * 最近一条消息
     */
    @Embedded(prefix = "msg_")
    var message: RecentLog,
) : Serializable