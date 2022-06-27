package com.fzm.chat.group

import  android.view.View
import androidx.fragment.app.commit
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.databinding.ActivityRemoveGroupUserBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.ext.toast
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/05/25
 * Description:
 */
@Route(path = MainModule.REMOVE_GROUP_USER)
class RemoveGroupUserActivity : BizActivity() {

    @JvmField
    @Autowired
    var groupInfo: GroupInfo? = null

    private val viewModel by viewModel<GroupViewModel>()
    private lateinit var fragment: GroupUserListFragment

    private val binding by init { ActivityRemoveGroupUserBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initData() {
        ARouter.getInstance().inject(this)
        groupInfo?.apply {
            fragment = GroupUserListFragment.create(gid, GroupUserListFragment.ACTION_MULTI, GroupUser.LEVEL_USER)
            supportFragmentManager.commit {
                add(R.id.fcv_container, fragment)
            }
        }
    }

    override fun initView() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.removeResult.observe(this) {
            dismiss()
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressed() }
        binding.tvSubmit.setOnClickListener { _ ->
            groupInfo?.apply {
                val selected = fragment.getSelectedUsers().map { it.address }
                if (selected.isEmpty()) {
                    toast(R.string.chat_tips_select_delete_users)
                    return@setOnClickListener
                }
                viewModel.removeGroupMembers(server.address, gid, selected)
            }
        }
    }
}