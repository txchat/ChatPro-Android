package com.fzm.chat.core.net.source

import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.data.po.GroupInfo

/**
 * @author zhengjy
 * @since 2019/09/17
 * Description:
 */
interface SearchDataSource {

    suspend fun searchFriends(keywords: String): List<FriendUser>

    suspend fun searchFriendsWithBlocked(keywords: String): List<FriendUser>

    suspend fun searchGroups(keywords: String): List<GroupInfo>

    suspend fun searchChatLogs(keywords: String): List<ChatMessage>

    suspend fun searchChatLogsByTarget(keywords: String, target: ChatTarget): List<ChatMessage>

    @Deprecated("暂时不需要同时搜索和所有人的聊天文件")
    suspend fun searchChatFiles(keywords: String): List<ChatMessage>

    suspend fun searchChatFilesByTarget(keywords: String, target: ChatTarget): List<ChatMessage>

}