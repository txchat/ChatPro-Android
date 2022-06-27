package com.fzm.chat.core.chain

import com.zjy.architecture.data.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * @author zhengjy
 * @since 2021/08/31
 * Description:
 */

private const val MAX_RETRY_QUERY = 10

/**
 * 查询交易是否被区块链确认，以及业务状态是否成功
 *
 * @param initDelay     首次查询延迟
 * @param period        每次查询间隔
 * @param maxRetryTimes 最大重试次数
 * @param checker       结果数据检查器
 */
suspend fun <T> waitForChainResult(
    initDelay: Long = 2000L,
    period: Long = 1000L,
    maxRetryTimes: Int = MAX_RETRY_QUERY,
    checker: (T) -> Boolean,
    request: suspend () -> Result<T>
): Result<T> {
    return withContext(Dispatchers.IO) {
        if (initDelay > 0) delay(initDelay)
        checkTransactionStatus(1, maxRetryTimes, period, checker, request)
    }
}

/**
 * 检查区块链交易的状态和结果
 */
private suspend fun <T> checkTransactionStatus(
    counter: Int,
    maxRetryTimes: Int,
    period: Long,
    checker: (T) -> Boolean,
    request: suspend () -> Result<T>
): Result<T> {
    val result = request.invoke()
    if (result.isSucceed()) {
        return if (checker(result.data())) {
            result
        } else {
            Result.Error(Exception("区块链业务失败"))
        }
    }
    return if (counter < maxRetryTimes) {
        if (period > 0) delay(period)
        checkTransactionStatus(counter + 1, maxRetryTimes, period, checker, request)
    } else {
        Result.Error(result.error())
    }
}