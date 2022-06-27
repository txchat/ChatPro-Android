package com.fzm.chat.core.repo

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.bean.SearchResult
import com.fzm.chat.core.data.bean.SearchScope.Companion.ALL
import com.fzm.chat.core.data.bean.SearchScope.Companion.CHAT_LOG
import com.fzm.chat.core.data.bean.SearchScope.Companion.FRIEND
import com.fzm.chat.core.data.bean.SearchScope.Companion.GROUP
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.contact
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.core.net.source.SearchDataSource

/**
 * @author zhengjy
 * @since 2021/04/14
 * Description:
 */
class SearchRepository(
    private val contactManager: ContactManager,
    private val dataSource: SearchDataSource
) {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    suspend fun searchDataScoped(keywords: String, scope: Int): SearchResult.Wrapper {
        val searchResult = mutableListOf<SearchResult>()
        when (scope) {
            FRIEND -> searchFriends(keywords, searchResult)
            GROUP -> searchGroups(keywords, searchResult)
            CHAT_LOG -> searchChatLogs(keywords, searchResult)
            ALL -> {
                searchFriends(keywords, searchResult)
                searchGroups(keywords, searchResult)
                searchChatLogs(keywords, searchResult)
            }
        }
        return SearchResult.Wrapper(keywords, searchResult)
    }

    suspend fun searchChatLogs(keywords: String, target: ChatTarget): List<ChatMessage> {
        return dataSource.searchChatLogsByTarget(keywords, target)
    }

    private suspend fun searchFriends(keywords: String, searchResult: MutableList<SearchResult>) {
        val friends = dataSource.searchFriends(keywords)
        friends.forEach {
            val result = if (it.remark.isEmpty()) {
                SearchResult(FRIEND, keywords, it.address, it.getDisplayImage(), it.nickname.ifEmpty { it.address }, null, null)
            } else {
                SearchResult(FRIEND, keywords, it.address, it.getDisplayImage(), it.remark, it.nickname.ifEmpty { it.address }, null)
            }
            searchResult.add(result)
        }
    }

    private suspend fun searchGroups(keywords: String, searchResult: MutableList<SearchResult>) {
        val groups = dataSource.searchGroups(keywords)
        groups.forEach {
            searchResult.add(SearchResult(GROUP, keywords, it.getId(), it.getDisplayImage(), it.getDisplayName(), null, null))
        }
    }

    private suspend fun searchChatLogs(keywords: String, searchResult: MutableList<SearchResult>) {
        val chatLogs = dataSource.searchChatLogs(keywords)
        val groupCache = mutableMapOf<Long, GroupInfo>()
        chatLogs.groupBy {
            ChatTarget(it.channelType, it.contact)
        }.forEach {
            val target = it.key
            if (target.channelType == ChatConst.GROUP_CHANNEL) {
                val gid = target.targetId.toLong()
                var groupInfo = groupCache[gid]
                if (groupInfo == null) {
                    groupInfo = database.groupDao().getGroupInfo(gid)
                }
                if (groupInfo != null) {
                    groupCache[gid] = groupInfo
                    searchResult.add(
                        SearchResult(
                            CHAT_LOG, keywords, target.targetId,
                            groupInfo.getDisplayImage(), groupInfo.getDisplayName(), null, it.value
                        )
                    )
                }
            } else if (target.channelType == ChatConst.PRIVATE_CHANNEL) {
                val local = contactManager.getUserInfo(target.targetId)
                searchResult.add(SearchResult(CHAT_LOG, keywords, local.getId(),
                    local.getDisplayImage(), local.getDisplayName(), null, it.value))
            }
        }
    }
}