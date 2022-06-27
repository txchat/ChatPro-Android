package com.fzm.chat.contact

import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.databinding.ActivityAddFriendBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.ext.singleClick
import com.zjy.architecture.ext.toast
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/11/15
 * Description:
 */
@Route(path = MainModule.ADD_FRIEND)
class AddFriendActivity : BizActivity() {

    @JvmField
    @Autowired
    var address: String = ""

    private lateinit var defaultMsg: String

    private var message: String = ""
    private var remark: String = ""

    private val viewModel by viewModel<AddFriendViewModel>()
    private val manager by inject<ContactManager>()

    private val binding by init<ActivityAddFriendBinding>()

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.addFriendResult.observe(this) {
            toast(R.string.chat_tips_add_friend_success)
            dismiss()
            finish()
        }
    }

    override fun initData() {
        lifecycleScope.launchWhenResumed {
            val user = manager.getUserInfo(viewModel.getAddress() ?: "", true)
            val friend = manager.getUserInfo(address, true) as? FriendUser?
            defaultMsg = "你好，我是${user.getDisplayName()}，已添加你为好友！"
            binding.etMessage.hint = defaultMsg
            binding.etRemark.setText(friend?.remark)
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.etMessage.addTextChangedListener {
            it?.trim()?.toString()?.apply {
                message = this
            }
        }
        binding.etRemark.addTextChangedListener {
            it?.trim()?.toString()?.apply {
                remark = this
            }
        }
        binding.tvSubmit.singleClick {
            viewModel.addFriend(address, remark, message.ifEmpty { defaultMsg })
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.biz_slide_bottom_out)
    }
}