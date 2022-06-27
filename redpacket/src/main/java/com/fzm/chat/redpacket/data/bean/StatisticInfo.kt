package com.fzm.chat.redpacket.data.bean

import java.io.Serializable

data class StatisticInfo(
    /**
     * 总结果数
     */
    val totalCount: Int,
    /**
     * 红包总额(仅当币种、收发动作确定时，才能确定总额，该项无效时返回-1)
     */
    val sum: Long,
    /**
     * 有效结果数(即总结果数-失败状态数，在收红包动作下有入账失败的情况)
     */
    val count: Int,
    /**
     * 收发动作
     * 1: 发红包  2：收红包
     */
    val operation: Int,
    /**
     * 收红包信息列表
     */
    val receiveRecords: List<ReceiveInfo>?,
    /**
     * 发红包信息列表
     */
    val redPackets: List<RedPacketInfo>?
) : Serializable
