package com.fzm.chat.router.wallet

/**
 * @author zhengjy
 * @since 2021/08/03
 * Description:
 */
object WalletModule {

    private const val GROUP = "/wallet"

    const val INJECTOR = "$GROUP/injector"

    const val SERVICE = "$GROUP/service"

    const val WEB = "${GROUP}/web"

    const val WALLET = "${GROUP}/wallet_fragment"

    const val TRANSFER = "$GROUP/transfer"

    const val WALLET_INDEX = "$GROUP/wallet_index"

    const val TRANSACTION_DETAIL = "$GROUP/transaction_detail"

    const val ASSET_DETAIL = "$GROUP/asset_detail"

    const val WALLET_CODE = "$GROUP/wallet_code"

    const val DRAWER_ASSETS = "$GROUP/drawer_assets"

    const val ADD_COINS = "$GROUP/add_coins"

    const val SEARCH_COINS = "$GROUP/search_coins"
}