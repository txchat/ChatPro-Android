package com.fzm.chat.redpacket.data.bean

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/26
 * Description:
 */
data class ReceiveRedPacketParams(
    /**
     * 红包id
     */
    val packetId: String,
    @SerializedName("redpacketSign")
    val redPacketSign: RedPacketSign,
    /**
     * 平行链名称
     */
    @Transient
    val chain: String
) : Serializable {

    data class RedPacketSign(
        /**
         * 用该红包的私钥对自己地址进行签名
         */
        val signature: String,
        val ty: Int = 1
    ) : Serializable
}

