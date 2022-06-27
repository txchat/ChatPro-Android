package com.fzm.rtc.msg

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.toMessagePO
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.core.rtc.data.RTCEndStatus
import com.fzm.chat.core.rtc.data.RTCTask
import com.fzm.chat.core.session.LoginDelegate
import dtalk.biz.Biz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2021/07/22
 * Description:
 */
class LocalRTCMessageManager(private val delegate: LoginDelegate) {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    /**
     * 音视频通话正常结束消息
     */
    fun callComplete(task: RTCTask) {
        if (task.caller.isNullOrEmpty()) return
        sendMessageToDB(task.clone(), RTCEndStatus.NORMAL)
    }

    /**
     * 音视频通话拒绝消息
     */
    fun callRejected(task: RTCTask) {
        if (task.caller.isNullOrEmpty()) return
        sendMessageToDB(task.clone(), RTCEndStatus.REJECT)
    }

    /**
     * 音视频通话取消消息
     */
    fun callCanceled(task: RTCTask) {
        if (task.caller.isNullOrEmpty()) return
        sendMessageToDB(task.clone(), RTCEndStatus.CANCEL)
    }

    /**
     * 音视频通话忙线消息
     */
    fun callBusy(task: RTCTask) {
        if (task.caller.isNullOrEmpty()) return
        sendMessageToDB(task.clone(), RTCEndStatus.BUSY)
    }

    /**
     * 音视频通话超时消息
     */
    fun callTimeout(task: RTCTask) {
        if (task.caller.isNullOrEmpty()) return
        sendMessageToDB(task.clone(), RTCEndStatus.TIMEOUT)
    }

    /**
     * 音视频通话失败消息
     */
    fun callFailed(task: RTCTask) {
        if (task.caller.isNullOrEmpty()) return
        sendMessageToDB(task.clone(), RTCEndStatus.FAIL)
    }

    private fun sendMessageToDB(task: RTCTask, status: Int) {
        val channelType = if (task.groupId != 0L) ChatConst.GROUP_CHANNEL else ChatConst.PRIVATE_CHANNEL
        val targetId = if (channelType == ChatConst.GROUP_CHANNEL) {
            task.groupId.toString()
        } else {
            if (task.caller == delegate.getAddress()) {
                task.userList[0]
            } else {
                task.caller
            }
        }
        val content = MessageContent.rtcCall(task.rtcType, status, task.duration.toInt())
        val message = if (task.caller == delegate.getAddress()) {
            ChatMessage.create(
                delegate.getAddress(),
                targetId,
                channelType,
                Biz.MsgType.RTCCall,
                content
            )
        } else {
            ChatMessage.create(
                targetId,
                delegate.getAddress(),
                channelType,
                Biz.MsgType.RTCCall,
                content
            )
        }
        GlobalScope.launch(Dispatchers.Main) {
            database.recentSessionDao().insertMessage(message.toMessagePO(), 0, false)
            MessageSubscription.onReceiveMessage(message)
        }
    }
}