package com.fzm.chat.vm

import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chad.library.adapter.base.entity.SectionEntity
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.local.MessageRepository
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.toMessagePO
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.core.media.DownloadManager2
import com.zjy.architecture.mvvm.LoadingViewModel
import kotlinx.coroutines.launch
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/03/29
 * Description:
 */
class ChatFileViewModel : LoadingViewModel() {

    var selectable = false

    private var chatFilePage = Long.MAX_VALUE
    private var chatMediaPage = Long.MAX_VALUE

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val _chatFiles = MutableLiveData<ChatFileWrapper>()
    val chatFiles: LiveData<ChatFileWrapper>
        get() = _chatFiles

    private val _chatMedia = MutableLiveData<ChatMediaWrapper>()
    val chatMedia: LiveData<ChatMediaWrapper>
        get() = _chatMedia

    private val _chooseMode = MutableLiveData<Boolean>()
    val chooseMode: LiveData<Boolean>
        get() = _chooseMode

    private val selectMsg = mutableListOf<ChatMessage>()

    private val _downloadResult by lazy { MutableLiveData<String>() }
    val downloadResult: LiveData<String>
        get() = _downloadResult

    private val _deleteResult by lazy { MutableLiveData<List<ChatMessage>>() }
    val deleteResult: LiveData<List<ChatMessage>>
        get() = _deleteResult

    fun select(message: ChatMessage) {
        selectMsg.add(message)
    }

    fun unSelect(message: ChatMessage) {
        selectMsg.remove(message)
    }

    fun clearSelect() {
        selectMsg.clear()
    }

    fun downloadSelected() {
        if (selectMsg.isEmpty()) {
            switchChooseMode()
            return
        }
        launch {
            loading(true)
            val downloads = ArrayList(selectMsg)
            downloads.forEach {
                DownloadManager2.saveToDownloadSync(it)
            }
            _downloadResult.value = "${Environment.DIRECTORY_DOWNLOADS}/${AppConfig.APP_NAME_EN}"
            dismiss()
        }
    }

    fun deleteSelected() {
        if (selectMsg.isEmpty()) {
            switchChooseMode()
            return
        }
        launch {
            loading(true)
            selectMsg.forEach {
                database.messageDao().deleteMessage(it.msgId, it.logId, it.channelType)
            }
            _deleteResult.value = ArrayList(selectMsg)
            dismiss()
        }
    }

    fun updateMessage(message: ChatMessage) = launch {
        database.messageDao().insert(message.toMessagePO())
    }

    fun switchChooseMode() {
        selectable = !selectable
        _chooseMode.value = selectable
    }

    fun refreshChatFile(target: String, channelType: Int) {
        getChatFile(target, channelType)
    }

    fun loadMoreChatFile(target: String, channelType: Int) {
        getChatFile(target, channelType, chatFilePage)
    }

    private fun getChatFile(target: String, channelType: Int, datetime: Long = Long.MAX_VALUE) {
        launch {
            val chatFiles = MessageRepository.getChatFile(target, channelType, datetime)
            chatFiles.lastOrNull()?.also {
                chatFilePage = it.datetime
            }
            _chatFiles.value = ChatFileWrapper(chatFiles, datetime == Long.MAX_VALUE, chatFiles.size < ChatConfig.PAGE_SIZE)
        }
    }

    fun refreshChatMedia(target: String, channelType: Int) {
        getChatMedia(target, channelType)
    }

    fun loadMoreChatMedia(target: String, channelType: Int) {
        getChatMedia(target, channelType, chatMediaPage)
    }

    private fun getChatMedia(target: String, channelType: Int, datetime: Long = Long.MAX_VALUE) {
        launch {
            val chatMedia = MessageRepository.getChatMedia(target, channelType, datetime)
            chatMedia.lastOrNull()?.also {
                chatMediaPage = it.datetime
            }
            val entity = chatMedia.buildSection()
            _chatMedia.value = ChatMediaWrapper(entity, datetime == Long.MAX_VALUE, chatMedia.size < ChatConfig.PAGE_SIZE)
        }
    }

    private fun List<ChatMessage>.buildSection(): List<ChatMediaEntity> {
        if (isEmpty()) return emptyList()
        var lastDay = ""
        val result = mutableListOf<ChatMediaEntity>().also {
            lastDay = Utils.formatDay(this[0].datetime)
            it.add(ChatMediaEntity(true, lastDay))
        }
        for (i in this.indices) {
            val currentDay = Utils.formatDay(this[i].datetime)
            if (lastDay != currentDay) {
                lastDay = currentDay
                result.add(ChatMediaEntity(true, lastDay))
            }
            result.add(ChatMediaEntity(false, this[i]))
        }
        return result
    }
}

data class ChatFileWrapper(
    val list: List<ChatMessage>,
    val refresh: Boolean,
    val noMore: Boolean
) : Serializable

data class ChatMediaWrapper(
    val list: List<ChatMediaEntity>,
    val refresh: Boolean,
    val noMore: Boolean
) : Serializable

data class ChatMediaEntity(override val isHeader: Boolean, val content: Any) : SectionEntity, Serializable