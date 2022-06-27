package com.fzm.arch.connection.exception

/**
 * @author zhengjy
 * @since 2021/12/31
 * Description:
 */
class CipherException : Exception {

    constructor(message: String) : super(message)

    constructor(message: String, e: Exception) : super(message, e)
}