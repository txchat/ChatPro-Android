package com.fzm.chat.core.net.source

import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2021/10/08
 * Description:
 */
interface ChatDataSource {

    /**
     * 撤回消息
     *
     * @param type  类型，0撤回私聊消息 1撤回群聊消息
     * @param logId 消息id
     */
    suspend fun revokeMessage(type: Int, logId: Long): Result<Any>

    /**
     * 关注消息
     *
     * @param type  类型，0关注私聊消息 1关注群聊消息
     * @param logId 消息id
     */
    suspend fun focusMessage(type: Int, logId: Long): Result<Any>
}