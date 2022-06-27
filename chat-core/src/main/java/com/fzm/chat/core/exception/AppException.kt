package com.fzm.chat.core.exception

import com.fzm.chat.core.R
import com.fzm.chat.core.data.ChatConst.Error.CREATE_WORDS_ERROR
import com.fzm.chat.core.data.ChatConst.Error.DECRYPT_ERROR
import com.fzm.chat.core.data.ChatConst.Error.ENCRYPT_ERROR
import com.fzm.chat.core.data.ChatConst.Error.SAVE_WORDS_ERROR
import com.zjy.architecture.Arch
import java.lang.RuntimeException

/**
 * @author zhengjy
 * @since 2019/10/25
 * Description:
 */
class AppException(
    private val errorCode: Int,
    private val errorMessage: String
) : RuntimeException(errorMessage) {

    /**
     * 动态指定message
     * @param errorCode
     */
    constructor(errorCode: Int) : this(errorCode, getExceptionMessage(errorCode))

    fun getErrorCode(): Int {
        return errorCode
    }

    override fun toString(): String {
        return errorMessage
    }

    companion object {
        /**
         * 由于服务器传递过来的错误信息直接给用户看的话，用户未必能够理解
         * 需要根据错误码对错误信息进行一个转换，在显示给用户
         *
         * @param code
         * @return
         */
        private fun getExceptionMessage(code: Int): String {
            return when (code) {
                DECRYPT_ERROR -> Arch.context.getString(R.string.core_error_decrypt_fail)
                ENCRYPT_ERROR -> Arch.context.getString(R.string.core_error_encrypt_fail)
                CREATE_WORDS_ERROR -> Arch.context.getString(R.string.core_error_create_words_fail)
                SAVE_WORDS_ERROR -> Arch.context.getString(R.string.core_error_save_words_fail)
                else -> Arch.context.getString(R.string.core_error_unknown)
            }
        }
    }
}