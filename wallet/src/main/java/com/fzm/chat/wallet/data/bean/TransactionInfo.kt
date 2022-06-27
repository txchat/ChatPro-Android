package com.fzm.chat.wallet.data.bean

import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.po.FriendUser
import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/04
 * Description:
 */
data class TransactionInfo(
    @SerializedName("txid")
    val txId: String,
    /**
     * 交易时间
     */
    @SerializedName("blocktime")
    val blockTime: Long,
    /**
     * 矿工费
     */
    val fee: String,
    /**
     * 转账地址
     */
    val from: String,
    /**
     * 收款地址
     */
    val to: String,
    /**
     * 区块高度
     */
    val height: Int,
    /**
     * 交易状态
     * 失败：-1，确认中：0，成功：1
     */
    var status: String,
    /**
     * 交易金额
     */
    val value: String,
    /**
     * 上链备注
     */
    val note: String?,
) : Serializable {

    data class Wrapper(
        val refresh: Boolean,
        val data: List<TransactionInfo>
    ) : Serializable

    companion object {
        const val FAIL = "-1"
        const val PENDING = "0"
        const val SUCCESS = "1"
    }

    /**
     * 发送或接受方的联系人对象
     */
    var contact: Contact? = null

    fun getAddress(): String {
        return if (from == AppPreference.ADDRESS) to else from
    }

    fun getFriendName(): String {
        val friend = contact as? FriendUser ?: return ""
        return friend.remark.ifEmpty { friend.nickname }
    }
}

val TransactionInfo.isSend: Boolean
    get() = from == AppPreference.ADDRESS