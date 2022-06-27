package com.fzm.arch.connection.utils

import android.net.Uri
import androidx.collection.LruCache
import androidx.core.os.bundleOf
import chat33.comet.Socket
import com.fzm.arch.connection.protocol.Protocols
import com.zjy.architecture.ext.tryWith

/**
 * @author zhengjy
 * @since 2021/03/10
 * Description:
 */
private val cacheMap = LruCache<String, String>(16)

fun String.urlKey(): String {
    val cache = cacheMap[this]
    if (cache != null) return cache
    val uri = tryWith { Uri.parse(this) }
    return uri?.authority?.also { cacheMap.put(this, it) } ?: this
}

fun extra(opt: Int, ack: Int, requireAck: Boolean) = bundleOf(
    Protocols.OPTION to opt,
    Protocols.ACK to ack,
    Protocols.REQUIRE_ACK to requireAck
)

fun extra(opt: Socket.Op, ack: Int = 0, requireAck: Boolean = false) = extra(opt.number, ack, requireAck)