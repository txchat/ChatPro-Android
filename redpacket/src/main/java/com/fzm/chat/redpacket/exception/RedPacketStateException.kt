package com.fzm.chat.redpacket.exception

import java.lang.Exception

/**
 * @author zhengjy
 * @since 2021/09/01
 * Description:红包状态异常
 */
class RedPacketStateException(val state: Int, message: String) : Exception(message)