package com.fzm.arch.connection.processor

import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * @author zhengjy
 * @since 2020/12/07
 * Description:
 */
interface Processor<T> {

    /**
     * 消息处理逻辑
     *
     * @param server    服务器地址
     * @param message   消息结构
     *
     * @return  处理后的消息，返回null则不再继续传递处理
     */
    suspend fun process(server: String, message: T): T?

    companion object {

        private val methodCache = ConcurrentHashMap<Class<*>, Method>()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : GeneratedMessageV3> parseProtocol(clazz: Class<T>, bytes: ByteString): T {
        return suspendCancellableCoroutine { continuation ->
            try {
                val method = methodCache[clazz] ?: clazz.getMethod(
                    "parseFrom",
                    ByteString::class.java
                ).also {
                    methodCache[clazz] = it
                }
                continuation.resume(method.invoke(null, bytes) as T)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

}