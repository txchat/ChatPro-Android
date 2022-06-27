package com.fzm.chat.core.repo

import android.util.LruCache
import com.fzm.chat.core.crypto.encrypt
import com.fzm.chat.core.data.*
import com.fzm.chat.core.data.bean.*
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.core.data.po.GroupUserPO
import com.fzm.chat.core.net.source.GroupDataSource
import com.zjy.architecture.data.Result
import com.zjy.architecture.data.UNKNOWN_ERROR
import com.zjy.architecture.exception.ApiException

/**
 * @author zhengjy
 * @since 2021/05/13
 * Description:
 */
class GroupRepository(
    private val dataSource: GroupDataSource,
) : GroupDataSource by dataSource {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val serverCache: LruCache<Long, String> = LruCache(40)

    /**
     * 从本地数据库获取群组的服务器地址
     */
    private suspend fun getGroupServer(url: String?, gid: Long): String? {
        var server = url?.ifEmpty { serverCache[gid] }
        if (server.isNullOrEmpty()) {
            server = database.groupDao().getGroupInfo(gid)?.server?.address
            serverCache.put(gid, server ?: "")
        }
        return server
    }

    override suspend fun createGroup(
        url: String,
        name: String,
        users: List<String>
    ): Result<GroupInfoTO> {
        val result = dataSource.createGroup(url, name, users)
        if (result.isSucceed()) {
            val info = result.data().toGroupInfo(Server("", "", url), Contact.RELATION)
            database.groupDao().insert(info)
        }
        return result
    }

    override suspend fun editGroupAvatar(url: String?, gid: Long, avatar: String): Result<Any> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.editGroupAvatar(server, gid, avatar)
        if (result.isSucceed()) {
            database.groupDao().editGroupAvatar(gid, avatar)
        }
        return result
    }

    override suspend fun editGroupName(url: String?, gid: Long, name: String): Result<Any> {
        val info = database.groupDao().getGroupInfo(gid)
        val server = info?.server?.address
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.editGroupName(server, gid, name.encrypt(info.key))
        if (result.isSucceed()) {
            database.groupDao().editGroupName(gid, name)
        }
        return result
    }

    override suspend fun editGroupNames(url: String?, gid: Long, name: String, publicName: String): Result<Any> {
        val info = database.groupDao().getGroupInfo(gid)
        val server = info?.server?.address
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.editGroupNames(server, gid, name.encrypt(info.key), publicName)
        if (result.isSucceed()) {
            database.groupDao().editGroupNames(gid, name, publicName)
        }
        return result
    }

    override suspend fun editNicknameInGroup(url: String?, gid: Long, name: String): Result<Any> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.editNicknameInGroup(server, gid, name)
        if (result.isSucceed()) {
            database.groupDao().getGroupInfo(gid)?.also {
                it.person?.nickname = name
                database.groupDao().insert(it)
            }
            database.groupUserDao().editNickname(gid, AppPreference.ADDRESS, name)
        }
        return result
    }

    override suspend fun removeMembers(
        url: String?,
        gid: Long,
        members: List<String>
    ): Result<StringList> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.removeMembers(server, gid, members)
        if (result.isSucceed()) {
            result.data().strings.forEach {
                database.groupUserDao().disableGroupUsers(gid, it)
            }
        }
        return result
    }

    /**
     * 获取群成员信息时，将群成员存入数据库
     */
    override suspend fun getGroupUser(
        url: String?,
        gid: Long,
        address: String
    ): Result<GroupUserPO> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.getGroupUser(server, gid, address)
        if (result.isSucceed()) {
            database.groupUserDao().insert(result.data())
        } else {
            val error = result.error()
            if (error is ApiException && (error.code == NotInGroup || error.code == UserNotInGroup)) {
                database.groupUserDao().disableGroupUsers(gid, address)
            }
        }
        return result
    }

    override suspend fun getGroupUserList(url: String?, gid: Long): Result<List<GroupUserPO>> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.getGroupUserList(server, gid)
        if (result.isSucceed()) {
            database.groupUserDao().insert(result.data())
        }
        return result
    }

    /**
     * 获取群信息时，和本地群信息组合起来
     */
    suspend fun getGroupInfo(url: String?, gid: Long, saveFlag: Int): Result<GroupInfo> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.getGroupInfo(server, gid)
        return if (result.isSucceed()) {
            val to = result.data()
            val groupInfo = to.toGroupInfo(Server("", "", server), 0)
            val local = database.groupDao().getGroupInfo(gid)
            if (local != null) {
                groupInfo.server = local.server
                groupInfo.flag = local.flag or saveFlag or groupInfo.flag
            } else if (saveFlag != 0) {
                groupInfo.flag = saveFlag or groupInfo.flag
            }
            database.groupDao().insert(groupInfo)
            Result.Success(groupInfo)
        } else {
            (result.error() as ApiException?)?.apply {
                // -10015：你已不在本群中
                // -10002：该群已解散
                if (code == -10015 || code == -10002) {
                    database.groupDao().deleteFlag(gid, Contact.RELATION)
                }
            }
            Result.Error(result.error())
        }
    }

    override suspend fun getGroupInfo(url: String?, gid: Long): Result<GroupInfoTO> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.getGroupInfo(server, gid)
        if (result.isSucceed()) {
            val to = result.data()
            val groupInfo = to.toGroupInfo(Server("", "", server), 0)
            val local = database.groupDao().getGroupInfo(gid)
            if (local != null) {
                groupInfo.server = local.server
                groupInfo.flag = local.flag or groupInfo.flag
            }
            database.groupDao().insert(groupInfo)
        } else {
            (result.error() as ApiException?)?.apply {
                // -10015：你已不在本群中
                // -10002：该群已解散
                if (code == -10015 || code == -10002) {
                    database.groupDao().deleteFlag(gid, Contact.RELATION)
                }
            }
        }
        return result
    }


    override suspend fun getGroupPubInfo(
        url: String?,
        gid: Long
    ): Result<GroupInfoTO> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        return dataSource.getGroupPubInfo(server, gid)
    }

    override suspend fun joinGroup(url: String?, gid: Long, inviterId: String?): Result<Any> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        return dataSource.joinGroup(url, gid, inviterId)
    }

    /**
     * 获取群信息列表时，和本地群信息组合起来
     */
    override suspend fun getGroupList(url: String): Result<GroupInfoTO.Wrapper> {
        val result = dataSource.getGroupList(url)
        if (result.isSucceed()) {
            val processed = mutableListOf<GroupInfo>()
            val list = result.data().groups
            list.forEach {
                val local = database.groupDao().getGroupInfo(it.gid)
                if (local != null) {
                    processed.add(it.toGroupInfo(Server("", "", url), local.flag))
                } else {
                    processed.add(it.toGroupInfo(Server("", "", url), Contact.RELATION))
                }
            }
            database.groupDao().insert(processed)
        }
        return result
    }

    override suspend fun changeGroupOwner(url: String?, gid: Long, address: String): Result<Any> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.changeGroupOwner(server, gid, address)
        if (result.isSucceed()) {
            database.groupUserDao().changeGroupUserRole(gid, address, GroupUser.LEVEL_OWNER)
        }
        return result
    }

    override suspend fun changeGroupUserRole(
        url: String?,
        gid: Long,
        address: String,
        role: Int
    ): Result<Any> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.changeGroupUserRole(server, gid, address, role)
        if (result.isSucceed()) {
            database.groupUserDao().changeGroupUserRole(gid, address, role)
        }
        return result
    }

    override suspend fun changeFriendType(url: String?, gid: Long, friendType: Int): Result<Any> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.changeFriendType(server, gid, friendType)
        if (result.isSucceed()) {
            database.groupDao().changeFriendType(gid, friendType)
        }
        return result
    }

    override suspend fun changeJoinType(url: String?, gid: Long, joinType: Int): Result<Any> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.changeJoinType(server, gid, joinType)
        if (result.isSucceed()) {
            database.groupDao().changeJoinType(gid, joinType)
        }
        return result
    }

    override suspend fun changeMuteType(url: String?, gid: Long, muteType: Int): Result<Any> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.changeMuteType(server, gid, muteType)
        if (result.isSucceed()) {
            database.groupDao().changeMuteType(gid, muteType)
        }
        return result
    }

    override suspend fun changeMuteTime(
        url: String?,
        gid: Long,
        muteTime: Long,
        members: List<String>
    ): Result<GroupUserTO.Wrapper> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.changeMuteTime(server, gid, muteTime, members)
        if (result.isSucceed()) {
            result.data().members.forEach {
                database.groupUserDao().changeMuteTime(gid, it.address, it.muteTime)
            }
        }
        return result
    }

    override suspend fun exitGroup(url: String?, gid: Long): Result<Any> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.exitGroup(server, gid)
        if (result.isSucceed()) {
            database.recentSessionDao().deleteSession(gid.toString(), ChatConst.GROUP_CHANNEL)
            database.groupDao().deleteGroup(gid)
            database.groupUserDao().deleteGroupUsersByGroup(gid)
        }
        return result
    }

    override suspend fun disbandGroup(url: String?, gid: Long): Result<Any> {
        val server = getGroupServer(url, gid)
        if (server.isNullOrEmpty()) {
            return Result.Error(ApiException(UNKNOWN_ERROR))
        }
        val result = dataSource.disbandGroup(server, gid)
        if (result.isSucceed()) {
            database.recentSessionDao().deleteSession(gid.toString(), ChatConst.GROUP_CHANNEL)
            database.groupDao().deleteFlag(gid, Contact.RELATION)
            database.groupUserDao().deleteGroupUsersByGroup(gid)
        }
        return result
    }
}