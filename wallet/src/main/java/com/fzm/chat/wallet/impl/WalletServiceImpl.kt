package com.fzm.chat.wallet.impl

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.core.utils.CipherUtils
import com.fzm.chat.router.main.SimpleTx
import com.fzm.chat.router.redpacket.ModuleAsset
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.router.wallet.WalletService
import com.fzm.chat.wallet.WalletConfig
import com.fzm.chat.wallet.data.*
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.WalletConfiguration
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.decimalPlaces
import com.fzm.wallet.sdk.utils.fullChain
import com.fzm.wallet.sdk.utils.isBty
import com.fzm.wallet.sdk.utils.isCoin
import com.zjy.architecture.di.rootScope

/**
 * @author zhengjy
 * @since 2021/08/13
 * Description:
 */
@Route(path = WalletModule.SERVICE)
class WalletServiceImpl : WalletService {

    private val repository by rootScope.inject<WalletRepository>()

    override suspend fun importMnem(
        user: String,
        mnem: String,
        name: String,
        password: String
    ) {
        val wallet = BWallet.get().getAllWallet(user).firstOrNull()
        if (wallet == null) {
            val config = WalletConfiguration.mnemonicWallet(
                mnem,
                name,
                password.ifEmpty { CipherUtils.DEFAULT_PASSWORD },
                user,
                defaultCoins
            )
            BWallet.get().importWallet(config, true)
        } else {
            BWallet.get().changeWallet(wallet)
        }
    }

    override suspend fun closeWallet() {
        BWallet.get().close()
    }

    override fun getDefaultFee(): Double {
        return WalletConfig.DEFAULT_FEE
    }

    override suspend fun getTransactionByHash(chain: String, hash: String, symbol: String): Result<SimpleTx> {
        return try {
            val transactions = BWallet.get().getTransactionByHash(chain, symbol, hash)
            Result.success(transactions.let {
                SimpleTx(
                    hash,
                    it.blocktime,
                    it.fee ?: "0",
                    it.from ?: "",
                    it.to ?: "",
                    it.height,
                    it.status.toString(),
                    it.value ?: "0",
                    it.note
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getModuleAssets(address: String): List<ModuleAsset> {
        return repository.getAssetList(address).dataOrNull()
            ?.filter { it.isBty }
            ?.map {
                ModuleAsset(
                    it.nickname,
                    it.fullChain,
                    it.isCoin,
                    it.name ?: "",
                    it.balance ?: "",
                    it.decimalPlaces
                )
            } ?: emptyList()
    }

    override suspend fun updateChainAddress(address: Map<String, String>?): String {
        return if (address.isNullOrEmpty()) {
            val map = BWallet.get().getAllCoins().associate { Pair(it.chain, it.address) }
            repository.setChainAddress(map).dataOrNull() ?: ""
        } else {
            repository.setChainAddress(address).dataOrNull() ?: ""
        }
    }

    override fun init(context: Context?) {
        
    }
}

private val defaultCoins = listOf(
    Coin().apply {
        chain = "BTC"
        name = "BTC"
        netId = "89"
        platform = "btc"
        treaty = "1"
    },
    Coin().apply {
        chain = "ETH"
        name = "ETH"
        netId = "90"
        platform = "ethereum"
        treaty = "1"
    },
    Coin().apply {
        chain = "ETH"
        name = "YCC"
        netId = "155"
        platform = "ethereum"
        treaty = "1"
    },
    Coin().apply {
        chain = "YCC"
        name = "YCC"
        netId = "521"
        platform = "ycc"
        treaty = "1"
    },
    Coin().apply {
        chain = "BTY"
        name = "BTY"
        netId = "154"
        platform = "bty"
        treaty = "1"
    },
    Coin().apply {
        chain = "TRX"
        name = "USDT"
        netId = "600"
        platform = "trx"
        treaty = "1"
    },
    Coin().apply {
        chain = "ETH"
        name = "USDT"
        netId = "288"
        platform = "ethereum"
        treaty = "1"
    },
    Coin().apply {
        chain = "BTC"
        name = "USDT"
        netId = "358"
        platform = "omni"
        treaty = "1"
    },
    Coin().apply {
        chain = "BNB"
        name = "USDT"
        netId = "694"
        platform = "bnb"
        treaty = "1"
    },
    Coin().apply {
        chain = "ETC"
        name = "ETC"
        netId = "102"
        platform = "etc"
        treaty = "1"
    },
    Coin().apply {
        chain = "BNB"
        name = "BNB"
        netId = "641"
        platform = "bnb"
        treaty = "1"
    },
    Coin().apply {
        chain = "BCH"
        name = "BCH"
        netId = "444"
        platform = "bch"
        treaty = "1"
    },
    Coin().apply {
        chain = "BTY"
        name = "CCNY"
        netId = "237"
        platform = "bty"
        treaty = "1"
    },
    Coin().apply {
        chain = "LTC"
        name = "LTC"
        netId = "446"
        platform = "ltc"
        treaty = "1"
    },
    Coin().apply {
        chain = "ZEC"
        name = "ZEC"
        netId = "447"
        platform = "zec"
        treaty = "1"
    },
    Coin().apply {
        chain = "DCR"
        name = "DCR"
        netId = "111"
        platform = "dcr"
        treaty = "1"
    },
    Coin().apply {
        chain = "NEO"
        name = "NEO"
        netId = "530"
        platform = "neo"
        treaty = "1"
    },
    Coin().apply {
        chain = "ATOM"
        name = "ATOM"
        netId = "527"
        platform = "atom"
        treaty = "1"
    },
)