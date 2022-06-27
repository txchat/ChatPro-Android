package com.fzm.chat.security

import android.view.View
import androidx.fragment.app.commit
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.databinding.ActivityEncryptPasswordBinding
import com.fzm.chat.router.main.MainModule

/**
 * @author zhengjy
 * @since 2019/05/24
 * Description:修改密聊密码界面
 */
@Route(path = MainModule.ENCRYPT_PWD)
class EncryptPasswordActivity : BizActivity() {

    @JvmField
    @Autowired
    var setMode: Boolean = false

    @JvmField
    @Autowired
    var bindPhone: String? = null

    private val binding by init { ActivityEncryptPasswordBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        if (setMode) {
            supportFragmentManager.commit {
                add(R.id.fl_container, SetEncryptPasswordFragment.create(bindPhone))
            }
        } else {
            supportFragmentManager.commit {
                add(R.id.fl_container, UpdateEncryptPasswordFragment())
            }
        }
    }

    override fun initData() {

    }

    override fun setEvent() {

    }
}
