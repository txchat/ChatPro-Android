package com.fzm.chat.core.session.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.ChatQuery
import com.fzm.chat.core.data.bean.Field
import com.fzm.chat.core.data.bean.Server
import com.fzm.chat.core.data.bean.ServerGroupInfo
import com.fzm.chat.core.logic.ServerManager
import com.fzm.chat.core.repo.BackupRepository
import com.fzm.chat.core.session.UserDataSource
import com.fzm.chat.core.session.UserInfo
import com.fzm.chat.router.oa.CompanyUser
import com.zjy.architecture.data.Result
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author zhengjy
 * @since 2020/12/04
 * Description:
 */
class UserRepository(
    private val dataSource: UserDataSource,
    private val repository: BackupRepository
) : UserDataSource by dataSource {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    /**
     * 是否已经设置过server
     */
    private var hasServer: AtomicBoolean = AtomicBoolean(false)
    private var hasPubKey: AtomicBoolean = AtomicBoolean(false)

    private val serverLock by lazy { Mutex() }
    private val keyLock by lazy { Mutex() }

    override suspend fun getUserInfo(address: String, publicKey: String, query: ChatQuery): Result<UserInfo> {
        val result = dataSource.getUserInfo(address, publicKey, query)
        if (result.isSucceed()) {
            val data = result.data()
            val builder = result.data().newBuilder()
            setupFirstUpload(data, builder, publicKey)
            val backup = repository.fetchBackupByAddress()
            val modified: UserInfo
            if (backup.isSucceed()) {
                // 从中心化备份服务获取相关的绑定信息
                builder.setPhone(backup.data().phone)
                builder.setEmail(backup.data().email)
                modified = builder.build()
                database.userInfoDao().insert(modified)
            } else {
                modified = builder.build()
                modified.apply {
                    database.userInfoDao().insertWithoutPhone(address, nickname, avatar, publicKey, servers, searchKey, chainAddress)
                }
            }
            return Result.Success(modified)
        } else {
            return result
        }
    }

    /**
     * 处理首次上传用户信息逻辑
     */
    private suspend fun setupFirstUpload(data: UserInfo, builder: UserInfo.Builder, publicKey: String) {
        if (data.servers.isEmpty() && ServerManager.defaultChatUrl.isNotEmpty() && !hasServer.get()) {
            try {
                serverLock.lock()
                // 再次判断是否有服务器
                if (data.servers.isEmpty() && ServerManager.defaultChatUrl.isNotEmpty() && !hasServer.get()) {
                    // 如果没有保存服务器地址，则上传默认地址
                    val default = ServerManager.defaultChatUrl
                    val addDefault = modifyServerGroup(listOf(ServerGroupInfo("", 1, "default", default)))
                    if (addDefault.isSucceed()) {
                        hasServer.set(true)
                        // 第一个分组id，默认直接填写"1"（可能会一些问题）
                        builder.setServers(mutableListOf(Server("1", "default", default)))
                    }
                }
            } finally {
                if (serverLock.isLocked) {
                    serverLock.unlock()
                }
            }
        }
        if (data.publicKey.isEmpty() && !hasPubKey.get()) {
            try {
                keyLock.lock()
                if (data.publicKey.isEmpty() && !hasPubKey.get()) {
                    // 上传用户公钥
                    val pubResult = setUserInfo(listOf(Field(ChatConst.UserField.PUB_KEY, publicKey, 1)))
                    builder.setPublicKey(publicKey)
                    if (pubResult.isSucceed()) {
                        hasPubKey.set(true)
                    }
                }
            } finally {
                if (keyLock.isLocked) {
                    keyLock.unlock()
                }
            }
        }
    }

    override suspend fun logout(address: String) {
        hasServer.set(false)
        hasPubKey.set(false)
    }

    fun getUserInfoLive(address: String): LiveData<UserInfo> {
        return database.userInfoDao().getUserInfo(address).map {
            // UserInfo可能为空，因此这里判空是必要的
            it ?: UserInfo.Builder(address).build()
        }
    }

    fun getCompanyUserLive(address: String): LiveData<CompanyUser> {
        return database.companyUserDao().getCompanyUserInfo(address).map { bo ->
            if (bo == null) return@map CompanyUser.EMPTY_COMPANY_USER
            return@map CompanyUser(
                bo.depId, bo.depName, bo.entId, bo.entName, bo.id, bo.leaderId,
                bo.name, bo.phone, bo.shortPhone, bo.email, bo.position, bo.role,
                bo.workplace, bo.joinTime, bo.isActivated
            ).also { it.company = bo.company }
        }
    }

    suspend fun updateUserInfo(info: UserInfo?) {
        info?.apply { database.userInfoDao().insert(this) }
    }
}