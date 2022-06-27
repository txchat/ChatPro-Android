package com.fzm.chat.wallet.data

import com.fzm.chat.wallet.data.bean.TransactionInfo
import com.fzm.wallet.sdk.db.entity.Coin
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2021/08/04
 * Description:
 */
interface WalletDataSource {

    /**
     * 转账方法（适用于多种主链，需要消耗手续费）
     *
     * @param target    转账对象
     * @param coin      币种
     * @param amount    转账数量
     * @param fee       矿工费
     * @param password  密码
     * @param note      上链备注
     *
     */
    suspend fun transfer(target: String, coin: Coin, amount: Double, fee: Double, password: String, note: String? = null): Result<String>

    /**
     * 获取资产余额
     *
     * @param address    查询地址
     *
     */
    suspend fun getAssetList(address: String): Result<List<Coin>>

    /**
     * 获取交易记录
     */
    suspend fun getTransactionList(coin: Coin, type: Int, index: Long): Result<List<TransactionInfo>>

    /**
     * 获取交易记录
     */
    suspend fun getTransactionById(chain: String, txId: String, tokenSymbol: String): Result<TransactionInfo?>

    /**
     * 上传用户主链地址
     */
    suspend fun setChainAddress(address: Map<String, String>): Result<String>
}