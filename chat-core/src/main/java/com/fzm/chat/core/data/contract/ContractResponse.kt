package com.fzm.chat.core.data.contract

import com.zjy.architecture.data.Result
import com.zjy.architecture.exception.ApiException
import com.zjy.architecture.ext.handleException
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/02/05
 * Description:
 */
data class ContractResponse<T>(
    var id: Int,
    var result: T,
    var error: String?
) : Serializable {

    fun isSuccess(): Boolean {
        return error.isNullOrEmpty()
    }
}

suspend fun <T> apiCall2(call: suspend () -> ContractResponse<T>): Result<T> {
    return try {
        call().let {
            if (it.isSuccess()) {
                Result.Success(it.result)
            } else {
                Result.Error(handleException(ApiException(it.error)))
            }
        }
    } catch (e: Exception) {
        Result.Error(handleException(e))
    }
}