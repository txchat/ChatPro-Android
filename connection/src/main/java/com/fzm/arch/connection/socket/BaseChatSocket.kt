package com.fzm.arch.connection.socket

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * @author zhengjy
 * @since 2021/02/24
 * Description:
 */
abstract class BaseChatSocket : ChatSocket {

    protected val listeners = mutableListOf<ChatSocketListener>()

    /**
     * 重连重试次数
     */
    protected var reconnectTimes = 0

    @SocketState.State
    var _state: Int = SocketState.INITIAL
    protected val _stateLive = MutableLiveData(SocketState.INITIAL)

    override val state: LiveData<Int>
        get() = _stateLive

    override val isAlive: Boolean
        get() = _state == SocketState.INITIAL || _state == SocketState.ESTABLISHED || reconnectTimes <= MAX_RECONNECT_PERMITTED

    /**
     * 注册socket消息回调
     */
    override fun register(listener: ChatSocketListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    /**
     * 解除socket消息回调
     */
    override fun unregister(listener: ChatSocketListener) {
        listeners.remove(listener)
    }

    override fun ack(seq: Int, data: ByteArray) {

    }

    companion object {

        internal const val MSG_CALLBACK = 1
        internal const val OPEN_CALLBACK = 2
        internal const val CLOSE_CALLBACK = 3

        /**
         * 判断为断开连接前最大允许重连次数
         */
        internal const val MAX_RECONNECT_PERMITTED = 3
    }
}