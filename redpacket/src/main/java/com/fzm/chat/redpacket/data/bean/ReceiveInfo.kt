package com.fzm.chat.redpacket.data.bean

import com.fzm.chat.core.data.bean.Contact
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/10/11
 * Description:
 */
data class ReceiveInfo(
    /**
     * 领取者的地址
     */
    val addr: String,
    /**
     * 领取金额
     */
    val amount: Long,
    /**
     * 币种所属合约
     */
    val assetExec: String,
    /**
     * 币种名
     */
    val assetSymbol: String,
    /**
     * 领取时间
     */
    val createTime: Long,
    /**
     * 仅当为入账失败状态时才会返回
     */
    val failMessage: String?,
    /**
     * 领取编号，用于滚动查询
     */
    val receiveId: String,
    /**
     * 领取hash
     */
    val receiveHash: String?,
    /**
     * 领取状态
     * 1：入账成功 2：入账失败
     */
    val status: Int,
    /**
     * 币种精度
     */
    var decimalPlaces: Int,

    /**
     * 红包ID
     */
    var packetId: String
) : Serializable {

    /**
     * 红包领取人信息
     */
    var receiver: Contact? = null

    /**
     * 红包发送地址
     */
    var fromAddr: String = ""

    /**
     * 红包发送人信息
     */
    var sender: Contact? = null

    /**
     * 红包类型
     */
    var type: Int = 1

    companion object {
        val EMPTY = ReceiveInfo("empty", 0L, "", "", 0L, null, "", "", 0, 0, "")
    }

    val isEmpty: Boolean get() = addr == "empty"

    data class Wrapper(
        val receiveRecords: List<ReceiveInfo>
    ) : Serializable
}