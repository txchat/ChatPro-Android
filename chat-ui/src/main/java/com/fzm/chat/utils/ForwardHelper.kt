package com.fzm.chat.utils

import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.filterFile
import com.fzm.chat.core.data.model.isFileType
import com.fzm.chat.core.data.model.localExists
import com.fzm.chat.core.media.DownloadManager2
import com.zjy.architecture.base.Loadable
import dtalk.biz.Biz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author zhengjy
 * @since 2021/09/24
 * Description:
 */
object ForwardHelper {

    /**
     * 转发消息前，先把消息中包含的文件全部下载到本地
     *
     * @param loadable              界面loading
     * @param message               要转发的消息
     * @param downloadRecursive     下载转发消息中的文件类型消息（只下载一层，发送合并转发消息时不需要下载）
     * @param callback              下载完成回调
     */
    suspend fun checkForwardFile(
        loadable: Loadable?,
        message: ArrayList<ChatMessage>,
        downloadRecursive: Boolean = true,
        callback: (error: String?, messages: ArrayList<ChatMessage>) -> Unit
    ) {
        try {
            loadable?.loading(true)
            var error: String? = null
            withContext(Dispatchers.IO) {
                // 文件类型消息下载
                message.filterFile().forEach {
                    if (!it.localExists()) {
                        val result = DownloadManager2.downloadToAppSync(it)
                        if (!result.isSucceed()) {
                            error = result.error().message
                            return@withContext
                        }
                    }
                }
                if (downloadRecursive) {
                    // 合并转发中的文件类型消息下载
                    message.filter { it.msgType == Biz.MsgType.Forward_VALUE }.forEach { msg ->
                        msg.msg.forwardLogs.forEachIndexed { index, forwardMsg ->
                            if (forwardMsg.msgType == Biz.MsgType.Image_VALUE ||
                                forwardMsg.msgType == Biz.MsgType.Video_VALUE ||
                                forwardMsg.msgType == Biz.MsgType.File_VALUE
                            ) {
                                // 转发语音不能下载
                                val result = DownloadManager2.downloadToAppSync(msg, index)
                                if (!result.isSucceed()) {
                                    error = result.error().message
                                    return@withContext
                                }
                            }
                        }
                    }
                }
            }
            callback(error, message)
        } catch (e: Exception) {
            callback(e.message, message)
        } finally {
            loadable?.dismiss()
        }
    }
}