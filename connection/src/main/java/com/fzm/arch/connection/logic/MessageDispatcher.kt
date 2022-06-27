package com.fzm.arch.connection.logic

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import com.fzm.arch.connection.Disposable
import com.fzm.arch.connection.exception.CipherException
import com.fzm.arch.connection.processor.Processor
import com.fzm.arch.connection.protocol.ProtocolReader
import com.fzm.arch.connection.protocol.ProtocolWriter
import com.fzm.arch.connection.socket.ChatSocketListener
import com.fzm.arch.connection.socket.ConnectionManager
import com.fzm.arch.connection.utils.Utils
import com.fzm.arch.connection.utils.toProto
import com.fzm.chat.router.core.CoreModule
import com.fzm.chat.router.core.CoreService
import com.fzm.chat.router.route
import com.zjy.architecture.exception.ApiException
import com.zjy.architecture.ext.tryWith
import com.zjy.architecture.util.logE
import com.zjy.architecture.util.logW
import dtalk.proto.Api
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * @author zhengjy
 * @since 2020/12/04
 * Description:
 */
class MessageDispatcher(
    private val isLogin: () -> Boolean,
    private val manager: ConnectionManager,
    private val eventProcessors: List<Processor<Api.Proto>>
) : ChatSocketListener, Disposable, ProtocolReader.FrameCallback, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.IO

    private val coreService by route<CoreService>(CoreModule.SERVICE)

    private val handler = Handler(Looper.getMainLooper())
    private var authMap = ConcurrentHashMap<String, Runnable>()

    private var pingFuture = ConcurrentHashMap<String, Future<*>>()
    private lateinit var executor: ScheduledThreadPoolExecutor
    private val pingIntervalMillis: Long = 60_000L

    private val writer: ProtocolWriter = ProtocolWriter(manager)
    private val reader: ProtocolReader = ProtocolReader(this)

    private var sentPingCount = ConcurrentHashMap<String, Int>()
    private var awaitingPong = ConcurrentHashMap<String, Boolean>()
    private var firstConnectFlag = ConcurrentHashMap<String, Boolean>()

    companion object {
        const val AUTH_DELAY = 5000L
    }

    fun loop() {
        if (!this::executor.isInitialized || executor.isShutdown) {
            executor = ScheduledThreadPoolExecutor(1, Utils.threadFactory("MessageDispatcher-Ping"))
        }
        manager.register(this)
        manager.connectAll()
    }

    override fun onMessage(url: String, msg: ByteArray) {
        if (!isLogin()) {
            return
        }

        val proto = try {
            msg.toProto()
        } catch (e: Exception) {
            logE("decode error, $e")
            return
        }
        try {
            reader.processFrame(url, proto)
        } catch (e: Exception) {
            logE("frame error, $e")
        }
    }

    override fun onOpen(url: String) {
        if (!isLogin()) {
            return
        }
        val firstConnect = firstConnectFlag[url] ?: true
        writer.auth(url, firstConnect)
        postRetryAuth(url, firstConnect)
        pingFuture[url] = executor.scheduleAtFixedRate(PingRunnable(url), pingIntervalMillis, pingIntervalMillis, TimeUnit.MILLISECONDS)
    }

    /**
     * 延迟一段时间后重试Auth
     */
    private fun postRetryAuth(url: String, firstConnect: Boolean) {
        authMap[url] = object : Runnable {
            override fun run() {
                writer.auth(url, firstConnect)
                handler.postDelayed(this, AUTH_DELAY)
            }
        }
        authMap[url]?.also { handler.postDelayed(it, AUTH_DELAY) }
    }

    override fun onClose(url: String, e: ApiException) {
        authMap[url]?.also { handler.removeCallbacks(it) }
        authMap.remove(url)
        // 关闭指定的ping任务
        pingFuture[url]?.apply {
            cancel(false)
            pingFuture.remove(url)
        }
    }

    override fun dispose() {
        firstConnectFlag.clear()
        authMap.forEach { handler.removeCallbacks(it.value) }
        authMap.clear()
        if (this::executor.isInitialized) {
            // 关闭所有ping任务
            executor.shutdown()
        }
        pingFuture.forEach { it.value.cancel(false) }
        pingFuture.clear()

        coroutineContext.cancel()
        manager.dispose()
        manager.unregister(this)
    }

    private fun writePingFrame(url: String) {
        val failedPing = if (awaitingPong[url] == true) sentPingCount[url] ?: 1 else -1
        val count = sentPingCount[url]
        if (count == null) {
            sentPingCount[url] = 1
        } else {
            sentPingCount[url] = count + 1
        }
        awaitingPong[url] = true
        if (failedPing != -1) {
            logW("sent heart beat but didn't receive reply (after " + (failedPing - 1) + " successful ping/pongs)")
        }
        writer.writePing(url)
    }

    inner class PingRunnable(private val url: String) : Runnable {
        override fun run() {
            writePingFrame(url)
        }
    }

    override fun onReadAuthReply(url: String, payload: ByteArray) {
        authMap[url]?.also { handler.removeCallbacks(it) }
        authMap.remove(url)
        if (payload.isEmpty()) {
            firstConnectFlag[url] = false
        } else {
            coreService?.parseAuthReply(payload)
        }
    }

    override fun onReadHeartBeatReply(url: String) {
        awaitingPong[url] = false
    }

    override fun onReadAck(url: String, seq: Int, data: ByteArray) {
        manager.getChatSocket(url)?.ack(seq, data)
    }

    override fun onReadEvent(url: String, message: Api.Proto, seq: Int) {
        launch {
            var msg: Api.Proto? = message
            var processed = false
            for (processor in eventProcessors) {
                if (msg == null) {
                    break
                } else {
                    msg = try {
                        processed = true
                        processor.process(url.getServerUrl(), msg)
                    } catch (e: CipherException) {
                        processed = false
                        logE("decrypt error, ${e.message}")
                        null
                    } catch (e: Exception) {
                        logE("process error, $e")
                        processed = true
                        null
                    }
                }
            }
            if (processed) writer.writeAck(url, seq)
        }
    }

    private val cacheMap = LruCache<String, String>(16)

    private fun String.getServerUrl(): String {
        val cache = cacheMap[this]
        if (cache != null) return cache
        val uri = tryWith { Uri.parse(this).buildUpon().path("").build() }
        return uri?.toString().also { cacheMap.put(this, it) } ?: this
    }
}