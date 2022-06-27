package com.fzm.chat.conversation

import android.media.AudioManager
import androidx.lifecycle.*
import com.fzm.chat.R
import com.fzm.chat.bean.MuteParams
import com.fzm.chat.core.at.AtBlock
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.*
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.local.MessageRepository
import com.fzm.chat.core.data.model.*
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.core.data.po.Reference
import com.fzm.chat.core.logic.MessageSender
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.core.repo.ChatRepository
import com.fzm.chat.core.repo.ContractRepository
import com.fzm.chat.core.repo.GroupRepository
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.session.data.UserRepository
import com.fzm.chat.router.oa.CompanyUser
import com.fzm.chat.router.oa.OAModule
import com.fzm.chat.router.oa.OAService
import com.fzm.chat.router.route
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @author zhengjy
 * @since 2020/12/25
 * Description:
 */
class ChatViewModel(
    private val repository: ContractRepository,
    private val chatRepo: ChatRepository,
    private val groupRepo: GroupRepository,
    private val userRepo: UserRepository,
    private val contactManager: ContactManager,
    private val messageSender: MessageSender,
    delegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by delegate {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val oaService by route<OAService>(OAModule.SERVICE)

    private val _latestMessage = MutableLiveData<List<ChatMessage>>()
    val latestMessage: LiveData<List<ChatMessage>>
        get() = _latestMessage

    private val _locatePosition = MutableLiveData<Int>()
    val locatePosition: LiveData<Int>
        get() = _locatePosition

    private val _muteResult = MutableLiveData<GroupUserTO>()
    val muteResult: LiveData<GroupUserTO>
        get() = _muteResult

    private val _scrollBottom = MutableLiveData<Unit>()
    val scrollBottom: LiveData<Unit>
        get() = _scrollBottom

    private val _unReadCount = MutableLiveData<Int>()
    val unReadCount: LiveData<Int>
        get() = _unReadCount

    private val _scrollPosition = MutableLiveData<Int>()
    val scrollPosition: LiveData<Int>
        get() = _scrollPosition

    private var onMessageListener: ((MessageOption) -> Unit)? = null

    private var currentTarget: String? = null

    /**
     * 首次消息列表加载完成
     *
     * 用于在消息列表加载完成之前，挂起消息订阅通知，防止在并发接收消息的瞬间进入聊天页面，导致出现消息重复或缺失问题
     *
     *        原理： 打开页面时先订阅消息通知，但是先挂起，等加载完成数据库消息后，再处理消息通知
     * 可能存在问题：无法百分百保证订阅前的所有消息都已经插入到数据库
     */
    private var initLoadReady = AtomicBoolean(false)

    @ObsoleteCoroutinesApi
    private val sendChannel = actor<MessageOption>(capacity = Channel.UNLIMITED) {
        for (option in channel) {
            while (!initLoadReady.get()) {
                yield()
            }
            if (currentTarget == null) return@actor
            if (Option.UPDATE_CONTACT == option.option || currentTarget == option.contact) {
                if (option.option == Option.ADD_MSG || option.option == Option.UPDATE_STATE) {
                    if (option.message.sender == null) {
                        option.message.sender = if (option.message.channelType == ChatConst.PRIVATE_CHANNEL) {
                            contactManager.getUserInfo(option.message.from, true)
                        } else {
                            contactManager.getGroupUserInfo(option.message.target, option.message.from)
                        }
                    }
                    option.message.reference?.also { reference ->
                        if (reference.refMsg == null) {
                            reference.refMsg = database.messageDao().findMessage(reference.ref)?.apply {
                                sender = if (channelType == ChatConst.PRIVATE_CHANNEL) {
                                    contactManager.getUserInfo(from, true)
                                } else {
                                    contactManager.getGroupUserInfo(target, from)
                                }
                            }
                        }
                    }
                }
                onMessageListener?.invoke(option)
            }
        }
    }

    init {
        MessageSubscription.registerChannel(this, sendChannel)
    }

    override fun onCleared() {
        MessageSubscription.unregisterChannel(this)
    }

    fun setCurrentTarget(address: String?) {
        currentTarget = address
    }

    fun resendMessage(url: String, message: ChatMessage) {
        messageSender.resend(url, message)
    }

    fun sendMessage(url: String, message: ChatMessage) {
        messageSender.send(url, message)
    }

    /**
     * 收到消息回调
     */
    fun setOnMessageListener(listener: (MessageOption) -> Unit) {
        onMessageListener = listener
    }

    fun getMessageHistory(
        target: String,
        channelType: Int,
        datetime: Long = Long.MAX_VALUE,
        loadCount: Int = ChatConfig.PAGE_SIZE,
        needScroll :Boolean = false
    ) {
        launch {
            //以页为单位获取message
            var pageCount = loadCount
            if (loadCount % ChatConfig.PAGE_SIZE != 0) {
                pageCount = (loadCount / ChatConfig.PAGE_SIZE + 1) * ChatConfig.PAGE_SIZE
            }
            val list = MessageRepository.getMessageByTime(target, channelType, datetime, pageCount)
            _latestMessage.value = list
            initLoadReady.compareAndSet(false, true)
            if (datetime == Long.MAX_VALUE) {
                _scrollBottom.value = Unit
            }
            if (needScroll) {
                _scrollPosition.value = loadCount
            }
            _latestMessage.value?.forEach {
                queryMessageFocus(it, target)
            }
        }
    }

    fun getMessageHistoryToLogId(
        target: String,
        channelType: Int,
        msgId: String,
    ) {
        launch {
            val list = MessageRepository.getMessageByMsgId(target, channelType, msgId)
            if (list.size >= ChatConfig.PAGE_SIZE) {
                _latestMessage.value = list
            } else {
                _latestMessage.value = MessageRepository.getMessageByTime(target, channelType, Long.MAX_VALUE, ChatConfig.PAGE_SIZE)
            }
            initLoadReady.compareAndSet(false, true)
            _latestMessage.value?.forEachIndexed { index, chatMessage ->
                if (chatMessage.msgId == msgId) {
                    _locatePosition.value = index
                    return@forEachIndexed
                }
            }
            _latestMessage.value?.forEach {
                queryMessageFocus(it, target)
            }
        }
    }

    private suspend fun queryMessageFocus(message: ChatMessage, target: String) {
        // 查询消息自己是否关注
        if (message.logId != 0L) {
            val hasFocused = database.focusUserDao().hasFocused(message.logId, getAddress())
            MessageSubscription.onMessage(Option.UPDATE_FOCUS, MsgFocus(message.logId, 0, target, hasFocused))
        }
    }

    fun insert(message: ChatMessage) = launch {
        database.messageDao().insert(message.toMessagePO())
    }

    fun clearUnread(id: String, channelType: Int) = launch {
        database.recentSessionDao().clearUnread(id, channelType)
    }

    suspend fun getUnreadCount() = database.recentSessionDao().getUnreadCount()

    fun clearAtMsg(id: String, channelType: Int) = launch {
        database.recentSessionDao().clearAtMsg(id, channelType)
    }

    fun getUnreadCount(id: String, channelType: Int) = launch {
        val count = database.recentSessionDao().getUnreadMsgCount(id, channelType) ?: 0
        _unReadCount.value = count
    }

    fun saveDraft(text: String, aitInfo: Map<String, AtBlock>?, reference: Reference?, id: String, channelType: Int) {
        launch {
            if (text.isNotEmpty() || reference != null) {
                database.recentSessionDao().createOrUpdateDraft(Draft(text, aitInfo, reference, System.currentTimeMillis()), id, channelType)
            } else {
                database.recentSessionDao().createOrUpdateDraft(null, id, channelType)
            }
        }
    }

    fun loadDraft(id: String, channelType: Int) = liveData {
        val draft = database.recentSessionDao().getDraft(id, channelType)
        if (draft != null) {
            draft.reference?.let {
                database.messageDao().findMessage(it.ref)?.apply {
                    sender = if (channelType == ChatConst.PRIVATE_CHANNEL) {
                        contactManager.getUserInfo(from, true)
                    } else {
                        contactManager.getGroupUserInfoFast(target, from)
                    }
                    it.refMsg = this
                    emit(draft)
                }
            } ?: emit(draft)
        }
    }

    fun readAudioMessage(message: ChatMessage) {
        if (!message.msg.isRead) {
            message.msg.isRead = true
            insert(message)
        }
    }

    fun deleteMessage(message: ChatMessage) = launch {
        message.apply {
            database.recentSessionDao().deleteAndUpdate(contact, channelType, msgId, logId)
        }
    }

    fun deleteMessage(channelType: Int, address: String, logs: List<Pair<String, Long>>) = launch {
        database.recentSessionDao().deleteAndUpdate(address, channelType, logs)
    }

    fun muteUser(params: MuteParams) {
        request<GroupUserTO.Wrapper> {
            onRequest {
                groupRepo.changeMuteTime(null, params.gid, params.muteTime, listOf(params.address))
            }
            onSuccess {
                _muteResult.value = it.members[0]
            }
        }
    }

    fun fetchUserInfo(address: String?): LiveData<FriendUser> = liveData {
        if (address.isNullOrEmpty()) return@liveData
        emitSource(database.friendUserDao().getFriendLive(address))
        launch { repository.getFriendUser(address, 0, true) }
    }

    fun fetchCompanyUser(address: String?): LiveData<CompanyUser> = liveData {
        if (address.isNullOrEmpty()) return@liveData
        emitSource(userRepo.getCompanyUserLive(address))
        launch { oaService?.getCompanyUser(address) }
    }

    fun fetchGroupInfo(gid: String?): LiveData<GroupInfo> = liveData {
        if (gid.isNullOrEmpty()) return@liveData
        emitSource(database.groupDao().getGroupInfoLive(gid.toLong()))
        launch { groupRepo.getGroupInfo(null, gid.toLong()) }
    }

    /************************消息弹出菜单操作************************/

    private val _deleteMessage = MutableLiveData<ChatMessage>()
    val deleteMessage: LiveData<ChatMessage>
        get() = _deleteMessage

    private val _muteUser = MutableLiveData<MuteParams>()
    val muteUser: LiveData<MuteParams>
        get() = _muteUser

    private val _muteTips = MutableLiveData<Int>()
    val muteTips: LiveData<Int>
        get() = _muteTips

    private val _selectMode = MutableLiveData<Boolean>()
    val selectMode: LiveData<Boolean>
        get() = _selectMode

    private val _forwardMsg = MutableLiveData<ChatMessage>()
    val forwardMsg: LiveData<ChatMessage>
        get() = _forwardMsg

    private val _refMsg = MutableLiveData<ChatMessage>()
    val refMsg: LiveData<ChatMessage>
        get() = _refMsg

    fun requestDelete(message: ChatMessage) {
        _deleteMessage.value = message
    }

    fun requestMute(gid: Long, address: String) {
        launch {
            val user = contactManager.getGroupUserInfoFast(gid.toString(), address)
            if (user.role > GroupUser.LEVEL_USER) {
                _muteTips.value = R.string.chat_tips_can_not_mute
                return@launch
            }
            _muteUser.value = MuteParams(gid, address, user.muteTime, user.getDisplayName())
        }
    }

    fun switchAudioChannel(): Int {
        val currentMode = preference.AUDIO_CHANNEL
        if (currentMode == AudioManager.MODE_NORMAL) {
            preference.AUDIO_CHANNEL = AudioManager.MODE_IN_CALL
        } else {
            preference.AUDIO_CHANNEL = AudioManager.MODE_NORMAL
        }
        return preference.AUDIO_CHANNEL
    }

    fun requestSelect(select: Boolean) {
        _selectMode.value = select
    }

    fun requestForward(message: ChatMessage) {
        _forwardMsg.value = message
    }

    fun requestReference(message: ChatMessage) {
        _refMsg.value = message
    }

    fun requestFocus(message: ChatMessage) {
        request<Any> {
            onRequest { chatRepo.focusMessage(message.channelType, message.logId) }
        }
    }

    fun requestRevoke(message: ChatMessage) {
        request<Any> {
            onRequest { chatRepo.revokeMessage(message.channelType, message.logId) }
        }
    }
}