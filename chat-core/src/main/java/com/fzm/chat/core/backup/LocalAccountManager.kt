package com.fzm.chat.core.backup

import android.content.Context
import androidx.annotation.IntDef
import com.fzm.chat.core.backup.LocalAccountType.Companion.BACKUP_EMAIL
import com.fzm.chat.core.backup.LocalAccountType.Companion.BACKUP_MNEMONIC
import com.fzm.chat.core.backup.LocalAccountType.Companion.BACKUP_PHONE
import com.fzm.chat.core.data.po.LocalAccount
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.CoreDatabase
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.po.BackupKeypair
import com.fzm.chat.core.data.po.FriendUserPO
import com.fzm.chat.core.repo.ContractRepository
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.session.UserInfo
import com.fzm.chat.core.utils.CipherUtils
import com.fzm.chat.core.utils.formatMnemonic
import com.zjy.architecture.data.Result
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.ext.bytes2Hex
import com.zjy.architecture.ext.hex2Bytes
import com.zjy.architecture.ext.sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import walletapi.Walletapi
import kotlin.jvm.Throws

@IntDef(BACKUP_PHONE, BACKUP_EMAIL, BACKUP_MNEMONIC)
@Retention(AnnotationRetention.SOURCE)
annotation class LocalAccountType {
    companion object {
        const val BACKUP_PHONE = 1
        const val BACKUP_EMAIL = 2
        const val BACKUP_MNEMONIC = 3
    }
}

/**
 * @author zhengjy
 * @since 2021/11/29
 * Description:从本地账户管理
 */
class LocalAccountManager(
    private val context: Context,
    private val coreDatabase: CoreDatabase
) {

    private val repository by rootScope.inject<ContractRepository>()
    private val delegate by rootScope.inject<LoginDelegate>()

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    /**
     * 将用户信息存储为本地账户信息
     */
    suspend fun store(info: UserInfo) {
        val local = coreDatabase.localAccountDao().getAccountByAddress(info.address)
        val updated by lazy(LazyThreadSafetyMode.NONE) {
            LocalAccount(
                info.address,
                info.address.sha256(),
                info.publicKey,
                info.avatar,
                info.nickname,
                info.phone,
                info.email
            )
        }
        if (local == null) {
            coreDatabase.localAccountDao().insert(updated)
        } else if (local.avatar != info.avatar ||
            local.nickname != info.nickname ||
            local.phone != info.phone ||
            local.email != info.email ||
            local.publicKey != info.publicKey
        ) {
            coreDatabase.localAccountDao().update(updated.apply {
                id = local.id
                encryptMnemonic = local.encryptMnemonic
                encryptKeyId = local.encryptKeyId
            })
        }
    }

    /**
     * 使用本地生成的公私钥对，额外备份一份助记词（用于忘记密码时自动恢复）
     */
    suspend fun backupMnemonic(address: String, privateKey: String, mnemonic: String): Boolean {
        val keypair = generateBackupKeypair() ?: return false
        val encrypted = CipherUtils.encrypt(
            mnemonic.toByteArray(),
            keypair.publicKey,
            privateKey
        )
        val local = coreDatabase.localAccountDao().getAccountByAddress(address)
        if (local != null) {
            coreDatabase.localAccountDao().insert(
                LocalAccount(
                    local.address,
                    local.addressHash,
                    local.publicKey,
                    local.avatar,
                    local.nickname,
                    local.phone,
                    local.email
                ).apply {
                    id = local.id
                    encryptMnemonic = encrypted.bytes2Hex()
                    encryptKeyId = keypair.kid
                }
            )
            return true
        }
        return false
    }

    /**
     * 尝试恢复助记词
     */
    suspend fun tryRecoverMnemonic(account: String, @LocalAccountType type: Int): String? {
        val local = findLocalAccount(account, type) ?: return null
        val keypair = coreDatabase.backupKeypair().getKeypairById(local.encryptKeyId) ?: return null
        return local.encryptMnemonic?.let {
            String(CipherUtils.decrypt(it.hex2Bytes(), local.publicKey, keypair.privateKey))
        }
    }

    /**
     * 是否包含指定信息的本地账户
     */
    suspend fun hasLocalAccount(account: String, @LocalAccountType type: Int): Boolean {
        return findLocalAccount(account, type) != null
    }

    /**
     * 获取本地所有账户信息(隐藏地址信息)
     */
    suspend fun getLocalAccountList(predicate: ((LocalAccount) -> Boolean)? = null): List<LocalAccount> {
        val result = coreDatabase.localAccountDao().getAllAccounts() ?: emptyList()
        return result
            .run {
                if (predicate != null) filter(predicate) else this
            }
    }

    suspend fun findLocalAccount(account: String, @LocalAccountType type: Int): LocalAccount? {
        return when (type) {
            BACKUP_PHONE -> coreDatabase.localAccountDao().getAccountByPhone(account)
            BACKUP_EMAIL -> coreDatabase.localAccountDao().getAccountByEmail(account)
            BACKUP_MNEMONIC -> try {
                getAddressFromMnemonic(account)
            } catch (e: Exception) {
                return null
            }
            else -> return null
        }
    }

    /**
     * 导入另一个账户的数据
     */
    suspend fun importAccount(account: String, @LocalAccountType type: Int, addressHash: String? = null): Result<Unit> {
        val local = when (type) {
            BACKUP_PHONE -> coreDatabase.localAccountDao().getAccountByPhone(account)
            BACKUP_EMAIL -> coreDatabase.localAccountDao().getAccountByEmail(account)
            BACKUP_MNEMONIC -> try {
                getAddressFromMnemonic(account)
            } catch (e: Exception) {
                return Result.Error(e)
            }
            else -> return Result.Error(Exception("账户类型错误"))
        }
        if (local == null || (addressHash != null && addressHash != local.addressHash)) {
            return if (type == BACKUP_MNEMONIC) {
                Result.Error(Exception("输入助记词与账户不匹配"))
            } else {
                Result.Error(Exception("帐号已换绑，请尝试其他方式验证"))
            }
        }

        return importByOldAddress(local.address)
    }

    suspend fun importByOldAddress(address: String): Result<Unit> {
        val old = ChatDatabase.build(context, address)

        // 导入好友信息
        importFriends(old).apply {
            if (!isSucceed()) return Result.Error(error())
        }

        return Result.Success(Unit)
    }

    /**
     * 导入另一个账户的好友
     */
    private suspend fun importFriends(old: ChatDatabase): Result<Unit> {
        val friends = old.friendUserDao().getAllUsersSuspend()
        database.friendUserDao().insert(friends)
        val friendResult = repository.addFriendsSkipDatabase(
            friends.filter { it.isFriend && !it.isBlock && it.address != delegate.getAddress() }.map { it.address },
            emptyList()
        )
        if (!friendResult.isSucceed()) {
            return Result.Error(friendResult.error())
        }
        val blockResult = repository.blockUserSkipDatabase(
            friends.filter { it.isBlock }.map { it.address }
        )
        if (!blockResult.isSucceed()) {
            return Result.Error(blockResult.error())
        }
        return Result.Success(Unit)
    }

    /**
     * 通过助记词获取本地登录过的账户
     */
    @Throws(Exception::class)
    private suspend fun getAddressFromMnemonic(mnemonic: String): LocalAccount? {
        val mnemWithSpace = mnemonic.formatMnemonic()
        val hdWallet = withContext(Dispatchers.IO) {
            CipherUtils.getHDWallet(Walletapi.TypeBtyString, mnemWithSpace)
        } ?: throw Exception("助记词错误")
        val address = CipherUtils.pubToAddress(hdWallet.newKeyPub(0))
        return coreDatabase.localAccountDao().getAccountByAddress(address)
    }

    /**
     * 生成随机公私钥对
     */
    private suspend fun generateBackupKeypair(): BackupKeypair? {
        val mnemonic = CipherUtils.createMnemonicString(1, 160).formatMnemonic()
        val hdWallet = withContext(Dispatchers.IO) {
            CipherUtils.getHDWallet(Walletapi.TypeBtyString, mnemonic)
        } ?: return null
        val pub = hdWallet.newKeyPub(0).bytes2Hex()
        val pri = hdWallet.newKeyPriv(0).bytes2Hex()
        coreDatabase.backupKeypair().insert(BackupKeypair(0L, pub, pri))
        return coreDatabase.backupKeypair().getKeypairByPub(pub)
    }

    private val FriendUserPO.isFriend: Boolean
        get() = flag and Contact.RELATION == Contact.RELATION

    private val FriendUserPO.isBlock: Boolean
        get() = flag and Contact.BLOCK == Contact.BLOCK
}