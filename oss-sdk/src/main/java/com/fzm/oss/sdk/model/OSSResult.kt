package com.fzm.oss.sdk.model

import com.fzm.oss.sdk.exception.ServerException
import java.io.Serializable
import kotlin.jvm.Throws

/**
 * @author zhengjy
 * @since 2021/08/18
 * Description:
 */
open class OSSResult<T>(
    /**
     * 返回结果码
     */
    val result: Int,
    /**
     * 提示信息
     */
    val message: String?,
    /**
     * 返回数据
     */
    val data: T
) : Serializable

internal val OSSResult<*>.isSuccess get() = result == 0

@Throws(ServerException::class)
internal suspend fun <T> call(call: suspend () -> OSSResult<T>): T {
    return call().let {
        if (it.isSuccess) {
            it.data
        } else {
            throw ServerException(it.message)
        }
    }
}