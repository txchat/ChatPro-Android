package com.fzm.chat.redpacket.data.bean

import com.fzm.chat.router.redpacket.ModuleAsset
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/25
 * Description:
 */
data class SendRedPacketParams(
    /**
     * 红包总金额，需要放大1e8倍
     */
    val amount: Long,
    /**
     * 币种合约名
     */
    val assetExec: String,
    /**
     * 币种名
     */
    val assetSymbol: String,
    /**
     * 币种精度
     */
    val decimalPlaces: Int,
    /**
     * 过期时间，默认一天后
     */
    val expiresTime: Long,
    /**
     * 红包备注
     */
    val remark: String,
    /**
     * 发红包的客户端随机生成公私钥,将用自身公钥加密后的红包私钥填入该参数，用于发红包者解析分享
     */
    val signPrikey: String?,
    /**
     * 发红包的客户端随机生成公私钥,将公钥填入该参数,私钥分享给领红包者
     */
    val signPubkey: String?,
    /**
     * 红包个数
     */
    val size: Int,
    /**
     * 红包接受者地址
     */
    val toAddr: List<String>,
    /**
     * 红包类型
     * 1:随机红包2:固定金额红包
     */
    val type: Int
) : Serializable {

    @Transient
    var moduleAsset: ModuleAsset? = null
}

val SendRedPacketParams.bizPacketType: Int get() = if (type == 1) 0  else 1

val SendRedPacketParams.isCoins: Boolean get() = assetExec == "coins"