package com.fzm.chat.redpacket

import com.fzm.chat.router.redpacket.ModuleAsset
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.decimalPlaces
import com.fzm.wallet.sdk.utils.fullChain
import com.fzm.wallet.sdk.utils.isCoin
import com.zjy.architecture.util.AbstractConfig

/**
 * @author zhengjy
 * @since 2021/08/04
 * Description:
 */
object RedPacketConfig {

    private object WalletDev : AbstractConfig("redPacket-dev.properties")
    private object WalletPro : AbstractConfig("redPacket-pro.properties")

    private val config = if (BuildConfig.DEVELOP) WalletDev else WalletPro

    /**
     * 红包平行链
     */
    val FULL_CHAIN = config["FULL_CHAIN"]
}

fun Coin.toAsset() = ModuleAsset(
    nickname,
    fullChain,
    isCoin,
    name ?: "",
    balance ?: "",
    decimalPlaces
)