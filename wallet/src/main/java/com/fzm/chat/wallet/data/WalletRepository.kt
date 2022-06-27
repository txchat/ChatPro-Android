package com.fzm.chat.wallet.data

import com.fzm.chat.biz.base.AppConst
import com.fzm.chat.core.data.local.MemoryContactSource
import com.fzm.chat.wallet.data.bean.TransactionInfo
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.db.entity.Coin
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2021/08/04
 * Description:
 */
class WalletRepository(
    private val dataSource: WalletDataSource,
    private val memorySource: MemoryContactSource
): WalletDataSource by dataSource {

    override suspend fun transfer(
        target: String,
        coin: Coin,
        amount: Double,
        fee: Double,
        password: String,
        note: String?
    ): Result<String> {
        return try {
            val tx = BWallet.get().transfer(coin, target, amount, fee, note, password)
            Result.Success(tx)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getTransactionList(coin: Coin, type: Int, index: Long): Result<List<TransactionInfo>> {
        try {
            val txs = BWallet.get().getTransactionList(coin, type.toLong(), index, AppConst.PAGE_SIZE.toLong())
            return Result.Success(txs.map { tx ->
                TransactionInfo(
                    tx.txid ?: "",
                    tx.blocktime,
                    tx.fee ?: "0",
                    tx.from ?: "",
                    tx.to ?: "",
                    tx.height,
                    tx.status.toString(),
                    tx.value ?: "0",
                    tx.note
                ).apply {
                    contact = memorySource.friendMap[getAddress()]
                }
            })
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }

    override suspend fun getTransactionById(
        chain: String,
        txId: String,
        tokenSymbol: String
    ): Result<TransactionInfo> {
        return try {
            val tx = BWallet.get().getTransactionByHash(chain, tokenSymbol, txId)
            val info = TransactionInfo(
                txId,
                tx.blocktime,
                tx.fee ?: "0",
                tx.from ?: "",
                tx.to ?: "",
                tx.height,
                tx.status.toString(),
                tx.value ?: "0",
                tx.note
            )
            info.contact = memorySource.friendMap[info.getAddress()]
            Result.Success(info)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}