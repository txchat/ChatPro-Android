package com.fzm.chat.core.data.local

import androidx.collection.LruCache
import com.fzm.chat.core.data.*
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.core.repo.ContractRepository
import com.fzm.chat.core.repo.GroupRepository
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.AddressValidationUtils
import com.zjy.architecture.exception.ApiException
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex

/**
 * @author zhengjy
 * @since 2020/12/31
 * Description:
 */
@ObsoleteCoroutinesApi
class ContactManager(
    private val delegate: LoginDelegate,
    private val repository: ContractRepository,
    private val groupRepo: GroupRepository,
    private val memory: MemoryContactSource
) {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val mutex = Mutex()
    private val userRequest = mutableMapOf<String, BroadcastChannel<FriendUser>>()

    private val gMutex = Mutex()
    private val groupUserRequest = mutableMapOf<String, BroadcastChannel<GroupUser>>()

    private val iMutex = Mutex()
    private val groupInfoRequest = mutableMapOf<String, BroadcastChannel<GroupInfo>>()

    suspend fun getUserInfo(address: String, save: Boolean = false): Contact {
        if (!AddressValidationUtils.bitCoinAddressValidate(address) || !delegate.isLogin()) {
            return FriendUser.empty(address)
        }
        if (address == delegate.getAddress()) {
            val info = delegate.current.value!!
            return FriendUser(
                info.address,
                info.nickname,
                info.avatar,
                info.publicKey,
                0,
                info.servers,
                mutableListOf(),
                info.chainAddress,
                teamName = delegate.companyUser.value?.name
            )
        }

        try {
            // 加锁防止重复请求
            mutex.lock()
            val local = memory.userMap[address]
            if (local != null && local.publicKey.isNotEmpty()) {
                // 内存中存在直接返回
                return local
            }
            val channel = userRequest[address]
            if (channel != null) {
                // 如果已经有请求在进行中，则等待结果
                return channel.openSubscription().receiveCatching().getOrNull()
                    ?: FriendUser.empty(address)
            }
            val request = BroadcastChannel<FriendUser>(Channel.BUFFERED)
            userRequest[address] = request
            mutex.safeUnlock()
            // 从接口获取用户信息
            val result = repository.getFriendUser(address, save = save).dataOrNull()
            val user = result ?: FriendUser.empty(address)
            request.send(user)

            userRequest.remove(address)
            request.close()
            return user
        } finally {
            mutex.safeUnlock()
        }
    }

    /**
     * 群成员信息缓存
     */
    private val groupUserMap: LruCache<String, GroupUser> = LruCache(40)

    /**
     * 群信息缓存
     */
    private val groupInfoMap: LruCache<String, GroupInfo> = LruCache(40)

    /**
     * 群服务器地址缓存
     */
    private val groups: MutableMap<String, String> = mutableMapOf()

    suspend fun getGroupUserInfo(gid: String, address: String): Contact {
        try {
            if (!AddressValidationUtils.bitCoinAddressValidate(address) || !delegate.isLogin()) {
                return GroupUser.empty(gid, address)
            }
            // 加锁防止重复请求
            gMutex.lock()
            // 获取群所在的服务器地址
            var url = groups[gid]
            if (url == null) {
                val group = database.groupDao().getGroupInfo(gid.toLong())
                url = group?.server?.address ?: ""
                groups[gid] = url
            }
//            val local = groupUserMap[groupUserKey(gid, address)]
//            if (local != null) {
//                // 内存中存在直接返回
//                return local
//            }
            val user = database.friendUserDao().getFriendUser(address)
            if (user == null) {
                // 如果本地用户表没有这个群成员，则更新信息
                getUserInfo(address, true)
            }
            val localDb = database.groupUserDao().getGroupUser(gid.toLong(), address)
            if (localDb != null) {
                groupUserMap.put(groupUserKey(gid, address), localDb)
                return localDb
            }
            val channel = groupUserRequest[groupUserKey(gid, address)]
            if (channel != null) {
                // 如果已经有请求在进行中，则等待结果
                return channel.openSubscription().receiveCatching().getOrNull()
                    ?: GroupUser.empty(gid, address)
            }
            val request = BroadcastChannel<GroupUser>(Channel.BUFFERED)
            groupUserRequest[groupUserKey(gid, address)] = request
            gMutex.safeUnlock()
            // 从接口获取用户信息
            val user1 = groupRepo.getGroupUser(url, gid.toLong(), address).dataOrNull()
            val user2 = getUserInfo(address, true) as FriendUser
            val groupUser = GroupUser(
                gid,
                address,
                user1?.role ?: 0,
                user1?.flag ?: 0,
                user1?.nickname ?: "",
                user1?.muteTime ?: 0L,
                user2.nickname,
                user2.avatar,
                user2.remark,
                null
            )
            groupUserMap.put(groupUserKey(gid, address), groupUser)
            request.send(groupUser)

            groupUserRequest.remove(groupUserKey(gid, address))
            request.close()
            return groupUser
        } finally {
            gMutex.safeUnlock()
        }
    }

    /**
     * 快速获取群成员信息，不走网络请求
     */
    suspend fun getGroupUserInfoFast(gid: String, address: String): GroupUser {
        if (!delegate.isLogin()) return GroupUser.empty(gid, address)
        // 获取群所在的服务器地址
        var url = groups[gid]
        if (url == null) {
            val group = database.groupDao().getGroupInfo(gid.toLong())
            url = group?.server?.address ?: ""
            groups[gid] = url
        }
//        val local = groupUserMap[groupUserKey(gid, address)]
//        if (local != null) {
//            // 内存中存在直接返回
//            return local
//        }
        val localDb = database.groupUserDao().getGroupUser(gid.toLong(), address)
        return localDb ?: GroupUser(gid, address, 0, 0, "", 0L, "", "", null, null)
    }

    private inline fun groupUserKey(gid: String, address: String) = "$gid-$address"

    suspend fun getGroupInfo(gid: String, server: String? = null): GroupInfo {
        try {
            if (!delegate.isLogin()) return GroupInfo.empty(gid.toLong())
            // 加锁防止重复请求
            iMutex.lock()
            // 获取群所在的服务器地址
            var url = groups[gid] ?: server
            if (url == null) {
                val group = database.groupDao().getGroupInfo(gid.toLong())
                url = group?.server?.address ?: ""
                groups[gid] = url
            }
//            val local = groupInfoMap[gid]
//            if (local != null) {
//                // 内存中存在直接返回
//                return local
//            }
            val localDb = database.groupDao().getGroupInfo(gid.toLong())
            if (localDb != null) {
                groupInfoMap.put(gid, localDb)
                return localDb
            }
            val channel = groupInfoRequest[gid]
            if (channel != null) {
                // 如果已经有请求在进行中，则等待结果
                return channel.openSubscription().receiveCatching().getOrNull()
                    ?: GroupInfo.empty(gid.toLong())
            }
            val request = BroadcastChannel<GroupInfo>(Channel.BUFFERED)
            groupInfoRequest[gid] = request
            iMutex.safeUnlock()
            // 从接口获取群信息
            val result = groupRepo.getGroupInfo(url, gid.toLong(), 0)
            val groupInfo = if (result.isSucceed()) {
                result.data()
            } else {
                val error = result.error()
                if (error is ApiException && (error.code == NotInGroup || error.code == GroupIsDisband)) {
                    GroupInfo.empty(gid.toLong(), ChatConst.INVALID_AES_KEY)
                } else {
                    GroupInfo.empty(gid.toLong())
                }
            }
            groupInfoMap.put(gid, groupInfo)
            request.send(groupInfo)

            request.close()
            groupInfoRequest.remove(gid)
            return groupInfo
        } finally {
            iMutex.safeUnlock()
        }
    }

    fun clearGroupCache(gid: Long) {
        groupInfoMap.remove(gid.toString())
    }

    fun clearGroupUserCache(gid: Long, address: String) {
        groupUserMap.remove(groupUserKey(gid.toString(), address))
    }

    private fun Mutex.safeUnlock(owner: Any? = null) {
        if (isLocked) {
            try {
                unlock(owner)
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}