package com.fzm.chat.core.data.msg

/**
 * 消息发送的状态
 */
object MsgState {
    /**
     * 发送中
     */
    const val SENDING = 0

    /**
     * 发送失败
     */
    const val FAIL = 1

    /**
     * 已发送，未送达
     */
    const val SENT = 2

    /**
     * 已发送，已送达
     */
    const val SENT_AND_RECEIVE = 3
}