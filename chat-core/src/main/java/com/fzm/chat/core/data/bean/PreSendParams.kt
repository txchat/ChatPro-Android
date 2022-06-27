package com.fzm.chat.core.data.bean

import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.data.po.MessageSource
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/06/21
 * Description:
 */
data class PreSendParams(
    val msgType: Int,
    val msg: MessageContent,
    val source: MessageSource?
) : Serializable