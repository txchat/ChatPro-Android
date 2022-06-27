package com.fzm.chat.core.data.bean

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/01/22
 * Description:
 */
data class SendMsgReply(
    val logId: Long,
    val datetime: Long
) : Serializable {
    override fun toString(): String {
        return "[logId=$logId, datetime=$datetime]"
    }
}