package com.fzm.chat.redpacket.impl

import com.fzm.chat.core.data.contract.ContractRequest
import com.fzm.chat.core.data.contract.apiCall2
import com.fzm.chat.core.net.source.TransactionSource
import com.fzm.chat.redpacket.data.RedPacketDataSource
import com.fzm.chat.redpacket.data.bean.*
import com.fzm.chat.redpacket.net.RedPacketService
import com.fzm.chat.router.redpacket.ModuleAsset
import com.fzm.chat.router.redpacket.exexer
import com.fzm.chat.router.redpacket.fullExec
import com.zjy.architecture.data.Result
import com.zjy.architecture.exception.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import walletapi.*

/**
 * @author zhengjy
 * @since 2021/08/25
 * Description:
 */
class NetRedPacketDataSource(
    private val transaction: TransactionSource,
    private val service: RedPacketService,
    private val client: ChainClient
) : RedPacketDataSource {

    override suspend fun transferRedPacket(asset: ModuleAsset, amount: Long): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Result.Success(client.transferToExec(amount, 0, asset.fullExec, asset.exexer("redpacket"), asset.symbol))
            }catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun withDrawRedPacket(
        assetExec: String,
        execer: String,
        symbol: String,
        amount: Long
    ): Result<String> {
        if (amount == 0L) {
            return Result.Error(ApiException("红包资产不足"))
        }
        return transaction.handleForRawHash(assetExec) {
            withContext(Dispatchers.IO) {
                try {
                    Result.Success(client.withdrawFromExec(amount, 0, assetExec, execer, symbol))
                }catch (e: Exception) {
                    Result.Error(e)
                }
            }
        }
    }

    override suspend fun sendRedPacket(params: SendRedPacketParams): Result<String> {
        return withContext(Dispatchers.IO) {
            val sendParams = SendRedPacket().apply {
                assetSymbol = params.assetSymbol
                assetExec = params.assetExec
                amount = params.amount
                size = params.size
                type = params.type
                decimalPlaces = params.decimalPlaces
                remark = params.remark
                signPubkey = params.signPubkey
                signPrikey = params.signPrikey
                params.toAddr.firstOrNull()?.also {
                    setToAddr(it)
                }
                expiresTime = params.expiresTime
            }
            try {
                Result.Success(client.sendRedPacketTx(sendParams))
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun receiveRedPacket(params: ReceiveRedPacketParams): Result<String> {
        return transaction.handleForRawHash("redpacket") {
            withContext(Dispatchers.IO) {
                val receive = ReceiveRedPacket().apply {
                    this.packetId = params.packetId
                    this.redpacketSign = RedpacketSign().also {
                        it.signature = params.redPacketSign.signature
                        it.ty = params.redPacketSign.ty
                    }
                }
                try {
                    Result.Success(client.receiveReadPacketTx(receive))
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }
        }
    }

    override suspend fun getRedPacketInfo(packetId: String): Result<RedPacketInfo> {
        val request = ContractRequest.createQuery(
            mapOf(
                "execer" to "redpacket",
                "funcName" to "GetRedPacketInfo",
                "payload" to mapOf("packetId" to packetId)
            )
        )
        val result = apiCall2 { service.getRedPacketInfo(request) }
        return if (result.isSucceed()) {
            Result.Success(result.data().redPacket)
        } else {
            Result.Error(result.error())
        }
    }

    override suspend fun getReceiveDetail(
        packetId: String,
        receiveId: String,
        count: Int
    ): Result<List<ReceiveInfo>> {
        val request = ContractRequest.createQuery(
            mapOf(
                "execer" to "redpacket",
                "funcName" to "GetReceiveDetail",
                "payload" to mapOf(
                    "packetId" to packetId,
                    "count" to count,
                    "receiveId" to receiveId
                )
            )
        )
        val result = apiCall2 { service.getReceiveDetail(request) }
        return if (result.isSucceed()) {
            Result.Success(result.data().receiveRecords)
        } else {
            Result.Error(result.error())
        }
    }

    override suspend fun getReceiveRecord(address: String, packetId: String): Result<ReceiveInfo> {
        val request = ContractRequest.createQuery(
            mapOf(
                "execer" to "redpacket",
                "funcName" to "GetReceiveRecord",
                "payload" to mapOf(
                    "address" to address,
                    "packetId" to packetId,
                )
            )
        )
        val result = apiCall2 { service.getReceiveRecords(request) }
        return if (result.isSucceed()) {
            Result.Success(result.data().receiveRecords.firstOrNull()?: ReceiveInfo.EMPTY)
        } else {
            Result.Error(result.error())
        }
    }

    override suspend fun getReceiveRecordList(
        address: String,
        receiveId: String,
        count: Int
    ): Result<List<ReceiveInfo>> {
        val request = ContractRequest.createQuery(
            mapOf(
                "execer" to "redpacket",
                "funcName" to "GetReceiveRecord",
                "payload" to mapOf(
                    "address" to address,
                    "count" to count,
                    "receiveId" to receiveId
                )
            )
        )
        val result = apiCall2 { service.getReceiveRecords(request) }
        return if (result.isSucceed()) {
            Result.Success(result.data().receiveRecords)
        } else {
            Result.Error(result.error())
        }
    }

    override suspend fun getStatisticRecordList(map: Map<String, String>): Result<StatisticInfo> {
        val request = ContractRequest.createQuery(
            mapOf(
                "execer" to "redpacket",
                "funcName" to "GetStatistic",
                "payload" to map
            )
        )
        return apiCall2 { service.getStatisticRecords(request) }
    }

    override suspend fun backRedPacket(packetId: String): Result<String> {
        return transaction.handleForRawHash("redpacket") {
            withContext(Dispatchers.IO) {
                val back = BackRedPacket().apply { this.packetId = packetId }
                try {
                    Result.Success(client.backReadPacketTx(back))
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }
        }
    }
}