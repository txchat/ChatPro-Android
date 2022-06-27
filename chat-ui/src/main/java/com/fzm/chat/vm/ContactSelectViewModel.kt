package com.fzm.chat.vm

import androidx.lifecycle.*
import com.fzm.chat.bean.model.ForwardContact
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.comparator.PinyinComparator
import com.fzm.chat.core.data.comparator.RecentMsgComparator
import com.fzm.chat.core.data.model.RecentContactMsg
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.toLikeKey
import com.zjy.architecture.mvvm.LoadingViewModel

/**
 * @author zhengjy
 * @since 2021/06/18
 * Description:
 */
abstract class ContactSelectViewModel(delegate: LoginDelegate) : LoadingViewModel(), LoginDelegate by delegate {

    companion object {
        const val PRIVATE = 1
        const val GROUP = 1 shl 1
        const val SESSION = 1 shl 2
    }

    private val pinyinComparator by lazy { PinyinComparator() }
    private val sessionComparator by lazy { RecentMsgComparator() }

    /**
     * 默认显示会话列表，且会话列表中的群聊会话和私聊会话都显示
     */
    protected open val channelFilter: Int = SESSION or PRIVATE or GROUP

    protected val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val searchMode = MutableLiveData<Boolean>().apply { value = false }

    val contact: LiveData<List<ForwardContact>> = searchMode.switchMap { inSearch ->
        when {
            inSearch -> {
                searchResult(keywords).map {
                    it.map { contact ->
                        ForwardContact(
                            if (contact.getType() == ChatConst.PRIVATE_CHANNEL) ForwardContact.FRIEND else ForwardContact.GROUP,
                            contact
                        )
                    }
                }
            }
            channelFilter and SESSION != 0 -> {
                sessionList().map {
                    it.map { session ->
                        ForwardContact(ForwardContact.RECENT_SESSION, session)
                    }
                }
            }
            else -> contactList()
        }
    }

    /**
     * 搜索关键词
     */
    var keywords: String = ""
        private set

    private fun sessionList() = liveData {
        val session = mutableListOf<RecentContactMsg>()
        if (channelFilter and PRIVATE != 0) {
            session += database.recentSessionDao().getPrivateSessionList()
        }
        if (channelFilter and GROUP != 0) {
            session += database.recentSessionDao().getGroupSessionList()
        }
        emit(session.sortedWith(sessionComparator))
    }

    private fun contactList() = liveData<List<ForwardContact>> {
        val contacts = mutableListOf<ForwardContact>()
        if (channelFilter and PRIVATE != 0) {
            contacts += database.friendUserDao().getFriendList()
                .sortedWith(pinyinComparator)
                .map { ForwardContact(ForwardContact.FRIEND, it) }
        }
        if (channelFilter and GROUP != 0) {
            contacts += database.groupDao().getGroupList(Contact.RELATION)
                .sortedWith(pinyinComparator)
                .map { ForwardContact(ForwardContact.GROUP, it) }
        }
        emit(contacts)
    }

    private fun searchResult(keywords: String) = liveData {
        val friends = if (channelFilter and PRIVATE != 0) {
            database.friendUserDao().searchUsers(keywords.toLikeKey(), Contact.RELATION)
        } else emptyList()
        val groups = if (channelFilter and GROUP != 0) {
            database.groupDao().searchGroups(keywords.toLikeKey(), Contact.RELATION)
        } else emptyList()
        emit(friends.sortedWith(pinyinComparator) + groups.sortedWith(pinyinComparator))
    }

    fun searchByKeywords(keywords: String?) {
        this.keywords = keywords ?: ""
        triggerQuery(!keywords.isNullOrEmpty())
    }

    private fun triggerQuery(search: Boolean) {
        searchMode.value = search
    }
}