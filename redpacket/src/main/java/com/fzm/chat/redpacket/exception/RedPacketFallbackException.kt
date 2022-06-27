package com.fzm.chat.redpacket.exception

import java.lang.Exception

/**
 * @author zhengjy
 * @since 2021/09/01
 * Description:红包发送失败异常
 */
class RedPacketFallbackException : Exception {
    constructor() : super() {}
    constructor(message: String?) : super(message) {}
    constructor(cause: Throwable?) : super(cause) {}
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
}