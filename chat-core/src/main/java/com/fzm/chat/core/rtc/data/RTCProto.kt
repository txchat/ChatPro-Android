package com.fzm.chat.core.rtc.data

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/07/14
 * Description:
 */
class RTCProto(
    /**
     * 通话id
     */
    val traceId: Long,
    /**
     * 通话房间id
     */
    val roomId: Int,
    /**
     * 通知类型
     */
    val actionType: Int,
    /**
     * 用户签名，有了签名才能使用TRTC
     */
    val signature: String = "",
    /**
     * privateKey可以指定用户进入某个房间的权限
     */
    val privateMapKey: String? = null,
    /**
     * privateKey可以指定用户进入某个房间的权限
     */
    val sdkAppId: Int = 0,
    /**
     * 通话结束原因
     */
    val reason: Int = -1
) : Serializable