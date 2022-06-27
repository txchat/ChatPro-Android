package com.fzm.chat.group

import android.view.View
import androidx.core.text.HtmlCompat
import androidx.core.widget.doOnTextChanged
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.databinding.ActivityEditGroupNameBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.toast
import com.zjy.architecture.ext.visible
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/06/02
 * Description:
 */
@Route(path = MainModule.EDIT_GROUP_NAME)
class EditGroupNameActivity : BizActivity() {

    companion object {
        const val TYPE_GROUP_NAME = 0
        const val TYPE_NICKNAME_IN_GROUP = 1
    }

    /**
     * 修改昵称类型
     * 0：修改群名
     * 1：修改我在群内的昵称
     */
    @JvmField
    @Autowired
    var type: Int = 0

    @JvmField
    @Autowired
    var groupId: Long = 0L

    @JvmField
    @Autowired
    var groupName: String? = null

    @JvmField
    @Autowired
    var groupPubName: String? = null

    @JvmField
    @Autowired
    var nickname: String? = null

    private val viewModel by viewModel<GroupViewModel>()

    private val binding by init { ActivityEditGroupNameBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        if (type == TYPE_GROUP_NAME) {
            binding.ctbTitle.setTitle(getString(R.string.chat_title_edit_group_name))
            binding.tvNameTips.setText(R.string.chat_tip_group_name)
            binding.etName.setText(groupName)
            binding.etName.selectAll()
            binding.etPubName.setText(groupPubName)
            binding.tvNameCount.text = getString(R.string.chat_tips_num_20, groupName?.length ?: 0)
            binding.slPubName.visible()
            binding.tipsGroupPubName.visible()
            binding.tipsGroupNickname.gone()
        } else if (type == TYPE_NICKNAME_IN_GROUP) {
            binding.ctbTitle.setTitle(getString(R.string.chat_title_edit_nickname_in_group))
            binding.tvNameTips.setText(R.string.chat_tip_nickname_in_group)
            binding.etName.setText(nickname)
            binding.etName.selectAll()
            binding.tvNameCount.text = getString(R.string.chat_tips_num_20, nickname?.length ?: 0)
            binding.slPubName.gone()
            binding.tipsGroupPubName.gone()
            binding.tipsGroupNickname.visible()
            binding.tipsGroupNickname.text = HtmlCompat.fromHtml(
                getString(
                    R.string.chat_tips_set_nickname_in_group,
                    AppConfig.APP_ACCENT_COLOR_STR,
                    groupName
                ), HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        }

        binding.etName.postDelayed({
            KeyboardUtils.showKeyboard(binding.etName)
        }, 300)
    }

    override fun initData() {
        viewModel.editNameResult.observe(this) {
            if (type == TYPE_GROUP_NAME) {
                toast(R.string.chat_tip_edit_group_name_success)
            } else if (type == TYPE_NICKNAME_IN_GROUP) {
                toast(R.string.chat_tip_edit_group_nickname_success)
            }
            dismiss()
            finish()
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressed() }
        binding.etName.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.tvNameCount.text = getString(R.string.chat_tips_num_20, 0)
            } else {
                binding.tvNameCount.text = getString(R.string.chat_tips_num_20, text.length)
            }
        }
        binding.etPubName.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.tvPubNameCount.text = getString(R.string.chat_tips_num_20, 0)
            } else {
                binding.tvPubNameCount.text = getString(R.string.chat_tips_num_20, text.length)
            }
        }
        binding.tvSubmit.setOnClickListener {
            val newName = binding.etName.text?.toString()?.trim()
            if (type == TYPE_GROUP_NAME) {
                if (newName.isNullOrEmpty()) {
                    toast(R.string.chat_tip_group_name_empty)
                    return@setOnClickListener
                }
                val newPubName = binding.etPubName.text?.toString()?.trim()
                if (newPubName.isNullOrEmpty()) {
                    toast(R.string.chat_tip_group_pub_name_empty)
                    return@setOnClickListener
                }
                if (groupName != newName || groupPubName != newPubName) {
                    when {
                        groupName == newName -> {
                            viewModel.editGroupNames(groupId, "", newPubName)
                        }
                        groupPubName == newPubName -> {
                            viewModel.editGroupNames(groupId, newName, "")
                        }
                        else -> {
                            viewModel.editGroupNames(groupId, newName, newPubName)
                        }
                    }
                } else {
                    finish()
                }
            } else if (type == TYPE_NICKNAME_IN_GROUP) {
                if (nickname != newName) {
                    viewModel.editNicknameInGroup(groupId, newName)
                } else {
                    finish()
                }
            }
        }
    }
}