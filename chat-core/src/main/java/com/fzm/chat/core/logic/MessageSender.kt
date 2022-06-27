package com.fzm.chat.core.logic

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.fzm.arch.connection.Disposable
import com.fzm.chat.core.crypto.toEncryptFile
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.model.*
import com.fzm.chat.core.data.msg.MsgState
import com.fzm.chat.core.media.DownloadManager2
import com.fzm.chat.router.oss.MediaType
import com.fzm.chat.router.oss.OssModule
import com.fzm.chat.router.oss.OssService
import com.fzm.chat.router.route
import com.zjy.architecture.util.FileUtils
import com.zjy.architecture.util.isContent
import com.zjy.architecture.util.logD
import com.zjy.architecture.util.logE
import dtalk.biz.Biz
import kotlinx.coroutines.*
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

/**
 * @author zhengjy
 * @since 2021/12/06
 * Description:
 */
class MessageSender(
    private val context: Context,
    private val socketSender: SocketSender,
    private val httpSender: HttpSender,
): CoroutineScope, Disposable {

    companion object {

        private val pendingSend = Collections.synchronizedList(ArrayList<String>())

        fun isSending(message: ChatMessage): Boolean {
            val sending = SocketSender.isSending(message.msgId) || HttpSender.isSending(message.msgId)
            val pending = pendingSend.contains(message.msgId)
            return sending || pending
        }
    }

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Main.immediate

    private val ossService by route<OssService>(OssModule.APP_OSS)

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    /**
     * 重新发送消息，手动触发消息重发
     *
     * @param url       发送的目标服务器
     * @param message   要发送的聊天消息
     */
    fun resend(url: String, message: ChatMessage) = launch {
        message.datetime = System.currentTimeMillis()
        message.state = MsgState.SENDING
        database.messageDao().insert(message.toMessagePO())
        database.recentSessionDao().updateRecentLog(message.contact, message.channelType)
        MessageSubscription.onUpdateContent(message)
        updateMessageState(message)
        send(url, message)
    }

    /**
     * 发送消息，如果是需要上传文件资源的消息类型，则自动上传文件，消息发送回调由
     * MessageSubscription.subscribeMessage分发
     *
     * @param url       发送的目标服务器
     * @param message   要发送的聊天消息
     */
    fun send(url: String, message: ChatMessage) {
        pendingSend.add(message.msgId)
        launch {
            try {
                sendSuspend(url, message)
            } finally {
                pendingSend.remove(message.msgId)
            }
        }
    }

    private suspend fun sendSuspend(url: String, message: ChatMessage) {
        MessageSubscription.onReceiveMessage(message)
        insertLocal(message)
        if (message.isFileType && message.msg.localUrl.isContent()) {
            // 如果文件不在应用内部存储，则复制一份到内部存储，作为本地文件地址
            val bundle = FileUtils.queryFile(context, message.msg.localUrl)
            val displayName = bundle?.getString(MediaStore.MediaColumns.DISPLAY_NAME)
            val (folder, name) = DownloadManager2.getFileStorePath(
                message.msgType,
                message.logId,
                message.msg.fileName,
                displayName
            )
            val file = FileUtils.createFile(context, folder, name)
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(Uri.parse(message.msg.localUrl))
                        ?.use { input ->
                            BufferedOutputStream(FileOutputStream(file)).use {
                                input.copyTo(it)
                                it.flush()
                            }
                        }
                    message.msg.localUrl = file.absolutePath
                    MessageSubscription.onUpdateContent(message)
                    updateMessage(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        var needUpload = false
        val uploadSuccess = when {
            message.msgType == Biz.MsgType.Forward_VALUE -> {
                needUpload = true
                uploadForwardFile(url, message.msg.forwardLogs, message.contact)
            }
            message.isFileType -> {
                needUpload = true
                uploadMessageFile(url, message)
            }
            else -> true
        }
        if (uploadSuccess) {
            if (needUpload) {
                MessageSubscription.onUpdateContent(message)
                updateMessage(message)
            }
            // 将构建好的消息发送到服务端
            sendMessageInner(url, message)
        } else {
            message.state = MsgState.FAIL
            MessageSubscription.onUpdateState(message)
            updateMessageState(message)
            updateMessage(message)
        }
    }

    /**
     * 发送消息
     *
     * 如果已经连接了应对地址的webSocket，则直接通过连接发送；否则走http通道发送
     *
     * @param url       发送的目标服务器
     * @param message   要发送的聊天消息
     */
    private suspend fun sendMessageInner(url: String, message: ChatMessage) {
        if (socketSender.sendMessage(url, message)) {
            return
        }
        httpSender.sendMessage(url, message)
    }

    /**
     * 插入消息本地数据库
     *
     * @param message   聊天消息
     */
    private suspend fun insertLocal(message: ChatMessage) {
        database.recentSessionDao().insertMessage(message.toMessagePO(), 0, false)
    }

    /**
     * 更新消息本地数据库
     *
     * @param message   聊天消息
     */
    private suspend fun updateMessage(message: ChatMessage) {
        database.messageDao().updateMessageContent(message.msgId, message.msg)
    }

    /**
     * 更新本地消息状态
     *
     * @param message   聊天消息
     */
    private suspend fun updateMessageState(message: ChatMessage) {
        database.messageDao().updateMsgState(message.msgId, message.state)
    }

    /**
     * 上传要转发的消息中的文件
     *
     * @return 是否全部上传成功
     */
    private suspend fun uploadMessageFile(url: String, message: ChatMessage): Boolean {
        val type = when (message.msgType) {
            Biz.MsgType.Image_VALUE -> MediaType.PICTURE
            Biz.MsgType.Audio_VALUE -> MediaType.AUDIO
            Biz.MsgType.Video_VALUE -> MediaType.VIDEO
            Biz.MsgType.File_VALUE -> MediaType.FILE
            else -> -1
        }
        if (type != -1 && message.msg.mediaUrl.isNullOrEmpty()) {
            if (ChatConfig.FILE_ENCRYPT) {
                val path = message.msg.localUrl?.toEncryptFile(context, message.contact)?.absolutePath
                message.msg.mediaUrl = uploadMedia(url, path, type)
            } else {
                message.msg.mediaUrl = uploadMedia(url, message.msg.localUrl, type)
            }
            if (message.msg.mediaUrl.isNullOrEmpty()) {
                return false
            }
        }
        return true
    }

    /**
     * 上传转发类型消息中，被转发的消息中的文件
     *
     * @return 是否全部上传成功
     */
    private suspend fun uploadForwardFile(url: String, list: List<ForwardMsg>?, target: String): Boolean {
        val start = System.currentTimeMillis()
        try {
            // 使用coroutineScope包裹上传任务，有一个任务失败，则取消整个上传任务
            return coroutineScope {
                val deferredQueue = ArrayDeque<Deferred<Unit>>()
                list?.forEach {
                    val type = when (it.msgType) {
                        Biz.MsgType.Image_VALUE -> MediaType.PICTURE
                        Biz.MsgType.Audio_VALUE -> MediaType.AUDIO
                        Biz.MsgType.Video_VALUE -> MediaType.VIDEO
                        Biz.MsgType.File_VALUE -> MediaType.FILE
                        else -> -1
                    }
                    if (type != -1 && it.msg.mediaUrl.isNullOrEmpty()) {
                        if (ChatConfig.FILE_ENCRYPT) {
                            deferredQueue.add(async {
                                val path = it.msg.localUrl?.toEncryptFile(context, target)?.absolutePath
                                it.msg.mediaUrl = uploadMedia(url, path, type, true)
                            })
                        } else {
                            deferredQueue.add(async {
                                it.msg.mediaUrl = uploadMedia(url, it.msg.localUrl, type, true)
                            })
                        }
                    }
                }
                while (deferredQueue.isNotEmpty()) {
                    deferredQueue.poll()?.await()
                }
                return@coroutineScope list?.none {
                    it.isFileType && it.msg.mediaUrl.isNullOrEmpty()
                } ?: true
            }
        } catch (e: Exception) {
            return false
        } finally {
            logD("转发文件上传耗时${System.currentTimeMillis() - start}ms")
        }
    }

    /**
     * 上传文件资源
     */
    private suspend fun uploadMedia(url: String, path: String?, @MediaType type: Int, throws: Boolean = false): String {
        return try {
            if (path.isContent()) {
                ossService?.uploadMedia(url, path?.toUri(), type) ?: ""
            } else {
                ossService?.uploadMedia(url, path, type) ?: ""
            }
        } catch (e: Exception) {
            logE(Log.getStackTraceString(e))
            // CrashReport.postCatchedException(Exception("文件上传失败", e))
            if (throws) throw e
            ""
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
        socketSender.dispose()
    }
}