package com.fzm.chat.core.logic

import android.content.Context
import android.os.Bundle
import com.fzm.arch.connection.OnMessageConfirmCallback
import com.fzm.arch.connection.type.ConfirmType
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.*
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.contact
import com.fzm.chat.core.data.model.isSendType
import com.fzm.chat.core.data.msg.MsgConfirmResult
import com.fzm.chat.core.session.LoginDelegate
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.util.ActivityUtils
import dtalk.biz.Biz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import java.util.*

/**
 * @author zhengjy
 * @since 2021/01/11
 * Description:消息分发中心，讲消息分发到外部订阅者
 */
object MessageSubscription : OnMessageConfirmCallback {

    private val context by rootScope.inject<Context>()
    private val delegate by rootScope.inject<LoginDelegate>()
    private val contactManager by rootScope.inject<ContactManager>()

    private val channel = Channel<MessageOption>(Channel.UNLIMITED)
    private val subscribers = mutableMapOf<Any, (MessageOption) -> Unit>()
    private val channels = mutableMapOf<Any, SendChannel<MessageOption>>()

    internal val confirmChannel = Channel<MsgConfirmResult>(Channel.UNLIMITED)

    internal val ackSet = Collections.synchronizedSet(HashSet<Long>())

    init {
        GlobalScope.launch(Dispatchers.Main) {
            launch {
                for (msg in channel) {
                    channels.forEach { it.value.trySend(msg) }
                    subscribers.forEach { it.value(msg) }
                    if (msg.option == Option.ADD_MSG) {
                        val main = ActivityUtils.isActivityTop(context, "com.fzm.chat.app.MainActivity")
                        val invisible = ActivityUtils.isBackground || (!main && msg.message.contact != ChatConfig.CURRENT_TARGET)
                        if (invisible &&
                            !msg.message.isSendType &&
                            msg.message.notify &&
                            msg.message.msgType != Biz.MsgType.RTCCall_VALUE &&
                            msg.option == Option.ADD_MSG &&
                            delegate.preference.NEW_MSG_NOTIFY
                        ) {
                            // 同时满足以下情况显示通知：
                            // 1.App在后台或者不在首页且不是当前聊天对象发送的消息（即聊天对象消息列表不可见）
                            // 2.收到的消息
                            // 3.不是音视频通知消息
                            // 4.option为ADD_MSG
                            // 5.没有设置免打扰
                            val target = if (msg.message.channelType == ChatConst.PRIVATE_CHANNEL) {
                                contactManager.getUserInfo(msg.message.from, true)
                            } else {
                                contactManager.getGroupInfo(msg.message.target)
                            }
                            if (!target.noDisturb) {
                                onShowNotificationListener?.invoke(msg.message)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 收到新消息操作
     */
    suspend fun onReceiveMessage(message: ChatMessage) {
        channel.send(MessageOption(Option.ADD_MSG, message))
    }

    /**
     * 收到消息删除操作
     */
    fun onDeleteMessage(message: ChatMessage) {
        channel.trySend(MessageOption(Option.REMOVE_MSG, message))
    }

    /**
     * 收到消息状态变化操作
     */
    fun onUpdateState(message: ChatMessage) {
        channel.trySend(MessageOption(Option.UPDATE_STATE, message))
    }

    /**
     * 收到消息内容变化操作
     */
    fun onUpdateContent(message: ChatMessage) {
        channel.trySend(MessageOption(Option.UPDATE_CONTENT, message))
    }

    /**
     * 消息发送者信息变化，尽量减少调用次数，以免阻塞新消息
     */
    fun onUpdateContact(contact: Contact) {
        channel.trySend(MessageOption(Option.UPDATE_CONTACT, contact))
    }

    /**
     * 消息操作
     */
    fun onMessage(option: Option, payload: Any) {
        channel.trySend(MessageOption(option, payload))
    }

    override fun onMessageConfirm(seqIdentifier: String, type: ConfirmType, extra: Bundle?) {
        GlobalScope.launch(Dispatchers.Main) {
            confirmChannel.send(MsgConfirmResult(seqIdentifier, type, extra))
        }
    }

    fun registerChannel(subscriber: Any, channel: SendChannel<MessageOption>) {
        channels[subscriber] = channel
    }

    fun unregisterChannel(subscriber: Any) {
        channels.remove(subscriber)
    }

    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "这个方法只能订阅即时消息通知，提前订阅并挂起",
        replaceWith = ReplaceWith("this.registerChannel(subscriber, channel)")
    )
    fun subscribeMessage(subscriber: Any, observer: (MessageOption) -> Unit) {
        subscribers[subscriber] = observer
    }

    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "",
        replaceWith = ReplaceWith("this.unregisterChannel(subscriber)")
    )
    fun unsubscribeMessage(subscriber: Any) {
        subscribers.remove(subscriber)
    }

    private var onShowNotificationListener: (suspend (ChatMessage) -> Unit)? = null

    fun setOnShowNotification(action: suspend (ChatMessage) -> Unit) {
        onShowNotificationListener = action
    }
}