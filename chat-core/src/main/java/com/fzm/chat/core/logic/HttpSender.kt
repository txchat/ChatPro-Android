package com.fzm.chat.core.logic

import android.net.Uri
import chat33.comet.Socket
import com.fzm.arch.connection.protocol.Protocols
import com.fzm.arch.connection.utils.print
import com.fzm.arch.connection.utils.urlKey
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.SendMsgReply
import com.fzm.chat.core.data.bean.getPubKey
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.contact
import com.fzm.chat.core.data.msg.MsgState
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.buildProto
import com.fzm.chat.core.utils.toHttpUrl
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zjy.architecture.data.PARSE_ERROR
import com.zjy.architecture.data.Result
import com.zjy.architecture.exception.ApiException
import com.zjy.architecture.ext.SUCCESS_CODE
import com.zjy.architecture.net.HttpResult
import com.zjy.architecture.util.logD
import com.zjy.architecture.util.logE
import com.zjy.architecture.util.logV
import kotlinx.coroutines.*
import okhttp3.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * @author zhengjy
 * @since 2020/12/08
 * Description:
 */
class HttpSender(
    private val client: OkHttpClient,
    private val delegate: LoginDelegate,
    private val contactManager: ContactManager,
    private val gson: Gson
) : LoginDelegate by delegate {

    companion object {
        const val MAX_RETRY_TIMES = 3
        const val API_SEND_MSG = "/record/push2"

        private val sendingMessage = ConcurrentHashMap<String, ChatMessage>()

        /**
         * 指定消息是否正在发送中
         */
        fun isSending(msgId: String) = sendingMessage.keys.any { it == msgId }
    }

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val callRetryMap = mutableMapOf<Request, Int>()

    /**
     * 使用Http接口发送消息
     *
     * 发消息的http请求不需要被取消，因此用GlobalScope
     */
    suspend fun sendMessage(url: String, message: ChatMessage) {
        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            updateMessageState(message, 0L, message.datetime, MsgState.FAIL)
            return
        }
        val path = uri.buildUpon().path(API_SEND_MSG).build().toString().toHttpUrl()
        if (!path.startsWith("http")) {
            updateMessageState(message, 0L, message.datetime, MsgState.FAIL)
            return
        }
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("message", "message", RequestBody.create(null, message.buildByteArray(path)))
            .build()
        val request = Request.Builder().url(path).post(body).build()
        sendingMessage[message.msgId] = message
        coroutineScope {
            launch(Dispatchers.IO) {
                val result = startRequest(request)
                if (result.isSucceed()) {
                    val reply = gson.fromJson(gson.toJson(result.data()), SendMsgReply::class.java)
                    val state = if (MessageSubscription.ackSet.remove(reply.logId)) {
                        MsgState.SENT_AND_RECEIVE
                    } else {
                        MsgState.SENT
                    }
                    updateMessageState(message, reply.logId, reply.datetime, state)
                    logD("msg:Http  Rev $url |", "$reply")
                } else {
                    updateMessageState(message, 0L, message.datetime, MsgState.FAIL)
                }
                result.dataOrNull()
            }
        }
    }

    private suspend fun updateMessageState(message: ChatMessage, logId: Long, datetime: Long, state: Int) {
        message.logId = logId
        message.datetime = datetime
        message.state = state
        // 数据库操作
        database.messageDao().updateMessage(message.msgId, message.logId, message.datetime, message.state)
        database.recentSessionDao().updateRecentLogId(message.logId, message.contact, message.channelType)
        sendingMessage.remove(message.msgId)
        MessageSubscription.onUpdateState(message)
    }

    private suspend fun ChatMessage.buildByteArray(url: String): ByteArray {
        val body = if (ChatConfig.ENCRYPT) {
            if (channelType == ChatConst.PRIVATE_CHANNEL) {
                val target = contactManager.getUserInfo(contact, true)
                buildProto(target.getPubKey(), preference.PRI_KEY)
            } else {
                val target = contactManager.getGroupInfo(contact)
                buildProto(target.key)
            }
        } else {
            buildProto()
        }
        val proto = Socket.Proto.newBuilder()
            .setVer(Protocols.PROTOCOL_VER)
            .setOp(Socket.Op.SendMsg_VALUE)
            .setSeq(0)
            .setAck(0)
            .setBody(body.toByteString())
            .build()
        proto.print("Http Send [${url.urlKey()}]")
        return proto.toByteArray()
    }

    private suspend fun startRequest(request: Request): Result<Map<String, Any>> {
        try {
            callRetryMap[request] = 1
            return request.executeWithRetry()
        } finally {
            callRetryMap.remove(request)
        }
    }

    private suspend fun Request.executeWithRetry(): Result<Map<String, Any>> {
        val call = client.newCall(this)
        val response = try {
            call.await()
        } catch (e: Exception) {
            val times = callRetryMap[this] ?: 1
            return if (times < MAX_RETRY_TIMES) {
                delay(5000)
                logV("HttpSender", "${if (times == 1) "2nd" else "3rd"} attempt to send")
                callRetryMap[this] = times + 1
                this.executeWithRetry()
            } else {
                logE("HttpSender", "Failed after trying to send 3 times")
                Result.Error(e)
            }
        }

        return try {
            val result = gson.fromJson<HttpResult<Map<String, Any>>>(response, object : TypeToken<HttpResult<Map<String, Any>>>() {}.type)
            if (result.code == SUCCESS_CODE) {
                Result.Success(result.data)
            } else {
                Result.Error(ApiException(result.message))
            }
        } catch (e: Exception) {
            Result.Error(ApiException(PARSE_ERROR))
        }
    }

    private suspend fun Call.await(): String {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                cancel()
            }
            enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            continuation.resume(body.string())
                        } else {
                            continuation.resumeWithException(ApiException(PARSE_ERROR))
                        }
                    } else {
                        continuation.resumeWithException(ApiException(response.message()))
                    }
                }
            })
        }
    }
}