package com.fzm.chat.core.repo

import com.fzm.chat.core.net.api.ChatService
import com.fzm.chat.core.net.source.ChatDataSource
import com.zjy.architecture.data.Result
import com.zjy.architecture.ext.apiCall

/**
 * @author zhengjy
 * @since 2021/10/08
 * Description:
 */
class ChatRepository(private val service: ChatService) : ChatDataSource {

    override suspend fun revokeMessage(type: Int, logId: Long): Result<Any> {
        return apiCall { service.revokeMessage(mapOf("type" to type, "logId" to logId)) }
    }

    override suspend fun focusMessage(type: Int, logId: Long): Result<Any> {
        return apiCall { service.focusMessage(mapOf("type" to type, "logId" to logId)) }
    }
}