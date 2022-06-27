package com.fzm.chat.core.net.api

import com.fzm.chat.core.data.ChatConst
import com.zjy.architecture.net.HttpResult
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * @author zhengjy
 * @since 2021/10/08
 * Description:
 */
@JvmSuppressWildcards
interface ChatService {

    /**
     * 撤回消息
     *
     * @param map type：类型，0撤回私聊消息 1撤回群聊消息
     *            logId：消息id
     */
    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CHAT_DOMAIN])
    @POST("/app/record/revoke")
    suspend fun revokeMessage(@Body map: Map<String, Any>): HttpResult<Any>

    /**
     * 关注消息
     *
     * @param map type：类型，0撤回私聊消息 1撤回群聊消息
     *            logId：消息id
     */
    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CHAT_DOMAIN])
    @POST("/app/record/focus")
    suspend fun focusMessage(@Body map: Map<String, Any>): HttpResult<Any>
}