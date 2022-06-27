package com.fzm.chat.wallet.data

import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.transferCode
import com.fzm.chat.core.data.AppPreference
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.isCoin
import dtalk.biz.msg.Msg

/**
 * @author zhengjy
 * @since 2022/01/11
 * Description:
 */
// 扩展字段移至SDK内部
//inline val Coin.isBty: Boolean get() = chain == "BTY"
//
//inline val Coin.isBtyChild: Boolean get() = isBty && platform != "bty"
//
//inline val Coin.isBtyCoins: Boolean get() = isBtyChild && isCoin
//
//inline val Coin.isBtyToken: Boolean get() = isBtyChild && !isCoin
//
///**
// * 判断是coins还是token
// */
//inline val Coin.isCoin: Boolean get() = treaty == "2"
//
//val Coin.tokenSymbol: String
//    get() {
//        return when {
//            isBtyCoins -> "$platform.coins"
//            isBtyToken -> "$platform.$name"
//            else -> name
//        }
//    }
//
///**
// * 币种精度
// */
//inline val Coin.decimalPlaces: Int get() = 2
//
///**
// * 币种唯一表识符
// */
//inline val Coin.uid: String get() = "$chain-$name-$platform"
//
///**
// * 完整链名
// */
//inline val Coin.fullChain: String get() = if (isBtyChild) "user.p.$platform" else chain
//
///**
// * 红包等需要的执行器
// */
//inline val Coin.fullExec: String get() = if (isCoin) "$fullChain.coins" else "$fullChain.token"
//
///**
// * 红包等需要的执行器
// */
//inline val Coin.assetExec: String get() = if (isCoin) "coins" else "token"

/**
 * 获取币种转账二维码字符串
 */
inline val Coin.qrCode: String get() = AppConfig.transferCode(AppPreference.ADDRESS, address, chain, platform)

inline val Coin.coinType: Int get() = if (isCoin) Msg.CoinType.Coins_VALUE else Msg.CoinType.Token_VALUE

fun Coin.copy() = Coin().also {
    it.netId = netId
    it.address = address
    it.balance = balance
    it.chain = chain
    it.platform = platform
    it.name = name
    it.nickname = nickname
    it.optionalName = optionalName
    it.treaty = treaty
    it.status = status
    it.icon = icon
    it.sort = sort
    it.rmb = rmb
    it.setpWallet(getpWallet())
}