package com.fzm.arch.connection.socket

import android.os.Bundle
import androidx.core.os.bundleOf
import com.fzm.arch.connection.OnMessageConfirmCallback
import com.fzm.arch.connection.protocol.Protocols
import com.fzm.arch.connection.type.ConfirmType
import com.zjy.architecture.util.logE
import com.zjy.architecture.util.logV
import java.io.Serializable
import java.util.concurrent.DelayQueue
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit

/**
 * @author zhengjy
 * @since 2021/03/11
 * Description:
 */
class DelegateSocket(
    private val socket: ChatSocket,
    private val callbacks: List<OnMessageConfirmCallback>
) : ChatSocket by socket {

    companion object {
        /**
         * 消息发送最大重试次数
         */
        const val MAX_RETRY_TIMES = 4
    }

    private val waitingToAckQueue: DelayQueue<SendingMessage> = DelayQueue()
    private var worker: Thread? = null

    init {
        loop()
    }

    private fun loop() {
        if (worker == null) {
            worker = Thread({
                try {
                    while (!Thread.interrupted()) {
                        val msg = waitingToAckQueue.take()
                        msg.run()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    worker = null
                }
            }, "DelegateSocket")
            worker?.start()
        }
    }

    override fun onForeground(foreground: Boolean) {
        if (foreground) loop()
        socket.onForeground(foreground)
    }

    override fun send(message: ByteArray, extra: Bundle?): String? {
        if (extra == null) return null
        val requireAck = extra.getBoolean(Protocols.REQUIRE_ACK)
        var sending: SendingMessage? = null
        if (requireAck) {
            val msg = Message(System.currentTimeMillis(), message, extra)
            waitingToAckQueue.put(SendingMessage(msg).also { sending = it })
        }
        val identifier = socket.send(message, extra)
        if (identifier == null) {
            sending?.let { waitingToAckQueue.remove(it) }
            return null
        }
        extra.putString(Protocols.SEQ_IDENTIFIER, identifier)
        extra.putString(Protocols.RETRY_SEQ_IDENTIFIER, identifier)
        return identifier
    }

    override fun ack(seq: Int, data: ByteArray) {
        waitingToAckQueue.forEach {
            // 首次发送记录的identifier
            val identifier = it.message.extra?.getString(Protocols.SEQ_IDENTIFIER)
            // 重新连接webSocket后，新的identifier
            val retry = it.message.extra?.getString(Protocols.RETRY_SEQ_IDENTIFIER)
            if (retry == socketIdentifier(socket.toString(), seq)) {
                sendConfirm(identifier, bundleOf("data" to data))
                waitingToAckQueue.remove(it)
                return@forEach
            }
        }
    }

    private fun sendConfirm(identifier: String?, extra: Bundle?) {
        if (identifier == null) return
        callbacks.forEach {
            it.onMessageConfirm(identifier, ConfirmType.SUCCESS, extra)
        }
    }

    private fun sendFail(identifier: String?) {
        if (identifier == null) return
        callbacks.forEach {
            it.onMessageConfirm(identifier, ConfirmType.FAIL, null)
        }
    }

    private fun sendAbort(identifier: String?) {
        if (identifier == null) return
        callbacks.forEach {
            it.onMessageConfirm(identifier, ConfirmType.ABORT, null)
        }
    }

    private class Message(
        var time: Long,
        var data: ByteArray,
        var extra: Bundle?
    ) : Serializable

    private inner class SendingMessage(val message: Message) : Delayed, Runnable {

        var sendTimes = 1

        override fun run() {
            val identifier = message.extra?.getString(Protocols.SEQ_IDENTIFIER)
            when {
                sendTimes < MAX_RETRY_TIMES -> {
                    message.time = System.currentTimeMillis()
                    val result = socket.send(message.data, message.extra)
                    if (result != null) {
                        sendTimes++
                        waitingToAckQueue.put(this)
                        message.extra?.putString(Protocols.RETRY_SEQ_IDENTIFIER, result)
                        logV("DelegateSocket", "$result attempt to send $sendTimes times")
                    } else {
                        sendAbort(identifier)
                        logE("DelegateSocket", "socket was closed, use http channel")
                    }
                }
                sendTimes == MAX_RETRY_TIMES -> {
                    sendFail(identifier)
                    logE("DelegateSocket", "$identifier failed after trying to send $MAX_RETRY_TIMES times")
                }
            }
        }

        override fun compareTo(other: Delayed?): Int {
            if (other == null || other !is SendingMessage) return 1
            if (other === this) return 0
            return when {
                this.message.time > other.message.time -> 1
                this.message.time == other.message.time -> 0
                else -> -1
            }
        }

        override fun getDelay(unit: TimeUnit): Long {
            return unit.convert(
                message.time + 5_000L - System.currentTimeMillis(),
                TimeUnit.MILLISECONDS
            )
        }
    }
}