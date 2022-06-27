package com.fzm.chat.wallet.impl

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.Field
import com.fzm.chat.core.data.contract.ContractRequest
import com.fzm.chat.core.data.contract.apiCall2
import com.fzm.chat.core.net.source.TransactionSource
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.session.UserDataSource
import com.fzm.chat.wallet.data.WalletDataSource
import com.fzm.chat.wallet.data.bean.*
import com.fzm.chat.wallet.net.WalletService
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.db.entity.Coin
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2021/08/04
 * Description:
 */
class NetWalletDataSource(
    private val delegate: LoginDelegate,
    private val transaction: TransactionSource,
    private val userSource: UserDataSource,
    private val service: WalletService
) : WalletDataSource {

    private suspend fun createRawTransaction(
        target: String,
        execer: String,
        assets: String,
        amount: Double,
        fee: Double,
        isToken: Boolean,
        note: String?
    ): Result<String> {
        val request = ContractRequest.create(
            "Chain33.CreateRawTransaction",
            TransferParams.createRawTransaction(target, execer, assets, amount, fee, isToken, note)
        )
        return apiCall2 { service.transfer(request) }
    }

    override suspend fun transfer(
        target: String,
        coin: Coin,
        amount: Double,
        fee: Double,
        password: String,
        note: String?
    ): Result<String> {
        throw UnsupportedOperationException("使用Native方法")
    }

    override suspend fun getAssetList(address: String): Result<List<Coin>> {
        return try {
            Result.Success(BWallet.get().getAllCoins().filter { it.status == Coin.STATUS_ENABLE })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    @Deprecated("使用Native方法")
    override suspend fun getTransactionList(
        coin: Coin,
        type: Int,
        index: Long
    ): Result<List<TransactionInfo>> {
        throw UnsupportedOperationException("使用Native方法")
    }

    @Deprecated("使用Native方法")
    override suspend fun getTransactionById(
        chain: String,
        txId: String,
        tokenSymbol: String
    ): Result<TransactionInfo?> {
        throw UnsupportedOperationException("使用Native方法")
    }

    override suspend fun setChainAddress(address: Map<String, String>): Result<String> {
        val user = delegate.current.value ?: return Result.Error(Exception("用户信息为空"))
        val fields = mutableListOf<Field>()
        for ((k, v) in address) {
            if (!user.chainAddress.containsKey(k)) {
                fields.add(Field("${ChatConst.UserField.CHAIN_PREFIX}$k", v, 1))
            }
        }
        return if (fields.isNotEmpty()) {
            val result = userSource.setUserInfo(fields)
            if (result.isSucceed()) {
                delegate.updateInfo {
                    fields.forEach {
                        chainAddress[it.name.substring(ChatConst.UserField.CHAIN_PREFIX.length)] = it.value
                    }
                }
            }
            result
        } else {
            Result.Success("")
        }
    }
}