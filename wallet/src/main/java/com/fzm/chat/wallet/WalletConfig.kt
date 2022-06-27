package com.fzm.chat.wallet

import com.zjy.architecture.util.AbstractConfig

/**
 * @author zhengjy
 * @since 2021/08/04
 * Description:
 */
object WalletConfig {

    private object WalletDev : AbstractConfig("wallet-dev.properties")
    private object WalletPro : AbstractConfig("wallet-pro.properties")

    private val config = if (BuildConfig.DEVELOP) WalletDev else WalletPro

    /**
     * 默认手续费
     */
    val DEFAULT_FEE = config["DEFAULT_FEE"].toDoubleOrNull() ?: 0.001
}