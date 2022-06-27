package com.fzm.arch.connection.socket

import com.zjy.architecture.exception.ApiException

/**
 * @author zhengjy
 * @since 2019/09/04
 * Description:连接消息与状态回调
 */
interface ChatSocketListener {

    /**
     * 收到连接消息
     *
     * @param url   收到消息的连接
     * @param msg   消息
     */
    fun onMessage(url: String, msg: ByteArray)

    /**
     * 连接建立回调
     *
     * @param url           打开的连接
     */
    fun onOpen(url: String)

    /**
     * 连接关闭回调
     *
     * @param url   关闭的连接
     * @param e     连接关闭原因
     */
    fun onClose(url: String, e: ApiException)
}
