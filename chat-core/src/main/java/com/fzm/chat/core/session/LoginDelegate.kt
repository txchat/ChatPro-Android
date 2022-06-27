package com.fzm.chat.core.session

import androidx.lifecycle.*
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.arch.connection.logic.MessageDispatcher
import com.fzm.chat.core.backup.LocalAccountManager
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.data.bean.ChatQuery
import com.fzm.chat.core.data.bean.Field
import com.fzm.chat.core.data.bean.Server
import com.fzm.chat.core.logic.MessageSender
import com.fzm.chat.core.media.DownloadManager2
import com.fzm.chat.core.rtc.calling.RTCCalling
import com.fzm.chat.core.rtc.data.isOnCalling
import com.fzm.chat.core.rtc.data.isWaiting
import com.fzm.chat.core.session.data.UserPreference
import com.fzm.chat.core.session.data.UserRepository
import com.fzm.chat.core.utils.CipherUtils
import com.fzm.chat.router.core.CoreModule
import com.fzm.chat.router.core.CoreService
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.main.MainService
import com.fzm.chat.router.oa.CompanyUser
import com.fzm.chat.router.oa.OAModule
import com.fzm.chat.router.oa.OAService
import com.fzm.chat.router.oa.hasCompany
import com.fzm.chat.router.route
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.router.wallet.WalletService
import com.zjy.architecture.data.Result
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.util.logE
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlin.coroutines.CoroutineContext

/**
 * @author zhengjy
 * @since 2020/08/06
 * Description:
 */
interface LoginDelegate {

    /**
     * 当前登录用户信息
     */
    val current: LiveData<UserInfo>

    /**
     * 当前登录用户企业信息
     */
    val companyUser: LiveData<CompanyUser>

    /**
     * 当用户信息或企业用户信息任意一项发生变化时会发出通知
     */
    val userInfoChanged: LiveData<Int>

    /**
     * 获取用户连接的所有服务器(包含企业服务器)
     */
    val servers: LiveData<List<Server>>

    /**
     * 当前用户信息本地存储信息
     */
    val preference: UserPreference

    /**
     * 登录事件
     */
    val loginEvent: LiveData<Boolean>

    /**
     * 登录成功之后设置公私钥对和地址，获取用户信息
     *
     * @param mnemonic  助记词，中文助记词需要用空格隔开
     */
    fun performLogin(publicKey: String, privateKey: String, mnemonic: String, password: String = "")

    suspend fun loginSuspend(publicKey: String, privateKey: String, mnemonic: String, password: String = "")

    /**
     * 注销操作
     */
    fun performLogout()

    suspend fun logoutSuspend()

    /**
     * 从服务端更新用户信息
     */
    fun updateInfo()

    /**
     * 从服务端更新企业和用户信息
     */
    fun updateCompanyInfo()

    /**
     * 手动更新本地用户信息
     */
    fun updateInfo(block: UserInfo.() -> Unit)

    /**
     * 修改用户信息
     */
    suspend fun setUserInfo(fields: List<Field>): Result<String>

    /**
     * 获取用户地址
     */
    fun getAddress(): String?

    /**
     * 是否已经登录
     */
    fun isLogin(): Boolean

    /**
     * 是否是企业用户（或关联了企业帐号）
     */
    fun hasCompany(): Boolean

    /**
     * 是否是自动登录
     */
    fun isAutoLogin(): Boolean
}

class LoginDelegateImpl(
    private val repository: UserRepository,
    private val manager: LocalAccountManager
) : LoginDelegate, CoroutineScope {

    private val coreService by route<CoreService>(CoreModule.SERVICE)
    private val mainService by route<MainService>(MainModule.SERVICE)
    private val walletService by route<WalletService>(WalletModule.SERVICE)
    private val oaService by route<OAService>(OAModule.SERVICE)

    private val dispatcher by rootScope.inject<MessageDispatcher>()
    private val messageManager by rootScope.inject<MessageSender>()
    private val rtcCall by rootScope.inject<RTCCalling>()

    private var autoLogin = true

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Main.immediate

    override val current: LiveData<UserInfo> = liveData {
        emitSource(AppPreference.address.switchMap { address ->
            if (address.isNotEmpty()) {
                repository.getUserInfoLive(address)
            } else {
                liveData { emit(UserInfo.EMPTY_USER) }
            }
        }.distinctUntilChanged())
        if (AppPreference.ADDRESS.isNotEmpty()) {
            // 如果一开始就登录了，则更新用户信息
            repository.getUserInfo(AppPreference.ADDRESS, preference.PUB_KEY, chatQuery)
            coreService?.checkService()
            _loginEvent.value = true
        }
    }

    override val companyUser: LiveData<CompanyUser> = liveData {
        emitSource(AppPreference.address.switchMap { address ->
            if (address.isNotEmpty()) {
                repository.getCompanyUserLive(address)
            } else {
                liveData { emit(CompanyUser.EMPTY_COMPANY_USER) }
            }
        }.distinctUntilChanged())
        if (AppPreference.ADDRESS.isNotEmpty()) {
            // 查自己的企业用户信息时不需要传地址
            oaService?.getCompanyUser(null)
        }
    }

    override val userInfoChanged: LiveData<Int> = liveData {
        launch {
            current.asFlow().collect { emit(1) }
        }
        launch {
            companyUser.asFlow().collect { emit(1) }
        }.join()
        // join是为了挂起这个协程，防止因为作用域结束，而导致不能发送事件
    }

    override val servers: LiveData<List<Server>> = userInfoChanged.switchMap {
        if (hasCompany()) {
            companyUser.map {
                val userServers = current.value?.servers ?: emptyList()
                if (it.company?.imServer.isNullOrEmpty()) {
                    userServers
                } else {
                    userServers.toMutableList().apply {
                        add(0, Server("company", it.company!!.name, it.company!!.imServer))
                    }
                }
            }
        } else {
            current.map { it.servers }
        }
    }

    override val preference: UserPreference
        get() = UserPreference.getInstance()

    override val loginEvent: LiveData<Boolean>
        get() = _loginEvent

    private val _loginEvent = MutableLiveData<Boolean>()

    private val chatQuery: ChatQuery
        get() = ChatQuery.userQuery(
            preference.ADDRESS,
            preference.ADDRESS,
            "",
            preference.getKeyPair()
        )

    private val changeLocalAccountObserver = Observer<UserInfo> {
        if (it.isLogin()) launch { manager.store(it) }
    }

    init {
        current.observeForever(changeLocalAccountObserver)
    }

    override fun performLogin(publicKey: String, privateKey: String, mnemonic: String, password: String) {
        launch { loginSuspend(publicKey, privateKey, mnemonic, password) }
    }

    override suspend fun loginSuspend(publicKey: String, privateKey: String, mnemonic: String, password: String) {
        autoLogin = false
        val address = CipherUtils.pubToAddress(publicKey)
        UserPreference.init(address)
        AppPreference.ADDRESS = address
        withContext(Dispatchers.IO) {
            preference.saveDHKeyPairAndAddress(publicKey, privateKey, address)
            manager.backupMnemonic(address, privateKey, mnemonic)
            if (password.isEmpty()) {
                preference.saveMnemonicStringWithDefaultPwd(mnemonic)
            } else {
                preference.saveMnemonicString(mnemonic, password)
            }
        }
        oaService?.getCompanyUser(null)
        try {
            walletService?.importMnem(address, mnemonic, address, password)
        } catch (e: Exception) {
            logE("钱包创建失败，${e.message}")
        }
        repository.getUserInfo(address, preference.PUB_KEY, chatQuery)
        mainService?.addInnateDynamicShortcuts()
        _loginEvent.value = true
    }

    override fun performLogout() {
        launch { logoutSuspend() }
    }

    override suspend fun logoutSuspend() {
        if (rtcCall.getCurrentTask().isOnCalling) {
            rtcCall.hangup().join()
        } else if (rtcCall.getCurrentTask().isWaiting) {
            rtcCall.reject().join()
        }
        walletService?.closeWallet()
        DownloadManager2.dispose()
        messageManager.dispose()
        dispatcher.dispose()
        val address = AppPreference.ADDRESS
        AppPreference.ADDRESS = ""
        repository.logout(address)
        mainService?.removeAllDynamicShortcuts()
        _loginEvent.value = false
    }

    override fun updateInfo() {
        launch {
            repository.getUserInfo(preference.ADDRESS, preference.PUB_KEY, chatQuery)
        }
    }

    override fun updateCompanyInfo() {
        launch {
            oaService?.getCompanyUser(null)
        }
    }

    override fun updateInfo(block: UserInfo.() -> Unit) {
        launch {
            val info = current.value?.newBuilder()?.build()?.apply(block)
            repository.updateUserInfo(info)
        }
    }

    override suspend fun setUserInfo(fields: List<Field>): Result<String> {
        return repository.setUserInfo(fields)
    }

    override fun getAddress(): String? {
        return current.value?.address
    }

    override fun isLogin(): Boolean {
        return current.value?.isLogin() ?: false
    }

    override fun hasCompany(): Boolean {
        return companyUser.value.hasCompany
    }

    override fun isAutoLogin(): Boolean {
        return autoLogin
    }
}

fun LoginDelegate.checkLogin(redirect: (() -> Unit)? = null, action: () -> Unit) {
    if (isLogin()) {
        action()
    } else if (redirect != null) {
        redirect()
    }
}

fun LoginDelegate.checkLogin(router: String? = null, action: () -> Unit) {
    if (isLogin()) {
        action()
    } else if (router != null) {
        ARouter.getInstance().build(router).navigation()
    }
}
