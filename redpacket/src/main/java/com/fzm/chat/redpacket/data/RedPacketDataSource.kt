package com.fzm.chat.redpacket.data

import com.fzm.chat.redpacket.data.bean.*
import com.fzm.chat.router.redpacket.ModuleAsset
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2021/08/25
 * Description:
 */
interface RedPacketDataSource {

    /**
     * 资金转移到红包合约中，用于发红包前准备
     */
    suspend fun transferRedPacket(asset: ModuleAsset, amount: Long): Result<String>

    /**
     * 将资金从红包合约中退回，红包发送失败时需要退回
     */
    suspend fun withDrawRedPacket(assetExec: String, execer: String, symbol: String, amount: Long): Result<String>

    /**
     * 发红包方法
     *
     */
    suspend fun sendRedPacket(params: SendRedPacketParams): Result<String>

    /**
     * 收红包，抢红包方法
     */
    suspend fun receiveRedPacket(params: ReceiveRedPacketParams): Result<String>

    /**
     * 查询红包详细信息
     */
    suspend fun getRedPacketInfo(packetId: String): Result<RedPacketInfo>

    /**
     * 查询某个红包的所有领取记录
     */
    suspend fun getReceiveDetail(packetId: String, receiveId: String, count: Int): Result<List<ReceiveInfo>>

    /**
     * 查询某个红包的的领取记录
     */
    suspend fun getReceiveRecord(address: String, packetId: String): Result<ReceiveInfo>

    /**
     * 按指定条件查询我的领取记录
     */
    suspend fun getReceiveRecordList(address: String, receiveId: String, count: Int): Result<List<ReceiveInfo>>

    /**
     * 红包统计信息(包括收、发红包)
     */
    suspend fun getStatisticRecordList(map: Map<String, String>): Result<StatisticInfo>

    /**
     * 退回红包
     */
    suspend fun backRedPacket(packetId: String): Result<String>
}