package com.fzm.chat.biz.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.fzm.chat.biz.bean.ModuleState
import com.fzm.chat.biz.data.repo.BusinessRepository
import com.fzm.chat.core.logic.ServerManager
import com.zjy.architecture.di.rootScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Serializable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author zhengjy
 * @since 2021/08/17
 * Description:
 */
object FunctionModules {

    private val MODULES = mapOf(
        "wallet"    to ModuleConfig(0x00000001, { it.firstOrNull()?.also { url -> ServerManager.changeWalletServer(url) } }),
        "redPacket" to ModuleConfig(0x00000002, { it.firstOrNull()?.also { url -> ServerManager.changeWalletServer(url) } }),
        "oa"        to ModuleConfig(0x00000004, { it.firstOrNull()?.also { url -> ServerManager.changeOAServer(url) } }),
        "live"      to ModuleConfig(0x00000008),
        "shop"      to ModuleConfig(0x00000010, { it.firstOrNull()?.also { url -> ServerManager.changeShopServer(url) } })
    )
    private val repository by rootScope.inject<BusinessRepository>()

    private val initLock = Mutex(true)

    private val initFlag = AtomicBoolean(false)

    /**
     * 模块配置是否已经初始化
     */
    val hasInit: Boolean
        get() = initFlag.get()

    /**
     * 模块启用标记
     *
     * 00000001    钱包模块
     */
    internal var moduleFlags: Int = 0

    internal val moduleFlagLiveData = MutableLiveData(0)

    suspend fun <T> awaitInit(block: suspend () -> T): T {
        // 防止第一次模块请求失败，导致的死锁
        repository.fetchModuleState().dataOrNull()?.also { parseModulesState(it) }
        return initLock.withLock {
            block()
        }
    }

    fun parseModulesState(list: List<ModuleState>?) {
        list?.forEach {
            val config = MODULES[it.name]
            if (config != null) {
                if (it.isEnabled) {
                    // 模块未启用则启用
                    if (moduleFlags and config.flag == 0) {
                        moduleFlags = moduleFlags or config.flag
                        config.action?.invoke(it.endPoints)
                        moduleFlagLiveData.value = moduleFlags
                    }
                } else {
                    // 模块已启用则禁用
                    if (moduleFlags and config.flag != 0) {
                        moduleFlags = moduleFlags and config.flag.inv()
                        config.disableAction?.invoke()
                        moduleFlagLiveData.value = moduleFlags
                    }
                }
            }
        }
        if (initFlag.compareAndSet(false, true) && initLock.isLocked) {
            try {
                initLock.unlock()
            } catch (e: IllegalStateException) {
                // ignore
            }
        }
    }

    private data class ModuleConfig(
        /**
         * 模块标志位
         */
        val flag: Int,
        /**
         * 模块启用的操作
         */
        val action: ((endPoints: List<String>) -> Unit)? = null,
        /**
         * 模块关闭的操作
         */
        val disableAction: (() -> Unit)? = null
    ) : Serializable
}

val FunctionModules.enableWallet: Boolean
    get() = (moduleFlags and 0x00000001) != 0

val FunctionModules.enableWalletLive: LiveData<Boolean>
    get() = moduleFlagLiveData.map { (it and 0x00000001) != 0 }.distinctUntilChanged()

val FunctionModules.enableRedPacket: Boolean
    get() = (moduleFlags and 0x00000002) != 0

val FunctionModules.enableRedPacketLive: LiveData<Boolean>
    get() = moduleFlagLiveData.map { (it and 0x00000002) != 0 }.distinctUntilChanged()

val FunctionModules.enableOA: Boolean
    get() = (moduleFlags and 0x00000004) != 0

val FunctionModules.enableOALive: LiveData<Boolean>
    get() = moduleFlagLiveData.map { (it and 0x00000004) != 0 }.distinctUntilChanged()

val FunctionModules.enableLive: Boolean
    get() = (moduleFlags and 0x00000008) != 0

val FunctionModules.enableLiveLive: LiveData<Boolean>
    get() = moduleFlagLiveData.map { (it and 0x00000008) != 0 }.distinctUntilChanged()

val FunctionModules.enableShop: Boolean
    get() = (moduleFlags and 0x00000010) != 0

val FunctionModules.enableShopLive: LiveData<Boolean>
    get() = moduleFlagLiveData.map { (it and 0x00000010) != 0 }.distinctUntilChanged()