package com.fzm.chat.biz.ui

import android.view.View
import com.fzm.chat.biz.R
import com.fzm.chat.biz.base.BizBottomDialogFragment
import com.fzm.chat.biz.databinding.DialogUserInfoAuthBinding
import com.fzm.chat.biz.utils.encryptAccount
import com.fzm.chat.biz.utils.encryptAddress
import com.fzm.chat.biz.utils.init
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.ifNull
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.zjy.architecture.ext.*
import org.json.JSONObject
import org.koin.android.ext.android.inject

/**
 * @author zhengjy
 * @since 2022/02/10
 * Description:
 */
class UserInfoAuthDialogFragment : BizBottomDialogFragment() {

    companion object {
        fun create() = UserInfoAuthDialogFragment()
    }

    private var onAuth: ((String) -> Unit)? = null
    private var onCancel: ((String) -> Unit)? = null

    private var name: String = ""
    private var avatar: String = ""
    private var reason: String = ""

    private val delegate by inject<LoginDelegate>()

    private val binding by init<DialogUserInfoAuthBinding>()

    override val root: View
        get() = binding.root

    override fun setupView(dialog: BottomSheetDialog) {
        binding.tvName.text = name
        if (avatar.isEmpty()) {
            binding.ivAvatar.gone()
        } else {
            binding.ivAvatar.visible()
            binding.ivAvatar.load(avatar)
        }
        val info = delegate.current.value.ifNull {
            toast("用户信息获取失败")
            return
        }
        if (!info.isLogin()) {
            toast("用户未登录")
            return
        }
        isCancelable = false
        binding.avatar.load(info.avatar, R.mipmap.default_avatar_round)
        binding.tvNickname.text = info.nickname.ifEmpty { encryptAddress(info.address) }
        binding.tvAddress.text = encryptAddress(info.address)
        info.phone?.let {
            binding.slPhone.visible()
            binding.tvPhone.text = encryptAccount(it)
        } ?: binding.slPhone.gone()
        info.email?.let {
            binding.slEmail.visible()
            binding.tvEmail.text = encryptAccount(it)
        } ?: binding.slEmail.gone()

        binding.reject.singleClick {
            onCancel?.invoke("cancel")
            dismiss()
        }
        binding.confirm.singleClick {
            val json = JSONObject().apply {
                put("address", info.address)
                put("nickname", info.nickname)
                put("avatar", info.avatar)
                put("phone", info.phone)
                put("email", info.email)
            }
            onAuth?.invoke(json.toString())
            dismiss()
        }
    }

    fun setName(name: String): UserInfoAuthDialogFragment {
        this.name = name
        return this
    }

    fun setAvatar(avatar: String): UserInfoAuthDialogFragment {
        this.avatar = avatar
        return this
    }

    fun setAuthReason(reason: String): UserInfoAuthDialogFragment {
        this.reason = reason
        return this
    }

    fun setOnAuthorizationListener(listener: (String) -> Unit): UserInfoAuthDialogFragment {
        this.onAuth = listener
        return this
    }

    fun setOnCancelListener(listener: (String) -> Unit): UserInfoAuthDialogFragment {
        this.onCancel = listener
        return this
    }
}