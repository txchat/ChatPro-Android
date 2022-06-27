package com.fzm.chat.biz.bus.event

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2022/03/15
 * Description:
 */
data class EndPointLoginEvent(
    val deviceName: String,
    val datetime: Long
) : Serializable