package com.fzm.arch.connection.socket

import androidx.annotation.IntDef

/**
 * @author zhengjy
 * @since 2019/09/04
 *
 * 连接状态类型
 */
class SocketState {

    companion object {
        /**
         * 初始化状态
         */
        const val INITIAL = 0
        /**
         * 正在连接状态
         */
        const val CONNECTING = 1
        /**
         * 连接断开状态
         */
        const val DISCONNECTED = 2
        /**
         * 连接建立成功，建立正常通讯
         */
        const val ESTABLISHED = 3
    }

    @IntDef(INITIAL, CONNECTING, DISCONNECTED, ESTABLISHED)
    @Retention(AnnotationRetention.SOURCE)
    annotation class State
}

/**
 * 连接状态变更监听
 */
interface SocketStateChangeListener {

    fun onSocketStateChange(@SocketState.State state: Int)
}

/**
 * 消息连接与seq组合成的唯一id，内存中使用
 */
inline fun socketIdentifier(socket: String, seq: Int) = "$socket-$seq"
