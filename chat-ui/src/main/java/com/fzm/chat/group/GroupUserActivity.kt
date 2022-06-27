package com.fzm.chat.group

import android.view.View
import androidx.fragment.app.commit
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.ChatSearchView
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.core.data.po.groupRole
import com.fzm.chat.databinding.ActivityGroupUserBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.widget.GroupUserActionPopup
import com.zjy.architecture.ext.setVisible
import com.zjy.architecture.util.KeyboardUtils

/**
 * @author zhengjy
 * @since 2021/05/20
 * Description:
 */
@Route(path = MainModule.GROUP_USER)
class GroupUserActivity : BizActivity(), View.OnClickListener {

    @JvmField
    @Autowired
    var groupInfo: GroupInfo? = null

    private lateinit var fragment: GroupUserListFragment
    private val binding by init { ActivityGroupUserBinding.inflate(layoutInflater) }

    private var showAdd = false
    private var showDel = false
    private var showAdmin = false

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        groupInfo?.also { info ->
            supportFragmentManager.commit {
                fragment = GroupUserListFragment.create(
                    info.gid,
                    role = groupInfo.groupRole
                )
                add(R.id.fl_container, fragment)
            }
        }
    }

    override fun initData() {
        binding.tvGroupName.text = "${groupInfo?.getDisplayName()}(${groupInfo?.memberNum})"
        showAdd = groupInfo?.joinType ?: 0 == GroupInfo.CAN_JOIN_GROUP || groupInfo.groupRole > GroupUser.LEVEL_USER
        showDel = groupInfo.groupRole > GroupUser.LEVEL_USER
        showAdmin = groupInfo.groupRole == GroupUser.LEVEL_OWNER
        binding.ivAdd.setVisible(showAdd || showDel || showAdmin)
    }

    override fun setEvent() {
        binding.ivBack.setOnClickListener(this)
        binding.ivSearch.setOnClickListener(this)
        binding.ivAdd.setOnClickListener(this)
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
            R.id.iv_add -> {
                GroupUserActionPopup.create(this, this, showAdd, showDel, showAdmin).show(v)
            }
            R.id.add_group_user -> {
                ARouter.getInstance().build(MainModule.CREATE_INVITE_GROUP)
                    .withString("server", groupInfo?.server?.address ?: "")
                    .withLong("groupId", groupInfo?.gid ?: 0L)
                    .withBoolean(
                        "selectFromOA",
                        groupInfo?.groupType ?: GroupInfo.TYPE_NORMAL != GroupInfo.TYPE_NORMAL
                    )
                    .navigation()
            }
            R.id.remove_group_user -> {
                ARouter.getInstance().build(MainModule.REMOVE_GROUP_USER)
                    .withSerializable("groupInfo", groupInfo)
                    .navigation()
            }
            R.id.set_admin -> {
                ARouter.getInstance().build(MainModule.SET_ADMIN)
                    .withSerializable("groupInfo", groupInfo)
                    .navigation()
            }
        }
    }

    override fun onBackPressedSupport() {
        if (!binding.chatSearch.onBackPressed()) {
            super.onBackPressedSupport()
        }
    }
}