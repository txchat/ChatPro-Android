package com.fzm.chat.router.biz

/**
 * @author zhengjy
 * @since 2021/10/26
 * Description:
 */
interface PasswordVerifier {

    fun setOnPasswordVerifyListener(listener: OnPasswordVerifyListener)

    interface OnPasswordVerifyListener {

        fun onSuccess(password: String)

        fun onFail() {}

        @Deprecated("暂时没有实现，不会触发回调")
        fun onCancel() {}
    }
}