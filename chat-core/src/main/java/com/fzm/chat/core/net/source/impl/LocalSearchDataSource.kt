package com.fzm.chat.core.net.source.impl

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.core.net.source.SearchDataSource
import com.fzm.chat.core.utils.toLikeKey
import com.fzm.chat.core.utils.toMatchKey

/**
 * @author zhengjy
 * @since 2021/04/14
 * Description:本地搜索
 */
class LocalSearchDataSource : SearchDataSource {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    override suspend fun searchFriends(keywords: String): List<FriendUser> {
        return database.friendUserDao().searchUsers(keywords.toLikeKey() , Contact.RELATION)
    }

    override suspend fun searchFriendsWithBlocked(keywords: String): List<FriendUser> {
        return database.friendUserDao().searchUsers(keywords.toLikeKey(), Contact.RELATION or Contact.BLOCK)
    }

    override suspend fun searchGroups(keywords: String): List<GroupInfo> {
        return database.groupDao().searchGroups(keywords.toLikeKey(), Contact.RELATION)
    }

    override suspend fun searchChatLogs(keywords: String): List<ChatMessage> {
        return database.ftsSearchDao().searchChatLogs(keywords.toMatchKey())
    }

    override suspend fun searchChatLogsByTarget(keywords: String, target: ChatTarget): List<ChatMessage> {
        return if (target.channelType == ChatConst.PRIVATE_CHANNEL) {
            database.ftsSearchDao().searchFriendChatLogs(target.targetId, keywords.toMatchKey())
        } else {
            database.ftsSearchDao().searchGroupChatLogs(target.targetId, keywords.toMatchKey())
        }
    }

    override suspend fun searchChatFiles(keywords: String): List<ChatMessage> {
        return database.ftsSearchDao().searchChatFiles(keywords.toMatchKey())
    }

    override suspend fun searchChatFilesByTarget(keywords: String, target: ChatTarget): List<ChatMessage> {
        return if (target.channelType == ChatConst.PRIVATE_CHANNEL) {
            database.ftsSearchDao().searchFriendChatFiles(target.targetId, keywords.toMatchKey())
        } else {
            database.ftsSearchDao().searchGroupChatFiles(target.targetId, keywords.toMatchKey())
        }
    }
}