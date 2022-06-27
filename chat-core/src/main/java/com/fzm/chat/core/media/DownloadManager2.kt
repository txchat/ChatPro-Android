package com.fzm.chat.core.media

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.core.crypto.decrypt
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Option
import com.fzm.chat.core.data.model.*
import com.fzm.chat.core.logic.MessageSubscription
import com.zjy.architecture.data.LocalFile
import com.zjy.architecture.data.Result
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.util.FileUtils
import dtalk.biz.Biz
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import org.koin.core.qualifier.named
import java.io.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/**
 * @author zhengjy
 * @since 2021/09/23
 * Description:
 */
object DownloadManager2 : CoroutineScope {

    class Task(
        val call: Call,
        val result: Deferred<Result<LocalFile>>,
        val liveData: MutableLiveData<Result<LocalFile>>
    )

    private val appFolder by rootScope.inject<String>(named("APP_NAME_EN"))

    private val lock = Mutex()

    private val client by lazy {
        val c = rootScope.get<OkHttpClient>()
        c.newBuilder()
            .addNetworkInterceptor(Interceptor {
                val original = it.proceed(it.request())
                return@Interceptor original.newBuilder()
                    .body(ProgressResponseBody(it.request().tag(), original.body(), object : ProgressResponseBody.Callback {
                        var oldProgress = 0f
                        override fun onProgress(tag: Any?, progress: Float) {
                            if (progress != oldProgress) {
                                oldProgress = progress
                                runningTask[tag]?.liveData?.postValue(Result.Loading(progress))
                            }
                        }
                    }))
                    .build()
            })
            .build()
    }

    private val context by rootScope.inject<Context>()

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val runningTask = mutableMapOf<String, Task>()

    suspend fun contains(tag: String) = lock.withLock {
        runningTask.values.any { it.call.request().tag() == tag }
    }

    private suspend fun getTask(tag: String) = lock.withLock {
        runningTask[tag]
    }

    suspend fun contains(message: ChatMessage) = lock.withLock {
        runningTask.keys.any { it == message.logId.toString() }
    }

    private suspend fun getTask(message: ChatMessage) = lock.withLock {
        runningTask[message.logId.toString()]
    }

    suspend fun contains(message: ForwardMsg) = lock.withLock {
        runningTask.keys.any { it == message.msg.mediaUrl }
    }

    private suspend fun getTask(message: ForwardMsg) = lock.withLock {
        runningTask[message.msg.mediaUrl]
    }

    suspend fun downloadToAppSync(message: ChatMessage): Result<LocalFile> {
        if (File(message.msg.localUrl ?: "").exists()) {
            return Result.Success(LocalFile(message.msg.localUrl, null))
        }
        val (folder, name) = getFileStorePath(
            message.msgType,
            message.logId,
            message.msg.fileName,
            message.msg.mediaUrl
        )
        return downloadInternal(
            message.msg.mediaUrl,
            folder,
            name,
            message.msg.md5,
            "${message.logId}",
            message.contact,
            false,
            MutableLiveData()
        ).also {
            it.dataOrNull()?.also { file ->
                // 下载完成后替换本地url
                message.msg.localUrl = file.absolutePath
                database.messageDao().insert(message.toMessagePO())
                MessageSubscription.onMessage(Option.UPDATE_CONTENT, message)
            }
        }
    }

    fun downloadToApp(message: ChatMessage): LiveData<Result<LocalFile>> {
        val liveData = MediatorLiveData<Result<LocalFile>>()
        launch {
            if (message.localExists()) {
                liveData.postValue(Result.Success(LocalFile(message.msg.localUrl, null)))
                return@launch
            }
            val task = getTask(message)
            if (task != null) {
                liveData.addSource(task.liveData) { liveData.value = it }
                return@launch
            }
            val (folder, name) = getFileStorePath(message.msgType, message.logId, message.msg.fileName, message.msg.mediaUrl)
            val result = downloadInternal(
                message.msg.mediaUrl,
                folder,
                name,
                message.msg.md5,
                "${message.logId}",
                message.contact,
                false,
                liveData
            ).also {
                it.dataOrNull()?.also { file ->
                    // 下载完成后替换本地url
                    message.msg.localUrl = file.absolutePath
                    database.messageDao().insert(message.toMessagePO())
                    MessageSubscription.onMessage(Option.UPDATE_CONTENT, message)
                }
            }
            liveData.postValue(result)
        }
        return liveData
    }

    fun saveToDownload(message: ChatMessage): LiveData<Result<LocalFile>> {
        val liveData = MediatorLiveData<Result<LocalFile>>()
        launch {
            val appFile = File(message.msg.localUrl ?: "")
            val (folder, name) = getFileStorePath(message.msgType, message.logId, message.msg.fileName, message.msg.mediaUrl)
            if (appFile.exists()) {
                liveData.postValue(saveFileToDownload(FileInputStream(appFile), folder, name))
            } else {
                liveData.addSource(downloadToApp(message)) {
                    if (it.isSucceed()) {
                        launch {
                            liveData.value = saveFileToDownload(FileInputStream(File(it.data().absolutePath)), folder, name)
                        }
                    } else {
                        liveData.value = it
                    }
                }
            }
        }
        return liveData
    }

    suspend fun saveToDownloadSync(message: ChatMessage): Result<LocalFile> {
        val appFile = File(message.msg.localUrl ?: "")
        val (folder, name) = getFileStorePath(
            message.msgType,
            message.logId,
            message.msg.fileName,
            message.msg.mediaUrl
        )
        return if (appFile.exists()) {
            saveFileToDownload(FileInputStream(appFile), folder, name)
        } else {
            val result = downloadToAppSync(message)
            if (result.isSucceed()) {
                saveFileToDownload(FileInputStream(result.data().absolutePath), folder, name)
            } else {
                Result.Error(result.error())
            }
        }
    }

    /**---------------------------转发消息下载-----------------------------*/
    suspend fun downloadToAppSync(message: ChatMessage, forwardPos: Int): Result<LocalFile> {
        val forwardMsg = message.msg.forwardLogs[forwardPos]
        if (forwardMsg.localExists()) {
            return Result.Success(LocalFile(forwardMsg.msg.localUrl, null))
        }
        val (folder, name) = getFileStorePath(
            forwardMsg.msgType,
            "forward",
            forwardMsg.msg.fileName,
            forwardMsg.msg.mediaUrl
        )
        return downloadInternal(
            forwardMsg.msg.mediaUrl,
            folder,
            name,
            forwardMsg.msg.md5,
            "${forwardMsg.msg.mediaUrl}",
            message.contact,
            false,
            MutableLiveData()
        ).also {
            it.dataOrNull()?.also { file ->
                // 下载完成后替换本地url
                message.msg.forwardLogs[forwardPos].msg.localUrl = file.absolutePath
                database.messageDao().insert(message.toMessagePO())
                MessageSubscription.onMessage(Option.UPDATE_CONTENT, message)
            }
        }
    }

    fun downloadToApp(message: ChatMessage, forwardPos: Int): LiveData<Result<LocalFile>> {
        val liveData = MediatorLiveData<Result<LocalFile>>()
        launch {
            val forwardMsg = message.msg.forwardLogs[forwardPos]
            if (forwardMsg.localExists()) {
                liveData.postValue(Result.Success(LocalFile(forwardMsg.msg.localUrl, null)))
                return@launch
            }
            val task = getTask(forwardMsg)
            if (task != null) {
                liveData.addSource(task.liveData) { liveData.value = it }
                return@launch
            }
            val (folder, name) = getFileStorePath(forwardMsg.msgType, "forward", forwardMsg.msg.fileName, forwardMsg.msg.mediaUrl)
            val result = downloadInternal(
                forwardMsg.msg.mediaUrl,
                folder,
                name,
                forwardMsg.msg.md5,
                "${forwardMsg.msg.mediaUrl}",
                message.contact,
                false,
                liveData
            ).also {
                it.dataOrNull()?.also { file ->
                    // 下载完成后替换本地url
                    message.msg.forwardLogs[forwardPos].msg.localUrl = file.absolutePath
                    database.messageDao().insert(message.toMessagePO())
                    MessageSubscription.onMessage(Option.UPDATE_CONTENT, message)
                }
            }
            liveData.postValue(result)
        }
        return liveData
    }

    fun saveToDownload(message: ChatMessage, forwardPos: Int): LiveData<Result<LocalFile>> {
        val liveData = MediatorLiveData<Result<LocalFile>>()
        val forwardMsg = message.msg.forwardLogs[forwardPos]
        launch {
            val appFile = File(forwardMsg.msg.localUrl ?: "")
            val (folder, name) = getFileStorePath(forwardMsg.msgType, "forward", forwardMsg.msg.fileName, forwardMsg.msg.mediaUrl)
            if (appFile.exists()) {
                liveData.postValue(saveFileToDownload(FileInputStream(appFile), folder, name))
                return@launch
            } else {
                liveData.addSource(downloadToApp(message, forwardPos)) {
                    if (it.isSucceed()) {
                        launch {
                            liveData.value = saveFileToDownload(FileInputStream(File(it.data().absolutePath)), folder, name)
                        }
                    } else {
                        liveData.value = it
                    }
                }
            }
        }
        return liveData
    }

    /**---------------------------头像等文件下载-----------------------------*/
    suspend fun downloadToApp(url: String, cache: Boolean = false): LiveData<Result<LocalFile>> {
        val liveData = MediatorLiveData<Result<LocalFile>>()
        val name = "chat_export_${System.currentTimeMillis()}.${FileUtils.getExtension(url)}"
        launch {
            val task = getTask(url)
            if (task != null) {
                liveData.addSource(task.liveData) { liveData.value = it }
                return@launch
            }
            val result = downloadInternal(
                url, Environment.DIRECTORY_PICTURES, name, "",
                url, null, cache, liveData
            )
            liveData.postValue(result)
        }
        return liveData
    }

    suspend fun saveToDownload(url: String): LiveData<Result<LocalFile>> {
        val liveData = MediatorLiveData<Result<LocalFile>>()
        launch {
            liveData.addSource(downloadToApp(url, true)) {
                if (it.isSucceed()) {
                    launch {
                        val name =
                            "chat_export_${System.currentTimeMillis()}.${FileUtils.getExtension(url)}"
                        liveData.value = saveFileToDownload(
                            FileInputStream(File(it.data().absolutePath)),
                            Environment.DIRECTORY_PICTURES,
                            name
                        )
                    }
                } else {
                    liveData.value = it
                }
            }
        }
        return liveData
    }

    fun getFileStorePath(msgType: Int, symbol: Any, filename: String?, url: String?): Pair<String, String> {
        val folder: String
        val name: String?
        when (Biz.MsgType.forNumber(msgType)) {
            Biz.MsgType.Audio -> {
                folder = "Audio"
                name = "VOC_${System.currentTimeMillis()}_${symbol}.${
                    FileUtils.getExtension(url)
                }"
            }
            Biz.MsgType.Image -> {
                folder = Environment.DIRECTORY_PICTURES
                name = "IMG_${System.currentTimeMillis()}_${symbol}.${
                    FileUtils.getExtension(url)
                }"
            }
            Biz.MsgType.Video -> {
                folder = "Video"
                name = "VID_${System.currentTimeMillis()}_${symbol}.${
                    FileUtils.getExtension(url)
                }"
            }
            else -> {
                folder = Environment.DIRECTORY_DOCUMENTS
                name = "$filename"
            }
        }
        return folder to name
    }

    private suspend fun downloadInternal(
        url: String?,
        folder: String,
        filename: String?,
        md5: String?,
        tag: String,
        target: String?,
        cache: Boolean,
        liveData: MutableLiveData<Result<LocalFile>>
    ): Result<LocalFile> {
        if (filename.isNullOrEmpty()) {
            return Result.Error(Exception("文件名为空"))
        }
        val httpUrl = try {
            HttpUrl.get(url!!)
        } catch (e: Exception) {
            return Result.Error(Exception("下载地址错误，$url"))
        }
        val result = lock.withLock {
            val task = runningTask[tag]
            if (task != null) {
                return@withLock task.result
            }

            val request = Request.Builder().get().url(httpUrl).tag(tag).build()
            val call = client.newCall(request)
            val result = async(Dispatchers.IO) {
                val input = call.await()
                val res = if (input.isSucceed()) {
                    val encrypted = url.contains(ChatConst.ENC_PREFIX)
                    val inputStream = if (encrypted) input.data().decrypt(target ?: "") else input.data()
                    saveFileToApp(inputStream, folder, filename, cache)
                } else {
                    Result.Error(input.error())
                }
                runningTask.remove(tag)
                return@async res
            }
            runningTask[tag] = Task(call, result, liveData)
            return@withLock result
        }
        return result.await()
    }

    private fun saveFileToApp(input: InputStream, folder: String, name: String, cache: Boolean): Result<LocalFile> {
        val dir = if (cache) {
            context.externalCacheDir?.let { File(it, folder) } ?: File(context.cacheDir, folder)
        } else {
            context.getExternalFilesDir(folder) ?: File(context.filesDir, folder)
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "${name}.downloading")
        try {
            BufferedOutputStream(FileOutputStream(file)).use { output ->
                input.use {
                    it.copyTo(output)
                    output.flush()
                }
            }
            val renameFile = File(dir, name)
            file.renameTo(renameFile)
            return Result.Success(LocalFile(renameFile.absolutePath, null))
        } catch (e: Exception) {
            try {
                // 发生错误时，删除临时文件
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                // ignore
            }
            return Result.Error(e)
        }
    }

    private suspend fun saveFileToDownload(input: InputStream, folder: String, name: String): Result<LocalFile> {
        val ext = FileUtils.getExtension(name)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        val file = FileUtils.saveToDownloads(
            context,
            input,
            "$appFolder${File.separator}$folder",
            name,
            mimeType
        )
        return if (file != null) {
            Result.Success(file)
        } else {
            Result.Error(Exception("保存失败"))
        }
    }

    private suspend fun Call.await(): Result<InputStream> {
        return suspendCancellableCoroutine { cont ->
            this.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.isSuccessful) {
                            val body = response.body()
                            if (body != null) {
                                cont.resume(Result.Success(body.byteStream()))
                            } else {
                                cont.resume(Result.Error(IOException("response body is null")))
                            }
                        } else {
                            cont.resume(Result.Error(Exception(response.message())))
                        }
                    } catch (e: Exception) {
                        cont.resume(Result.Error(e))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    cont.resume(Result.Error(e))
                }
            })
        }
    }

    fun dispose() {
        coroutineContext.cancel()
    }

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Main
}