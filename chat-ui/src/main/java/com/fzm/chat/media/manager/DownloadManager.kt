package com.fzm.chat.media.manager

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.ForwardMsg
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.util.FileUtils
import dtalk.biz.Biz
import kotlinx.coroutines.runBlocking
import okhttp3.*
import java.io.IOException

/**
 * @author zhengjy
 * @since 2021/03/17
 * Description:
 */
@Deprecated("使用DownloadManager2")
object DownloadManager {

    class Task(
        val call: Call,
        val result: LiveData<DownState>
    )

    private val client by rootScope.inject<OkHttpClient>()
    private val context by rootScope.inject<Context>()

    private val runningTask = mutableMapOf<String, Task>()

    fun contains(tag: String) = runningTask.values.any { it.call.request().tag() == tag }

    fun download(message: ChatMessage): LiveData<DownState> {
        val name = when (Biz.MsgType.forNumber(message.msgType)) {
            Biz.MsgType.Image -> {
                "IMG_${System.currentTimeMillis()}_${message.logId}.${FileUtils.getExtension(message.msg.mediaUrl)}"
            }
            Biz.MsgType.Video -> {
                "VID_${System.currentTimeMillis()}_${message.logId}.${FileUtils.getExtension(message.msg.mediaUrl)}"
            }
            Biz.MsgType.File -> "DOC_${System.currentTimeMillis()}_${message.msg.fileName}"
            else -> null
        }
        return download(message.msg.mediaUrl, name, message.msg.md5, "${message.logId}")
    }

    fun download(message: ForwardMsg): LiveData<DownState> {
        val name = when (Biz.MsgType.forNumber(message.msgType)) {
            Biz.MsgType.Image -> {
                "IMG_${System.currentTimeMillis()}_forward.${FileUtils.getExtension(message.msg.mediaUrl)}"
            }
            Biz.MsgType.Video -> {
                "VID_${System.currentTimeMillis()}_forward.${FileUtils.getExtension(message.msg.mediaUrl)}"
            }
            Biz.MsgType.File -> "DOC_${System.currentTimeMillis()}_${message.msg.fileName}"
            else -> null
        }
        return download(message.msg.mediaUrl, name, message.msg.md5, "${message.msg.mediaUrl}")
    }

    fun download(url: String?): LiveData<DownState> {
        val name = "chat_export_${System.currentTimeMillis()}.${FileUtils.getExtension(url)}"
        return download(url, name, null, name)
    }

    fun download(url: String?, name: String?, md5: String?, tag: String): LiveData<DownState> {
        val result = MutableLiveData<DownState>()
        if (url.isNullOrEmpty() || name.isNullOrEmpty()) {
            return result.apply { postValue(DownState.Fail(Exception("empty params"))) }
        }

        var call: Call
        synchronized(this) {
            val task = runningTask[tag]
            if (task != null) {
                return task.result
            }

            val request = Request.Builder().get().url(url).tag(tag).build()
            call = client.newCall(request)
            runningTask[tag] = Task(call, result)
        }
        call.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            saveFile(body, name, result)
                        } else {
                            throw IOException("response body is null")
                        }
                    } else {
                        result.postValue(DownState.Fail(Exception(response.message())))
                    }
                } catch (e: Exception) {
                    result.postValue(DownState.Fail(e))
                } finally {
                    runningTask.remove(tag)
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                result.postValue(DownState.Fail(e))
                runningTask.remove(tag)
            }
        })
        return result
    }

    /**
     * 将Response解析转换成File
     */
    @Throws(IOException::class)
    fun saveFile(body: ResponseBody, fileName: String, result: MutableLiveData<DownState>) {
        runBlocking {
            val ext = FileUtils.getExtension(fileName)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            val file = FileUtils.saveToDownloads(
                context,
                body.byteStream(),
                AppConfig.APP_NAME_EN,
                fileName,
                mimeType
            ) {
                result.postValue(DownState.Running(it))
            }
            if (file == null) {
                result.postValue(DownState.Fail(IOException("file save failed")))
            } else {
                result.postValue(DownState.Success(file))
            }
        }
    }
}