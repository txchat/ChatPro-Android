package com.fzm.chat.contact

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.repo.ContractRepository
import com.fzm.chat.core.utils.toLikeKey
import com.zjy.architecture.data.Result
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request

/**
 * @author zhengjy
 * @since 2021/11/19
 * Description:
 */
class ContactListViewModel(
    private val repository: ContractRepository,
) : LoadingViewModel() {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val _keywords by lazy(LazyThreadSafetyMode.NONE) {
        MutableLiveData<String>().apply { value = null }
    }
    val keywords: LiveData<String>
        get() = _keywords

    val friendList: LiveData<List<FriendUser>> = _keywords.switchMap {
        database.friendUserDao().searchUsersLive(it?.toLikeKey(), Contact.RELATION)
    }

    val blockList: LiveData<List<FriendUser>> = _keywords.switchMap {
        database.friendUserDao().searchUsersLive(it?.toLikeKey(), Contact.BLOCK)
    }

    fun getFriendList() {
        request<Unit> {
            onRequest {
                repository.getFriendList()
                Result.Success(Unit)
            }
        }
    }

    fun getBlockList() {
        request<Unit> {
            onRequest {
                repository.getBlockList()
                Result.Success(Unit)
            }
        }
    }

    fun setSearchKey(keyword: String) {
        _keywords.value = keyword
    }
}