package com.fzm.chat.group

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.databinding.ActivityAdminListBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.setVisible
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/06/08
 * Description:
 */
@Route(path = MainModule.ADMIN_LIST)
class AdminListActivity : BizActivity() {

    companion object {
        const val MAX_ADMIN_NUM = 10
    }

    @JvmField
    @Autowired
    var groupInfo: GroupInfo? = null

    private var groupOwner: GroupUser? = null
    private lateinit var mAdapter: BaseQuickAdapter<GroupUser, BaseViewHolder>

    private val viewModel by viewModel<GroupViewModel>()

    private val binding by init { ActivityAdminListBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        binding.rvAdmin.layoutManager = LinearLayoutManager(this)
        mAdapter = object :
            BaseQuickAdapter<GroupUser, BaseViewHolder>(R.layout.item_group_admin_set, null) {
            override fun convert(holder: BaseViewHolder, item: GroupUser) {
                holder.setText(R.id.tv_name, item.getDisplayName())
                holder.getView<ChatAvatarView>(R.id.iv_avatar)
                    .load(item.getDisplayImage(), R.mipmap.default_avatar_round)
                holder.getView<View>(R.id.iv_cancel_admin).setOnClickListener {
                    viewModel.changeGroupUserRole(
                        groupInfo?.gid ?: 0L,
                        item.address,
                        GroupUser.LEVEL_USER
                    )
                }
            }
        }
        binding.rvAdmin.adapter = mAdapter
    }

    override fun initData() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.getGroupAdmin(groupInfo?.gid ?: 0L).observe(this) { admin ->
            groupOwner = admin.find { it.role == GroupUser.LEVEL_OWNER }
            groupOwner?.apply {
                setupGroupOwner(this)
            }
            val data = admin.filter { it.role == GroupUser.LEVEL_ADMIN }
            binding.tvAdminNum.text = getString(R.string.chat_tips_group_admin_num, data.size)
            binding.tvAddAdmin.setVisible(data.size < MAX_ADMIN_NUM)
            mAdapter.setList(data)
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressed() }
        binding.tvAddAdmin.setOnClickListener {
            ARouter.getInstance().build(MainModule.SET_ADMIN)
                .withSerializable("groupInfo", groupInfo)
                .navigation()
        }
    }

    private fun setupGroupOwner(owner: GroupUser) {
        binding.tvOwnerName.text = owner.getDisplayName()
        binding.ivOwnerAvatar.load(owner.getDisplayImage(), R.mipmap.default_avatar_round)
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)
        super.onBackPressed()
    }
}