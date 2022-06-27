package com.fzm.arch.connection

import android.os.Bundle
import com.fzm.arch.connection.type.ConfirmType

/**
 * @author zhengjy
 * @since 2021/03/12
 * Description:
 */
interface OnMessageConfirmCallback {

    /**
     * 消息被服务端成功确认，如果不需要确认则直接返回成功
     *
     * @param seqIdentifier 消息唯一识别id，格式：${socket.hashCode()}-$seq
     * @param type          消息确认状态
     * @param extra         业务层需要的额外信息
     */
    fun onMessageConfirm(seqIdentifier: String, type: ConfirmType, extra: Bundle?)
}