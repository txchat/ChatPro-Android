package com.fzm.chat.core.data.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.distinctUntilChanged
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.data.po.isBlock
import com.fzm.chat.core.data.po.isFriend
import com.fzm.chat.core.session.LoginDelegate
import java.util.concurrent.ConcurrentHashMap

/**
 * @author zhengjy
 * @since 2020/12/31
 * Description:本地联系人数据
 */
class MemoryContactSource(delegate: LoginDelegate) {

    private var allUsers: LiveData<List<FriendUser>>? = null

    internal val _userMap = ConcurrentHashMap<String, FriendUser>()
    val userMap: Map<String, FriendUser>
        get() = _userMap

    private val _friendMap = ConcurrentHashMap<String, FriendUser>()
    val friendMap: Map<String, FriendUser>
        get() = _friendMap

    private val _blockMap = ConcurrentHashMap<String, FriendUser>()
    val blockMap: Map<String, FriendUser>
        get() = _blockMap

    private val observer = Observer<List<FriendUser>> { users ->
        _userMap.clear()
        _friendMap.clear()
        _blockMap.clear()
        users.forEach {
            _userMap[it.address] = it
            if (it.isBlock) {
                _blockMap[it.address] = it
            } else if (it.isFriend) {
                _friendMap[it.address] = it
            }
        }
    }

    init {
        delegate.current.observeForever {
            if (it.isLogin()) {
                allUsers = ChatDatabaseProvider.provide()
                    .friendUserDao()
                    .getAllUsers()
                    .distinctUntilChanged()
                allUsers?.observeForever(observer)
            } else {
                allUsers?.removeObserver(observer)
                allUsers = null
                _userMap.clear()
                _friendMap.clear()
                _blockMap.clear()
            }
        }
    }
}