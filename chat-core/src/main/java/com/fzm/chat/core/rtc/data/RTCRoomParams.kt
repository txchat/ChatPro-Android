package com.fzm.chat.core.rtc.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/07/14
 * Description:
 */
class RTCRoomParams(
    /**
     * 腾讯云音视频应用 ID
     */
    val sdkAppId: Int,
    /**
     * 房间id
     */
    val roomId: Int,
    /**
     * 用户签名，有了签名才能使用TRTC
     */
    @SerializedName("userSig")
    val signature: String? = null,
    /**
     * privateKey可以指定用户进入某个房间的权限
     */
    @SerializedName("privateMapKey")
    val privateMapKey: String?,
) : Serializable