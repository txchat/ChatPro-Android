package com.fzm.chat.router.redpacket

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/25
 * Description:
 */
data class ModuleAsset(
    /**
     * 币种中文名
     */
    val name: String?,
    /**
     * 币种链名称, user.p.xxx
     */
    val chain: String,
    /**
     * 是否是主代币
     */
    val isCoins: Boolean,
    /**
     * 币种名
     */
    val symbol: String,
    /**
     * 余额
     */
    var balance: String,
    /**
     * 币种精度（表示有几位小数）
     */
    val decimalPlaces: Int,
) : Serializable

private inline val ModuleAsset.isMainChain: Boolean
    get() = !chain.startsWith("user.p.")

inline val ModuleAsset.assetExec: String
    get() = if (isCoins) "coins" else "token"

val ModuleAsset.fullExec: String
    get() {
        return if (isMainChain) {
            assetExec
        } else {
            if (isCoins) "${chain}.coins" else "${chain}.token"
        }
    }

fun ModuleAsset.exexer(name: String): String = if (isMainChain) name else "${chain}.$name"