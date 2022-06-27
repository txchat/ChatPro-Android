package com.fzm.arch.connection.socket

import android.os.Bundle
import androidx.lifecycle.LiveData

/**
 * @author zhengjy
 * @since 2020/12/04
 * Description:
 */
interface ChatSocket {

    /**
     * 注册socket消息回调
     */
    fun register(listener: ChatSocketListener)

    /**
     * 解除socket消息回调
     */
    fun unregister(listener: ChatSocketListener)

    /**
     * 建立连接
     */
    fun connect()

    /**
     * 前后台切换
     */
    fun onForeground(foreground: Boolean)

    /**
     * 释放连接
     */
    fun release()

    /**
     * 发送字节数组消息
     *
     * @return 消息唯一标识
     */
    fun send(message: ByteArray, extra: Bundle?): String?

    /**
     * 确认消息
     */
    fun ack(seq: Int, data: ByteArray)

    /**
     * 连接是否存活（用户感知层面，因此正在初始化和正在连接也视为连接存活）
     */
    val isAlive: Boolean

    /**
     * 连接状态
     */
    val state: LiveData<Int>

    /**
     * 连接标识
     */
    override fun toString(): String
}