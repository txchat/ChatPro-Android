package com.fzm.chat.core.net.source

import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.SendTxParams
import com.fzm.chat.core.data.contract.ContractRequest
import com.fzm.chat.core.data.bean.TransactionParams
import com.fzm.chat.core.data.bean.TransactionResult
import com.fzm.chat.core.data.contract.ContractResponse
import com.fzm.chat.core.data.contract.apiCall2
import com.fzm.chat.core.data.execerPattern
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.mul
import com.fzm.chat.router.biz.BizModule
import com.fzm.chat.router.biz.BizService
import com.fzm.chat.router.route
import com.google.gson.Gson
import com.zjy.architecture.data.Result
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.exception.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import walletapi.ChainClient
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/14
 * Description:
 */
class TransactionSource(
    private val service: TransactionDataSource,
    private val client: ChainClient
) {

    private val delegate: LoginDelegate by rootScope.inject()
    private val gson by rootScope.inject<Gson>()
    private val bizService by route<BizService>(BizModule.SERVICE)

    /**
     * 查询交易信息
     */
    suspend fun queryTransaction(txHash: String): Result<TransactionResult> {
        val request = ContractRequest.create(
            "Chain33.QueryTransaction", mapOf("hash" to txHash)
        )
        return apiCall2 { service.queryTransaction(request) }
    }

    /**
     * 创建代扣交易
     */
    suspend fun createNoBalanceTx(private: String, txHex: String): Result<String> {
        val request = ContractRequest.create(
            "Chain33.CreateNoBalanceTransaction",
            TransactionParams.createNoBalanceTx(private, txHex)
        )
        return apiCall2 { service.contractRequest(request) }
    }

    //************************需要正确设置ChainClient才能使用的方法************************//
    /**
     * 创建包含代扣交易的交易组
     */
    suspend fun createTxGroup(tx1: String, tx2: String, fee: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Result.Success(client.creatTxGroup(tx1, tx2, fee))
            }catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    /**
     * 创建包含代扣交易的交易组
     */
    suspend fun createTxGroup(tx1: String, tx2: String, fee: Double): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Result.Success(client.creatTxGroup(tx1, tx2, fee.toLong()))
            }catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    /**
     * 签名交易组
     */
    suspend fun signTxGroup(private: String, txHex: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Result.Success(client.signTxGroup(private, txHex))
            } catch (e: Exception) {
                Result.Error(ApiException("签名失败:${e.message}"))
            }
        }
    }
    //************************需要正确设置ChainClient才能使用的方法************************//

    /**
     * 解析交易
     */
    suspend fun decodeRawTransaction(txHex: String): Result<TxPack> {
        return withContext(Dispatchers.IO) {
            try {
                val hex = if (txHex.startsWith("0x") || txHex.startsWith("0X")) {
                    txHex.substring(2)
                } else {
                    txHex
                }
                Result.Success(gson.fromJson(client.decodeTx(hex), TxPack::class.java))
            }catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    suspend fun getProperFee(txCount: Int, txSize: Int): Result<ProperFee> {
        val request = ContractRequest.create(
            "Chain33.GetProperFee",
            mapOf("txCount" to txCount, "txSize" to txSize)
        )
        return apiCall2 { service.getProperFee(request) }
    }

    /**
     * 签名交易
     */
    suspend fun signRawTx(private: String, txHex: String, fee: Long = 0L): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Result.Success(client.signTransactionGroup(2, txHex, private))
            } catch (e: Exception) {
                Result.Error(ApiException("签名失败:${e.message}"))
            }
        }
    }

    /**
     * 发送交易
     */
    suspend fun sendTransaction(data: String): Result<String> {
        val request = ContractRequest.create("Chain33.SendTransaction", SendTxParams(data))
        return apiCall2 { service.contractRequest(request) }
    }

    suspend fun handle(action: suspend () -> Result<String>): Result<String> {
        return handle(delegate.preference.PRI_KEY, action)
    }

    /**
     * 签名发送交易，请求完合约接口之后，进行签名和发送交易
     */
    suspend fun handle(privateKey: String, action: suspend () -> Result<String>): Result<String> {
        val baseFee = 0.001.mul(bizService?.getAmountScale() ?: 10000_0000)
        var totalFee = baseFee * 2
        val channel = Channel<Result<String>>(1)
        flow {
            emit(action())
        }.map {
            if (it.isSucceed()) {
                createNoBalanceTx(ChatConfig.NO_BALANCE_PRIVATE_KEY, it.data())
            } else {
                it
            }
//        }.map {
//            if (it.isSucceed()) {
//                val size = it.data().hex2Bytes().size
//                val fee = getProperFee(2, size)
//                if (fee.isSucceed()) {
//                    if (fee.data().properFee > 1000000) {
//                        // 超过10倍费率，则不发交易
//                        return@map Result.Error(ApiException("区块链繁忙，请稍后再试"))
//                    }
//                    totalFee = fee.data().properFee * ((size / 1024).coerceAtLeast(1) + 1)
//                }
//                it
//            } else {
//                it
//            }
        }.map {
            if (it.isSucceed()) {
                signRawTx(privateKey, it.data(), totalFee.toLong())
            } else {
                Result.Error(it.error())
            }
        }.map {
            if (it.isSucceed()) {
                sendTransaction(it.data())
            } else {
                it
            }
        }.collect {
            channel.send(it)
        }
        return channel.receive()
    }

    suspend fun handleForRawHash(
        execer: String,
        action: suspend () -> Result<String>
    ): Result<String> {
        return handleForRawHash(execer, delegate.preference.PRI_KEY, action)
    }

    /**
     * 签名发送交易，请求完合约接口之后，进行签名和发送交易
     *
     * 用于返回原始交易hash，钱包转账中需要用到
     */
    suspend fun handleForRawHash(
        execer: String,
        privateKey: String,
        action: suspend () -> Result<String>
    ): Result<String> {
        var totalFee = 100000L
        var rawHash = ""
        val channel = Channel<Result<String>>(1)
        flow {
            emit(action())
        }.map {
            if (it.isSucceed()) {
                createNoBalanceTx(ChatConfig.NO_BALANCE_PRIVATE_KEY, it.data())
            } else {
                it
            }
        }.map {
            if (it.isSucceed()) {
                val result = decodeRawTransaction(it.data())
                if (result.isSucceed()) {
                    val pattern = execerPattern(execer).toRegex()
                    for (tx in result.data().txs) {
                        if (pattern.matches(tx.execer) || tx.execer == execer) {
                            rawHash = tx.hash
                            break
                        }
                    }
                    if (rawHash.isEmpty()) {
                        return@map Result.Error(Exception("查询失败，原始交易hash为空"))
                    }
                    return@map it
                } else {
                    return@map Result.Error(result.error())
                }
            } else {
                return@map it
            }
//        }.map {
//            if (it.isSucceed()) {
//                val size = it.data().hex2Bytes().size
//                val fee = getProperFee(2, size)
//                if (fee.isSucceed()) {
//                    if (fee.data().properFee > 1000000) {
//                        // 超过10倍费率，则不发交易
//                        return@map Result.Error(ApiException("区块链繁忙，请稍后再试"))
//                    }
//                    totalFee = fee.data().properFee * ((size / 1024).coerceAtLeast(1) + 1)
//                }
//                it
//            } else {
//                it
//            }
        }.map {
            if (it.isSucceed()) {
                signRawTx(privateKey, it.data(), totalFee)
            } else {
                it
            }
        }.map {
            if (it.isSucceed()) {
                sendTransaction(it.data())
            } else {
                it
            }
        }.collect {
            if (it.isSucceed()) {
                // 请求成功则返回原始交易的hash
                channel.send(Result.Success(rawHash))
            } else {
                channel.send(Result.Error(it.error()))
            }
        }
        return channel.receive()
    }

    suspend fun handleForRawHash2(
        execer: String,
        action: suspend () -> Result<String>
    ): Result<String> {
        return handleForRawHash2(execer, delegate.preference.PRI_KEY, action)
    }

    suspend fun handleForRawHash2(
        execer: String,
        privateKey: String,
        action: suspend () -> Result<String>
    ): Result<String> {
        var rawHash = ""
        val channel = Channel<Result<String>>(1)
        flow {
            emit(action())
        }.map {
            if (it.isSucceed()) {
                val result = decodeRawTransaction(it.data())
                if (result.isSucceed()) {
                    val pattern = execerPattern(execer).toRegex()
                    for (tx in result.data().txs) {
                        if (pattern.matches(tx.execer) || tx.execer == execer) {
                            rawHash = tx.hash
                            break
                        }
                    }
                    if (rawHash.isEmpty()) {
                        return@map Result.Error(Exception("查询失败，原始交易hash为空"))
                    }
                    return@map it
                } else {
                    return@map Result.Error(result.error())
                }
            } else {
                return@map it
            }
        }.map {
            if (it.isSucceed()) {
                signTxGroup(privateKey, it.data())
            } else {
                it
            }
        }.map {
            if (it.isSucceed()) {
                sendTransaction(it.data())
            } else {
                it
            }
        }.collect {
            if (it.isSucceed()) {
                // 请求成功则返回原始交易的hash
                channel.send(Result.Success(rawHash))
            } else {
                channel.send(Result.Error(it.error()))
            }
        }
        return channel.receive()
    }

    internal class ChatTransactionDelegate(private val service: ChatTransactionService) : TransactionDataSource {
        override suspend fun contractRequest(request: ContractRequest): ContractResponse<String> {
            return service.contractRequest(request)
        }

        override suspend fun decodeRawTransaction(request: ContractRequest): ContractResponse<TxPack> {
            return service.decodeRawTransaction(request)
        }

        override suspend fun getProperFee(request: ContractRequest): ContractResponse<ProperFee> {
            return service.getProperFee(request)
        }

        override suspend fun queryTransaction(request: ContractRequest): ContractResponse<TransactionResult> {
            return service.queryTransaction(request)
        }
    }

    internal interface ChatTransactionService {

        /**
         * 1.创建代扣交易
         * 2.签名交易
         * 3.发送交易
         */
        @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
        @POST(".")
        suspend fun contractRequest(@Body request: ContractRequest): ContractResponse<String>

        /**
         * 解析交易数据
         */
        @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
        @POST(".")
        suspend fun decodeRawTransaction(@Body request: ContractRequest): ContractResponse<TxPack>

        /**
         * 解析交易数据
         */
        @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
        @POST(".")
        suspend fun getProperFee(@Body request: ContractRequest): ContractResponse<ProperFee>

        /**
         * 查询交易详情
         */
        @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CONTRACT_DOMAIN])
        @POST(".")
        suspend fun queryTransaction(@Body request: ContractRequest): ContractResponse<TransactionResult>
    }

    data class ProperFee(
        val properFee: Long
    ) : Serializable

    data class TxPack(
        val txs: List<Tx>
    ) : Serializable

    data class Tx(
        val execer: String,
//        val expire: Long,
//        val fee: Long,
//        val feefmt: String,
//        val from: String,
//        val groupCount: Int,
        val hash: String,
//        val header: String,
//        val next: String,
//        val nonce: String,
//        val payload: Payload?,
//        val rawPayload: String,
//        val signature: Signature,
//        val to: String,
    ) : Serializable

//    data class Payload(
//        val tansfer: Transfer,
//        val ty: Int
//    ) : Serializable
//
//    data class Transfer(
//        val cointoken: String,
//        val amount: String,
//        val note: String,
//        val to: String
//    ) : Serializable
//
//    data class Signature(
//        val pubkey: String,
//        val signature: String,
//        val ty: Int
//    ) : Serializable
}