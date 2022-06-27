package com.fzm.chat.core.chain

import java.lang.Exception

/**
 * @author zhengjy
 * @since 2021/08/31
 * Description:
 */
class ChainException : Exception {
    constructor() : super() {}
    constructor(message: String?) : super(message) {}
    constructor(cause: Throwable?) : super(cause) {}
    constructor(message: String?, cause: Throwable?) : super(message, cause) {}
}