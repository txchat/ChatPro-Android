package com.fzm.chat.core.net.api

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.rtc.data.RTCRoomParams
import com.fzm.chat.core.rtc.data.RTCTaskTO
import com.zjy.architecture.net.HttpResult
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * @author zhengjy
 * @since 2021/07/14
 * Description:
 */
@JvmSuppressWildcards
interface RTCService {

    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CHAT_DOMAIN])
    @POST("/call/app/start-call")
    suspend fun call(@Body map: Map<String, Any>): HttpResult<RTCTaskTO>

    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CHAT_DOMAIN])
    @POST("/call/app/check-call")
    suspend fun checkCall(@Body map: Map<String, Any>): HttpResult<RTCTaskTO>

    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CHAT_DOMAIN])
    @POST("/call/app/handle-call")
    suspend fun handleCall(@Body map: Map<String, Any>): HttpResult<RTCRoomParams>

    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CHAT_DOMAIN])
    @POST("/call/app/reply-busy")
    suspend fun lineBusy(@Body map: Map<String, Any>): HttpResult<Any>
}