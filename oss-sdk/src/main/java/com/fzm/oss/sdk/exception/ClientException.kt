package com.fzm.oss.sdk.exception

/**
 * @author zhengjy
 * @since 2021/08/18
 * Description:
 */
class ClientException : Exception {

    constructor(message: String?) : super(message)

    constructor(cause: Throwable) : super(cause)
}