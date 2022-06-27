package com.fzm.arch.connection.utils

import java.util.concurrent.ThreadFactory

/**
 * @author zhengjy
 * @since 2021/01/08
 * Description:
 */
object Utils {

    fun threadFactory(name: String, daemon: Boolean = false): ThreadFactory {
        return ThreadFactory { r -> Thread(r, name).apply { isDaemon = daemon } }
    }

}