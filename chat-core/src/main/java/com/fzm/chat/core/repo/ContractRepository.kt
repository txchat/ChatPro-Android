package com.fzm.chat.core.repo

import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.*
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.net.source.ContractDataSource
import com.fzm.chat.core.net.source.TransactionSource
import com.fzm.chat.core.session.LoginDelegate
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2020/12/22
 * Description:
 */
class ContractRepository(
    private val dataSource: ContractDataSource,
    private val transaction: TransactionSource,
    delegate: LoginDelegate
) : LoginDelegate by delegate {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    suspend fun getFriendList(): List<FriendUser> {
        return getRelativeUser(Contact.RELATION)
    }

    suspend fun getBlockList(): List<FriendUser> {
        return getRelativeUser(Contact.BLOCK)
    }

    suspend fun getServerGroup(): List<ServerGroupInfo> {
        val query = ChatQuery.serverGroupQuery(preference.ADDRESS, "", preference.getKeyPair())
        return dataSource.getServerGroup(query).dataOrNull()?.groups?.also { list ->
            updateInfo {
                servers.clear()
                servers.addAll(list.map { Server(it.id, it.name, it.value) }.toMutableList())
            }
        } ?: listOf()
    }

    suspend fun getFriendUser(address: String, saveFlag: Int = 0, save: Boolean = false, remark: String = ""): Result<FriendUser> {
        val result = dataSource.getUser(ChatQuery.userQuery(preference.ADDRESS, address, "", preference.getKeyPair()))
        return if (result.isSucceed()) {
            val data = result.data()
            val builder = FriendUser.Builder(address)
                .setServers(data.chatServers.toMutableList())
                .setGroups(data.groups.toMutableList())
            val map = mutableMapOf<String, String>()
            data.fields.forEach {
                when {
                    it.name == ChatConst.UserField.NICKNAME -> builder.setNickname(it.value)
                    it.name == ChatConst.UserField.AVATAR -> builder.setAvatar(it.value)
                    it.name == ChatConst.UserField.PUB_KEY -> builder.setPublicKey(it.value)
                    it.name.startsWith(ChatConst.UserField.CHAIN_PREFIX) -> {
                        map[it.name.substring(ChatConst.UserField.CHAIN_PREFIX.length)] = it.value
                    }
                }
            }
            builder.setChainAddress(map)
            val friendUser = builder.build()
            val local = database.friendUserDao().getFriendUser(address)
            if (local != null) {
                // 本地数据库包含这个用户，则更新信息
                friendUser.flag = local.flag or saveFlag
                friendUser.remark = remark.ifEmpty { local.remark }
                database.friendUserDao().insert(friendUser)
            } else if (saveFlag != 0) {
                // 本地数据库不包含这个用户，且saveFlag不为0，则插入数据库
                friendUser.flag = saveFlag
                friendUser.remark = remark
                database.friendUserDao().insert(friendUser)
            } else if (save) {
                friendUser.remark = remark
                database.friendUserDao().insert(friendUser)
            }
            Result.Success(friendUser)
        } else {
            Result.Error(result.error())
        }
    }

    private suspend fun getRelativeUser(flag: Int): List<FriendUser> {
        val mainAddress = preference.ADDRESS
        val friendAddress = getRelativeUserAddress(mainAddress, "", flag).map { it.friendAddress }
        val friends = arrayListOf<FriendUser>()
        for (address in friendAddress) {
            getFriendUser(address, flag).dataOrNull()?.also { friends.add(it) }
        }
        return friends
    }

    private suspend fun getRelativeUserAddress(address: String?, index: String?, flag: Int): List<UserAddress> {
        val count = ChatConfig.PAGE_SIZE
        val result = if (flag and Contact.BLOCK == Contact.BLOCK) {
            dataSource.getBlockList(ChatQuery.blockQuery(address, index, preference.getKeyPair()))
        } else {
            dataSource.getFriendList(ChatQuery.friendsQuery(address, index, preference.getKeyPair()))
        }
        return if (result.isSucceed()) {
            if (result.data().friends.isNullOrEmpty()) {
                emptyList()
            } else {
                val list = arrayListOf<UserAddress>()
                list.addAll(result.data().friends)
                if (result.data().friends.size == count) {
                    list.addAll(getRelativeUserAddress(address, result.data().friends.last().friendAddress, flag))
                }
                return list
            }
        } else {
            emptyList()
        }
    }

    suspend fun addFriends(address: List<String?>, groups: List<String>, remark: String = ""): Result<String> {
        val result = transaction.handle { dataSource.modifyFriend(address, 1, groups) }
        if (result.isSucceed()) {
            address.forEach {
                if (it != null) {
                    getFriendUser(it, Contact.RELATION, remark = remark)
                }
            }
        }
        return result
    }

    suspend fun addFriendsSkipDatabase(address: List<String?>, groups: List<String>): Result<String> {
        return transaction.handle { dataSource.modifyFriend(address, 1, groups) }
    }

    suspend fun deleteFriends(address: List<String?>): Result<String> {
        val result = transaction.handle { dataSource.modifyFriend(address, 2, emptyList()) }
        if (result.isSucceed()) {
            address.forEach {
                if (it != null) {
                    database.friendUserDao().deleteFlag(it, Contact.RELATION)
                    database.recentSessionDao().deleteSession(it, ChatConst.PRIVATE_CHANNEL)
                    database.messageDao().deleteContactMessage(it, ChatConst.PRIVATE_CHANNEL)
                }
            }
        }
        return result
    }

    suspend fun blockUser(address: List<String?>): Result<String> {
        val result = transaction.handle { dataSource.modifyBlock(address, 1) }
        if (result.isSucceed()) {
            address.forEach {
                if (it != null) {
                    database.recentSessionDao().deleteSession(it, ChatConst.PRIVATE_CHANNEL)
                    getFriendUser(it, Contact.BLOCK)
                }
            }
        }
        return result
    }

    suspend fun blockUserSkipDatabase(address: List<String?>): Result<String> {
        return transaction.handle { dataSource.modifyBlock(address, 1) }
    }

    suspend fun unBlockUser(address: List<String?>): Result<String> {
        val result = transaction.handle { dataSource.modifyBlock(address, 2) }
        if (result.isSucceed()) {
            address.forEach {
                if (it != null) {
                    database.friendUserDao().deleteFlag(it, Contact.BLOCK)
                }
            }
        }
        return result
    }

    suspend fun modifyServerGroup(groupInfo: List<ServerGroupInfo>): Result<String> {
        return transaction.handle { dataSource.modifyServerGroup(groupInfo) }
    }
}