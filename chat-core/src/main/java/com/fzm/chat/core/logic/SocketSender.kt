package com.fzm.chat.core.logic

import android.os.*
import chat33.comet.Socket
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.contact
import com.fzm.arch.connection.Disposable
import com.fzm.chat.core.data.msg.MsgState
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.arch.connection.socket.ConnectionManager
import com.fzm.arch.connection.type.ConfirmType
import com.fzm.arch.connection.utils.extra
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.getPubKey
import com.fzm.chat.core.utils.buildProto
import com.zjy.architecture.util.logE
import dtalk.biz.Biz
import dtalk.proto.Api
import kotlinx.coroutines.*
import java.io.Serializable
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * @author zhengjy
 * @since 2020/12/08
 * Description:
 */
class SocketSender(
    private val contactManager: ContactManager,
    private val delegate: LoginDelegate,
    private val manager: ConnectionManager
) : Disposable, CoroutineScope {

    companion object {
        private val waitingAckQueue = Collections.synchronizedList(mutableListOf<SendingMessage>())

        /**
         * 指定消息是否正在发送中
         */
        fun isSending(msgId: String) = waitingAckQueue.any { it.message.msgId == msgId }
    }

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    init {
        launch {
            for ((identifier, type, extra) in MessageSubscription.confirmChannel) {
                try {
                    when (type) {
                        ConfirmType.SUCCESS -> {
                            val data = extra?.getByteArray("data") ?: continue
                            val reply = parseReplay(data)
                            sendConfirm(identifier, reply.logId, reply.datetime)
                        }
                        ConfirmType.FAIL -> sendFail(identifier)
                        ConfirmType.ABORT -> sendAbort(identifier)
                    }
                } catch (e: Exception) {
                    logE("SocketSender", e.message)
                }
            }
        }
    }

    suspend fun sendMessage(url: String, message: ChatMessage): Boolean {
        return enqueueMessage(url, message) { _, _ ->  }
    }

    private suspend fun enqueueMessage(
        url: String,
        message: ChatMessage,
        callback: suspend (Boolean, ChatMessage) -> Unit
    ): Boolean {
        val result = manager.send(url, message.buildByteArray(), extra(Socket.Op.SendMsg, requireAck = true))
        if (result != null) {
            waitingAckQueue.add(SendingMessage(result, message, callback))
            return true
        }
        return false
    }

    /**
     * 聊天消息转换为字节数组
     */
    private suspend fun ChatMessage.buildByteArray(): ByteArray {
        val body = if (ChatConfig.ENCRYPT) {
            if (channelType == ChatConst.PRIVATE_CHANNEL) {
                val target = contactManager.getUserInfo(contact, true)
                buildProto(target.getPubKey(), delegate.preference.PRI_KEY)
            } else {
                val target = contactManager.getGroupInfo(contact)
                buildProto(target.key)
            }
        } else {
            buildProto()
        }
        return body.toByteArray()
    }

    /**
     * 移除waitingAckQueue中待确认的消息，将消息状态改为发送成功
     */
    private suspend fun sendConfirm(queueIdentify: String, logId: Long, datetime: Long) {
        waitingAckQueue.forEach {
            if (it.identifier == queueIdentify) {
                val state = if (MessageSubscription.ackSet.remove(logId)) {
                    MsgState.SENT_AND_RECEIVE
                } else {
                    MsgState.SENT
                }
                updateMessageState(it, logId, datetime, state)
                it.callback(true, it.message)
                return@forEach
            }
        }
    }

    /**
     * 移除waitingAckQueue中待确认的消息，将消息状态改为发送失败
     */
    private suspend fun sendFail(identifier: String) {
        waitingAckQueue.forEach {
            if (it.identifier == identifier) {
                updateMessageState(it, it.message.logId, it.message.datetime, MsgState.FAIL)
                it.callback(false, it.message)
                return@forEach
            }
        }
    }

    /**
     * 发送终止，清除waitingAckQueue中待确认的消息
     */
    private fun sendAbort(identifier: String) {
        waitingAckQueue.forEach {
            if (it.identifier == identifier) {
                waitingAckQueue.remove(it)
                return@forEach
            }
        }
    }

    private suspend fun updateMessageState(sending: SendingMessage, logId: Long, datetime: Long, state: Int) {
        sending.message.let { message->
            message.logId = logId
            message.datetime = datetime
            message.state = state
            // 数据库操作
            database.messageDao().updateMessage(message.msgId, message.logId, message.datetime, message.state)
            database.recentSessionDao().updateRecentLogId(message.logId, message.contact, message.channelType)
        }
        waitingAckQueue.remove(sending)
        MessageSubscription.onUpdateState(sending.message)
    }

    private suspend fun parseReplay(bytes: ByteArray): Biz.SendMsgAck {
        return suspendCancellableCoroutine { continuation ->
            try {
                val msg = Api.Proto.parseFrom(bytes)
                continuation.resume(Biz.SendMsgAck.parseFrom(msg.body))
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    override fun dispose() {
        waitingAckQueue.clear()
    }

    inner class SendingMessage(
        /**
         * 消息队列唯一标识符
         */
        val identifier: String,
        val message: ChatMessage,
        val callback: suspend (Boolean, ChatMessage) -> Unit
    ) : Serializable

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Main.immediate
}