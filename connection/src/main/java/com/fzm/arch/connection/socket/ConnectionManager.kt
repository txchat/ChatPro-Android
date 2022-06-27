package com.fzm.arch.connection.socket

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.fzm.arch.connection.Disposable
import com.fzm.arch.connection.OnMessageConfirmCallback
import com.fzm.arch.connection.utils.urlKey
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.util.ActivityUtils
import okhttp3.HttpUrl
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named

/**
 * @author zhengjy
 * @since 2020/12/07
 * Description:
 */
class ConnectionManager(
    serverList: LiveData<List<String>>,
    private val callbacks: List<OnMessageConfirmCallback>
) : Disposable, ActivityUtils.OnAppStateChangedListener {

    companion object {
        private const val WEB_SOCKET = "WebSocket"
        private const val TCP_SOCKET = "TcpSocket"
        private const val CHAT_SOCKET = WEB_SOCKET
    }

    private val socketUrls = mutableListOf<String>()
    private val connectionPool: HashMap<String, ChatSocket> = HashMap()

    private val listeners = mutableListOf<ChatSocketListener>()
    private val observers: HashMap<ChatSocket, Observer<Int>> = HashMap()

    private var _singleState: MutableLiveData<Pair<String, Int>>? = null
    private val singleState: MutableLiveData<Pair<String, Int>>
        get() = _singleState ?: MutableLiveData<Pair<String, Int>>().apply { _singleState = this }

    private var _totalState: MutableLiveData<Boolean>? = null
    private val totalState: MutableLiveData<Boolean>
        get() = _totalState ?: MutableLiveData<Boolean>().apply { _totalState = this }

    private var lastLeaveTime = 0L

    init {
        serverList.observeForever { servers ->
            ActivityUtils.addOnAppStateChangedListener(this)
            if (servers.isNotEmpty()) {
                socketUrls.clear()
                socketUrls.addAll(servers.map {
                    Uri.parse(it).buildUpon().path("/sub/").build().toString()
                }.distinct())
                if (listeners.isNotEmpty()) {
                    // 在MessageDispatcher没有注册前，不连接服务器
                    refreshServers()
                }
            }
        }
    }

    override fun onResumeApp() {
        if (System.currentTimeMillis() - lastLeaveTime > 60 * 1000) {
            // 处于后台1分钟后返回会检查重连状态，保证回到前台第一时间能重建连接（1分钟之内连接基本不会断）
            // 如果不设置延时，则有错误地址服务器时会导致首页服务器错误的提示反复出现
            connectionPool.values.forEach { it.onForeground(true) }
        }
    }

    override fun onLeaveApp() {
        lastLeaveTime = System.currentTimeMillis()
    }

    fun register(listener: ChatSocketListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
        connectionPool.values.forEach { it.register(listener) }
    }

    fun unregister(listener: ChatSocketListener) {
        listeners.remove(listener)
        connectionPool.values.forEach { it.unregister(listener) }
    }

    /**
     * 监听独立某个服务器的连接状态，只能收到最近一次某个服务器的状态变化，不能用来监听具体某个服务器的状态
     */
    fun observeSocketState(lifecycleOwner: LifecycleOwner, observer: Observer<Pair<String, Int>>) {
        singleState.observe(lifecycleOwner, observer)
    }

    /**
     * 监听整体的连接状态
     */
    fun observeState(lifecycleOwner: LifecycleOwner, observer: Observer<Boolean>) {
        totalState.observe(lifecycleOwner, observer)
    }

    fun connectAll() {
        socketUrls.forEach { connect(it) }
    }

    private fun connect(url: String) {
        val conn = connectionPool[url.urlKey()]
        if (conn != null) {
            if (!conn.isAlive) {
                conn.connect()
            }
        } else {
            generateNewSocket(url)?.also { connectionPool[url.urlKey()] = it }
        }
    }

    fun disconnectAll() {
        connectionPool.entries.forEach {
            clearStateObserver(it.key, it.value)
            it.value.release()
        }
        connectionPool.clear()
    }

    @Deprecated("调用refreshServers不需要手动断开某个服务器连接")
    fun disconnect(url: String) {
        connectionPool[url.urlKey()]?.apply {
            clearStateObserver(url, this)
            release()
        }
        connectionPool.remove(url.urlKey())
    }

    fun send(url: String, message: ByteArray, extra: Bundle?): String? {
        val socket = connectionPool[url.urlKey()]
        if (socket != null) {
            return socket.send(message, extra)
        }
        return null
    }

    fun getChatSocket(url: String) = connectionPool[url.urlKey()]

    /**
     * 是否全部服务器都正常连接
     */
    fun isConnected(): Boolean {
        for ((_, socket) in connectionPool) {
            if (!socket.isAlive) return false
        }
        return true
    }

    /**
     * 服务器列表更新后，移除不需要的连接，同时创建新的连接
     */
    internal fun refreshServers() {
        val tempMap = mutableMapOf<String, ChatSocket>()
        for (url in socketUrls) {
            val conn = connectionPool.remove(url.urlKey())
            if (conn != null) {
                tempMap[url.urlKey()] = conn
            } else {
                val uri = HttpUrl.parse(url) ?: continue
                if (uri.scheme() != "https" && uri.scheme() != "http") {
                    continue
                }
                if (uri.port() > 65535 || uri.port() < 1) {
                    continue
                }
                generateNewSocket(url)?.also { tempMap[url.urlKey()] = it }
            }
        }
        disconnectAll()
        connectionPool.putAll(tempMap)
        totalState.value = isConnected()
    }

    private fun generateNewSocket(url: String): ChatSocket? {
        return try {
            val socket = rootScope.get<ChatSocket>(named(CHAT_SOCKET)) { parametersOf(url) }
            val wrapper = DelegateSocket(socket, callbacks)
            listeners.forEach { wrapper.register(it) }
            setupStateObserver(url, wrapper)
            wrapper.apply { connect() }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 设置服务器连接变化监听
     */
    private fun setupStateObserver(url: String, socket: ChatSocket) {
        val observer = Observer<Int> {
            singleState.value = url.urlKey() to it
            totalState.value = isConnected()
        }
        socket.state.observeForever(observer)
        observers[socket] = observer
    }

    /**
     * 清除服务器连接变化监听
     */
    private fun clearStateObserver(url: String, socket: ChatSocket) {
        observers.remove(socket)?.also {
            // 当删除服务器时，也发出通知（防止因为删除服务器后会话仍旧不变灰）
            singleState.value = url.urlKey() to 0
            socket.state.removeObserver(it)
        }
    }

    override fun dispose() {
        ActivityUtils.removeOnAppStateChangedListener(this)
        disconnectAll()
        socketUrls.clear()
        listeners.clear()
        _singleState = null
        _totalState = null
    }
}