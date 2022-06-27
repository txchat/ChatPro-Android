package com.fzm.chat.group

import android.view.View
import androidx.core.text.HtmlCompat
import androidx.fragment.app.commit
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.ChatSearchView
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.core.data.po.groupRole
import com.fzm.chat.databinding.ActivitySetAdminBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.widget.dialog.EasyDialog
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/06/08
 * Description:设置管理员和群主
 */
@Route(path = MainModule.SET_ADMIN)
class SetAdminActivity : BizActivity(), View.OnClickListener {

    @JvmField
    @Autowired
    var groupInfo: GroupInfo? = null

    @JvmField
    @Autowired
    var changeOwner: Boolean = false

    private val viewModel by viewModel<GroupViewModel>()

    private lateinit var fragment: GroupUserListFragment

    private val binding by init { ActivitySetAdminBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        if (changeOwner) {
            binding.tvTitle.setText(R.string.chat_title_change_owner)
        } else {
            binding.tvTitle.setText(R.string.chat_title_add_admin)
        }
        groupInfo?.also { info ->
            supportFragmentManager.commit {
                fragment = GroupUserListFragment.create(
                    gid = info.gid,
                    action = GroupUserListFragment.ACTION_SINGLE,
                    level = if (changeOwner) GroupUser.LEVEL_ADMIN else GroupUser.LEVEL_USER,
                    role = groupInfo.groupRole
                )
                fragment.setOnSingleSelectedListener {
                    if (changeOwner) {
                        EasyDialog.Builder()
                            .setHeaderTitle(getString(R.string.biz_tips))
                            .setContent(HtmlCompat.fromHtml(getString(R.string.chat_tips_change_group_owner, AppConfig.APP_ACCENT_COLOR_STR, it.getDisplayName()), HtmlCompat.FROM_HTML_MODE_COMPACT))
                            .setBottomLeftText(getString(R.string.biz_cancel))
                            .setBottomRightText(getString(R.string.biz_confirm))
                            .setBottomRightClickListener { dialog ->
                                dialog.dismiss()
                                viewModel.changeOwner(info.gid, it.address)
                            }
                            .create(this@SetAdminActivity)
                            .show()
                    } else {
                        viewModel.changeGroupUserRole(info.gid, it.address, GroupUser.LEVEL_ADMIN)
                    }
                }
                add(R.id.fcv_container, fragment)
            }
        }
    }

    override fun initData() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.changeRoleResult.observe(this) {
            dismiss()
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun setEvent() {
        binding.ivBack.setOnClickListener(this)
        binding.ivSearch.setOnClickListener(this)
        binding.chatSearch.setOnSearchCancelListener(object : ChatSearchView.OnSearchCancelListener {
            override fun onSearchCancel() {
                KeyboardUtils.hideKeyboard(binding.chatSearch.getFocusView())
                binding.chatSearch.setText(null)
                binding.chatSearch.reduce()
            }
        })
        binding.chatSearch.setOnTextChangeListener(object : ChatSearchView.OnTextChangeListener {
            override fun onTextChange(s: String) {
                fragment.getGroupUserList(s)
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_back -> onBackPressed()
            R.id.iv_search -> {
                binding.chatSearch.expand()
                binding.chatSearch.postDelayed({ KeyboardUtils.showKeyboard(binding.chatSearch.getFocusView()) }, 100)
            }
        }
    }

    override fun onBackPressedSupport() {
        if (!binding.chatSearch.onBackPressed()) {
            super.onBackPressedSupport()
        }
    }
}