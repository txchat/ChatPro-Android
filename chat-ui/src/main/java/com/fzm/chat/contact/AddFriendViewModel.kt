package com.fzm.chat.contact

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.logic.MessageSender
import com.fzm.chat.core.repo.ContractRepository
import com.fzm.chat.core.session.LoginDelegate
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/11/15
 * Description:
 */
class AddFriendViewModel(
    private val repository: ContractRepository,
    private val manager: ContactManager,
    private val messageSender: MessageSender,
    delegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by delegate {

    private val _addFriendResult by lazy { MutableLiveData<Unit>() }
    val addFriendResult: LiveData<Unit>
        get() = _addFriendResult

    fun addFriend(address: String, remark: String, msgStr: String) {
        request<String> {
            onRequest {
                val result = repository.addFriends(listOf(address), emptyList(), remark)
                if (result.isSucceed()) {
                    val content = MessageContent.text(msgStr)
                    val message = ChatMessage.create(
                        getAddress(),
                        address,
                        ChatConst.PRIVATE_CHANNEL,
                        Biz.MsgType.Text,
                        content,
                        null
                    )
                    manager.getUserInfo(address).getServerList().firstOrNull()?.also {
                        sendMessageInner(it.address, message)
                    }
                }
                result
            }
            onSuccess {
                _addFriendResult.value = Unit
            }
        }
    }

    private fun sendMessageInner(url: String, message: ChatMessage) {
        messageSender.send(url, message)
    }
}