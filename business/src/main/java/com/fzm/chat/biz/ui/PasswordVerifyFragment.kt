package com.fzm.chat.biz.ui

import androidx.fragment.app.DialogFragment
import com.fzm.chat.router.biz.PasswordVerifier

/**
 * @author zhengjy
 * @since 2021/10/26
 * Description:用于验证密聊密码的DialogFragment
 */
abstract class PasswordVerifyFragment : DialogFragment(), PasswordVerifier {

    protected var listener: PasswordVerifier.OnPasswordVerifyListener? = null

    override fun setOnPasswordVerifyListener(listener: PasswordVerifier.OnPasswordVerifyListener) {
        this.listener = listener
    }
}