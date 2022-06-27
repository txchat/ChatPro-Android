package com.fzm.chat.redpacket.data.bean

import com.fzm.chat.core.data.bean.Contact
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/27
 * Description:
 */
data class RedPacketInfo(
    /**
     * 红包编号
     */
    val packetId: String,
    /**
     * 发红包者的地址
     */
    val addr: String,
    /**
     * 红包类型
     * 1:随机红包2:固定金额红包
     */
    val type: Int,
    /**
     * 币种所属合约
     */
    val assetExec: String,
    /**
     * 币种名
     */
    val assetSymbol: String,
    /**
     * 金额
     */
    val amount: Long,
    /**
     * 红包总个数
     */
    val size: Int,
    /**
     * 剩余红包个数
     */
    val remain: Int,
    /**
     * 红包接收用户
     */
    val toAddr: List<String>,
    /**
     * 红包状态
     * 1:待领 2:已领完 3:已退回 过期以过期时间为准
     */
    val status: Int,
    /**
     * 发红包的客户端随机生成公私钥,将公钥填入该参数，私钥分享给领红包者
     * 对于客户端无用处
     */
    val signPubkey: String,
    /**
     * 红包备注
     */
    val remark: String,
    /**
     * 创建时间，单位秒
     */
    val createTime: Long,
    /**
     * 更新时间，单位秒
     */
    val updatedTime: Long,
    /**
     * 过期时间，单位秒
     */
    val expiresTime: Long,
    /**
     * 币种精度
     */
    val decimalPlaces: Int,
    /**
     * 用于分页查询
     */
    val txIndex: String,
) : Serializable {

    /**
     * 红包发送者信息
     */
    var sender: Contact? = null

    companion object {
        /**
         * 假红包信息，只可用于忽略红包状态，直接进行抢红包
         */
        internal val FAKE_PACKET = RedPacketInfo("", "", 0, "", "",
            0L, 0, Int.MAX_VALUE, emptyList(), 1, "", "",
            0L, 0L, Long.MAX_VALUE, 0, "")
    }

    data class Wrapper(
        val redPacket: RedPacketInfo
    ) : Serializable
}

val RedPacketInfo.isExpired: Boolean get() = expiresTime != Long.MAX_VALUE && expiresTime * 1000 < System.currentTimeMillis()