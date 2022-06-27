package com.fzm.chat.group

import android.content.Intent
import android.text.Html
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.arch.connection.socket.ConnectionManager
import com.fzm.arch.connection.utils.urlKey
import com.fzm.chat.R
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.*
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.core.data.po.groupRole
import com.fzm.chat.core.data.po.isInGroup
import com.fzm.chat.core.data.po.isMuteAll
import com.fzm.chat.databinding.ActivityGroupInfoBinding
import com.fzm.chat.group.adapter.GroupManageAdapter
import com.fzm.chat.group.adapter.ManageOption
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.widget.BottomListDialog
import com.fzm.widget.SwitchView
import com.fzm.widget.dialog.EasyDialog
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.*
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.Serializable
import kotlin.math.min

/**
 * @author zhengjy
 * @since 2021/04/27
 * Description:
 */
@Route(path = MainModule.GROUP_INFO)
class GroupInfoActivity : BizActivity(), View.OnClickListener {

    companion object {
        const val REQUEST_ADD_MEMBERS = 100
        const val REQUEST_DEL_MEMBERS = 101
        const val REQUEST_MUTE_LIST = 102
        const val REQUEST_ADMIN_LIST = 103
        const val REQUEST_CHANGE_OWNER = 104
    }

    @JvmField
    @Autowired
    var groupId: Long = 0L

    private val viewModel by viewModel<GroupViewModel>()
    private val contactManager by inject<ContactManager>()
    private val connectionManager by inject<ConnectionManager>()

    private var groupInfo: GroupInfo? = null

    private lateinit var mAdapter: BaseQuickAdapter<GroupUserBean, BaseViewHolder>

    private val binding by init<ActivityGroupInfoBinding>()

    override val darkStatusColor: Boolean = true

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        binding.rvMember.layoutManager = GridLayoutManager(this, 5)
        mAdapter = object :
            BaseQuickAdapter<GroupUserBean, BaseViewHolder>(R.layout.item_group_user_grid, null) {
            override fun convert(holder: BaseViewHolder, item: GroupUserBean) {
                when (item.op) {
                    1 -> {
                        holder.setImageResource(R.id.operate, R.drawable.icon_group_info_add)
                        holder.setVisible(R.id.ll_head, false)
                        holder.setVisible(R.id.ll_operate, true)
                        holder.setText(R.id.operate_type, getString(R.string.chat_action_invite))
                        holder.itemView.setOnClickListener {
                            ARouter.getInstance().build(MainModule.CREATE_INVITE_GROUP)
                                .withString("server", groupInfo?.server?.address ?: "")
                                .withLong("groupId", groupId)
                                .withBoolean(
                                    "selectFromOA",
                                    groupInfo?.groupType ?: GroupInfo.TYPE_NORMAL != GroupInfo.TYPE_NORMAL
                                )
                                .navigation(instance, REQUEST_ADD_MEMBERS)
                        }
                    }
                    2 -> {
                        holder.setImageResource(R.id.operate, R.drawable.icon_group_info_minus)
                        holder.setVisible(R.id.ll_head, false)
                        holder.setVisible(R.id.ll_operate, true)
                        holder.setText(R.id.operate_type, getString(R.string.chat_action_remove))
                        holder.itemView.setOnClickListener {
                            ARouter.getInstance().build(MainModule.REMOVE_GROUP_USER)
                                .withSerializable("groupInfo", groupInfo)
                                .navigation(instance, REQUEST_DEL_MEMBERS)
                        }
                    }
                    else -> {
                        holder.setVisible(R.id.ll_head, true)
                        holder.setVisible(R.id.ll_operate, false)
                        holder.getView<ChatAvatarView>(R.id.head)
                            .load(item.user?.getDisplayImage(), R.mipmap.default_avatar_round)
                        holder.setText(R.id.name, item.user?.getDisplayName())
                        holder.itemView.setOnClickListener {
                            ARouter.getInstance().build(MainModule.CONTACT_INFO)
                                .withString("address", item.user?.getId())
                                .withLong("groupId", groupId)
                                .withInt("role", groupInfo.groupRole)
                                .navigation()
                        }
                    }
                }
            }
        }
        binding.rvMember.adapter = mAdapter
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.getGroupInfoLocal(groupId).observe(this) {
            setupGroupInfo(it)
        }
        viewModel.exitResult.observe(this) {
            ARouter.getInstance().build(AppModule.MAIN).navigation(this)
            finish()
        }
        viewModel.disbandResult.observe(this) {
            ARouter.getInstance().build(AppModule.MAIN).navigation(this)
            finish()
        }
    }

    override fun initData() {
        viewModel.getGroupInfo(groupId)
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressed() }
        binding.ctbTitle.setOnRightClickListener {
            ARouter.getInstance().build(MainModule.GROUP_QRCODE)
                .withSerializable("groupInfo", groupInfo).navigation()
        }
        binding.svNoDisturb.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
            override fun toggleToOn(view: SwitchView?) {
                viewModel.changeNoDisturb(groupId, true)
                view?.toggleSwitch(true)
            }

            override fun toggleToOff(view: SwitchView?) {
                viewModel.changeNoDisturb(groupId, false)
                view?.toggleSwitch(false)
            }
        })
        binding.svStickTop.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
            override fun toggleToOn(view: SwitchView?) {
                viewModel.changeStickTop(groupId, true)
                view?.toggleSwitch(true)
            }

            override fun toggleToOff(view: SwitchView?) {
                viewModel.changeStickTop(groupId, false)
                view?.toggleSwitch(false)
            }
        })
        binding.svMuteAll.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
            override fun toggleToOn(view: SwitchView?) {
                viewModel.changeMuteType(groupId, true)
                view?.toggleSwitch(true)
            }

            override fun toggleToOff(view: SwitchView?) {
                viewModel.changeMuteType(groupId, false)
                view?.toggleSwitch(false)
            }
        })
        binding.ivAvatar.setOnClickListener(this)
        binding.llSeeMember.setOnClickListener(this)
        binding.llGroupNickname.setOnClickListener(this)
        binding.llChatHistory.setOnClickListener(this)
        binding.llFiles.setOnClickListener(this)
        // 群管理
        binding.llAdmin.setOnClickListener(this)
        binding.llOwner.setOnClickListener(this)
        binding.llJoinLimit.setOnClickListener(this)
        binding.llAddLimit.setOnClickListener(this)
        binding.llMuteList.setOnClickListener(this)
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.getGroupInfo(groupId)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
//                REQUEST_ADD_MEMBERS -> viewModel.getGroupInfo(groupId)
//                REQUEST_DEL_MEMBERS -> viewModel.getGroupInfo(groupId)
//                REQUEST_MUTE_LIST -> viewModel.getGroupInfo(groupId)
//                REQUEST_ADMIN_LIST -> viewModel.getGroupInfo(groupId)
//                REQUEST_CHANGE_OWNER -> viewModel.getGroupInfo(groupId)
            }
        }
    }

    private fun setupGroupInfo(info: GroupInfo?) {
        if (info == null) {
            binding.slGroupMembers.gone()
            binding.slGroupNickname.gone()
            binding.slGroupSetting.gone()
            return
        }
        groupInfo = info
        binding.ivAvatar.load(info.getDisplayImage(), R.mipmap.default_avatar_room)
        binding.tvName.text = info.getDisplayName()
        if (info.name.isEmpty()) {
            binding.tvPubName.gone()
        } else {
            binding.tvPubName.visible()
            binding.tvPubName.text = getString(R.string.chat_tips_group_pub_name, info.publicName)
        }
        binding.tvMarkId.text = getString(R.string.chat_tips_group_mark_id, info.markId)
        binding.tvServerAddress.text = info.server.address.urlKey()
        binding.tvGroupType.text = when (info.groupType) {
            GroupInfo.TYPE_NORMAL -> getString(R.string.chat_detail_group_type0)
            GroupInfo.TYPE_TEAM -> getString(R.string.chat_detail_group_type1)
            GroupInfo.TYPE_DEPART -> getString(R.string.chat_detail_group_type2)
            else -> getString(R.string.chat_detail_group_type0)
        }
        val group =
            viewModel.servers.value?.find { server -> server.address.urlKey() == info.server.address.urlKey() }
        if (group != null) {
            binding.tvServerName.visible()
            binding.tvServerName.text = group.name
            connectionManager.observeSocketState(this) {
                val id =
                    if (connectionManager.getChatSocket(group.address.urlKey())?.isAlive == true) {
                        R.drawable.ic_server_connect
                    } else {
                        R.drawable.ic_server_disconnect
                    }

                val status = ResourcesCompat.getDrawable(resources, id, null)?.apply {
                    setBounds(0, 0, minimumWidth, minimumHeight)
                }
                binding.tvServerName.setCompoundDrawables(null, null, status, null)
            }
        } else {
            binding.tvServerName.gone()
            val status =
                ResourcesCompat.getDrawable(resources, R.drawable.ic_server_not_exist, null)
                    ?.apply {
                        setBounds(0, 0, minimumWidth, minimumHeight)
                    }
            binding.tvServerAddress.setCompoundDrawables(null, null, status, null)
        }
        if (info.isInGroup) {
            binding.slGroupMembers.visible()
            binding.slGroupNickname.visible()
            binding.slGroupSetting.visible()
            lifecycleScope.launch {
                binding.tvMemberNum.text =
                    getString(R.string.chat_tips_group_member_num, info.memberNum)
                val membersBean = mutableListOf<GroupUserBean>()
                if (info.groupRole > GroupUser.LEVEL_USER || info.joinType == GroupInfo.CAN_JOIN_GROUP) {
                    membersBean.add(GroupUserBean(1, null))
                }
                if (info.groupRole > GroupUser.LEVEL_USER) {
                    membersBean.add(GroupUserBean(2, null))
                }
                info.members?.also {
                    // 最多显示10个item，包括添加和删除按钮
                    val userNum = min(it.size, 10 - membersBean.size)
                    for (i in 0 until userNum) {
                        val user =
                            contactManager.getGroupUserInfo(info.gid.toString(), it[i].address)
                        membersBean.add(i, GroupUserBean(0, user))
                    }
                }
                mAdapter.setList(membersBean)
            }
            binding.svNoDisturb.isOpened = info.noDisturb
            binding.svStickTop.isOpened = info.stickTop
            if (info.groupType == GroupInfo.TYPE_NORMAL) {
                binding.tvTeamGroupTips.gone()
                if (info.owner.address == viewModel.getAddress()) {
                    binding.exitGroup.gone()
                    binding.disbandGroup.visible()
                    binding.disbandGroup.setOnClickListener(this)
                } else {
                    binding.exitGroup.visible()
                    binding.disbandGroup.gone()
                    binding.exitGroup.setOnClickListener(this)
                }
            } else {
                binding.tvTeamGroupTips.visible()
                binding.exitGroup.gone()
                binding.disbandGroup.gone()
            }
            binding.tvNickname.text = info.person?.nickname
            binding.llOnlyOwner.setVisible(info.groupRole == GroupUser.LEVEL_OWNER)
            if (info.groupRole > GroupUser.LEVEL_USER) {
                binding.slGroupManagement.visible()
                lifecycleScope.launch {
                    val owner =
                        contactManager.getGroupUserInfo(info.gid.toString(), info.owner.address)
                    binding.tvOwner.text = owner.getDisplayName()
                }
                binding.tvAdminNum.text =
                    getString(R.string.chat_tips_group_member_num, info.adminNum)
                binding.tvJoinType.text =
                    if (info.joinType == GroupInfo.CAN_JOIN_GROUP)
                        getString(R.string.chat_group_join_limit1)
                    else
                        getString(R.string.chat_group_join_limit2)
                if (info.groupType == GroupInfo.TYPE_NORMAL) {
                    binding.ivJoinLimit.visible()
                } else {
                    binding.ivJoinLimit.invisible()
                }
                binding.tvAddType.text =
                    if (info.friendType == GroupInfo.CAN_ADD_FRIEND)
                        getString(R.string.chat_group_add_limit1)
                    else
                        getString(R.string.chat_group_add_limit2)
                binding.svMuteAll.isOpened = info.isMuteAll
                binding.tvMuteList.text = getString(R.string.chat_tip_mute_user_num, info.muteNum)
                val drawable =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_edit, null)?.apply {
                        setBounds(0, 0, minimumWidth, minimumHeight)
                    }
                binding.tvName.setCompoundDrawables(null, null, drawable, null)
                binding.tvName.setOnClickListener {
                    ARouter.getInstance().build(MainModule.EDIT_GROUP_NAME)
                        .withInt("type", 0)
                        .withLong("groupId", info.gid)
                        .withString("groupName", info.name)
                        .withString("groupPubName", info.publicName)
                        .navigation()
                }
            } else {
                binding.slGroupManagement.gone()
                binding.tvName.setCompoundDrawables(null, null, null, null)
                binding.tvName.setOnClickListener(null)
            }
        } else {
            binding.exitGroup.gone()
            binding.disbandGroup.gone()
            binding.tvTeamGroupTips.gone()
            binding.slGroupMembers.gone()
            binding.slGroupNickname.gone()
            binding.slGroupSetting.gone()
            binding.slGroupManagement.gone()
            binding.tvName.setOnClickListener(null)
        }
    }

    private val joinAdapter by lazy {
        GroupManageAdapter(instance).apply {
            setData(
                listOf(
                    ManageOption(
                        getString(R.string.chat_group_join_limit1),
                        getString(R.string.chat_group_join_limit_sub1)
                    ) { viewModel.changeJoinType(groupId, GroupInfo.CAN_JOIN_GROUP) },
                    ManageOption(
                        getString(R.string.chat_group_join_limit2),
                        getString(R.string.chat_group_join_limit_sub2)
                    ) { viewModel.changeJoinType(groupId, GroupInfo.ONLY_INVITE_GROUP) }
                )
            )
        }
    }

    private val addAdapter by lazy {
        GroupManageAdapter(instance).apply {
            setData(
                listOf(
                    ManageOption(getString(R.string.chat_group_add_limit1), null) {
                        viewModel.changeFriendType(groupId, GroupInfo.CAN_ADD_FRIEND)
                    },
                    ManageOption(getString(R.string.chat_group_add_limit2), null) {
                        viewModel.changeFriendType(groupId, GroupInfo.FORBID_ADD_FRIEND)
                    }
                )
            )
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_avatar -> {
                ARouter.getInstance().build(MainModule.EDIT_GROUP_AVATAR)
                    .withLong("groupId", groupId)
                    .withBoolean(
                        "canEdit",
                        groupInfo.groupRole > GroupUser.LEVEL_USER
                    )
                    .withString("avatar", groupInfo?.avatar)
                    .navigation()
            }
            R.id.ll_see_member -> {
                groupInfo?.also {
                    ARouter.getInstance().build(MainModule.GROUP_USER)
                        .withSerializable("groupInfo", it)
                        .navigation()
                }
            }
            R.id.ll_group_nickname -> {
                groupInfo?.also {
                    ARouter.getInstance().build(MainModule.EDIT_GROUP_NAME)
                        .withInt("type", 1)
                        .withLong("groupId", it.gid)
                        .withString("groupName", it.name)
                        .withString("nickname", it.person?.nickname)
                        .navigation()
                }
            }
            R.id.ll_chat_history -> {
                ARouter.getInstance().build(MainModule.SEARCH_LOCAL_SCOPED)
                    .withInt("scope", SearchScope.CHAT_LOG)
                    .withSerializable(
                        "chatTarget",
                        ChatTarget(ChatConst.GROUP_CHANNEL, groupId.toString())
                    )
                    .withBoolean("popKeyboard", true)
                    .navigation()
            }
            R.id.ll_files -> {
                ARouter.getInstance().build(MainModule.FILE_MANAGEMENT)
                    .withString("target", groupId.toString())
                    .withInt("channelType", ChatConst.GROUP_CHANNEL)
                    .navigation()
            }
            R.id.ll_admin -> {
                if (groupInfo.groupRole == GroupUser.LEVEL_OWNER) {
                    ARouter.getInstance().build(MainModule.ADMIN_LIST)
                        .withSerializable("groupInfo", groupInfo)
                        .navigation(instance, REQUEST_ADMIN_LIST)
                }
            }
            R.id.ll_owner -> {
                if (groupInfo.groupRole == GroupUser.LEVEL_OWNER) {
                    ARouter.getInstance().build(MainModule.SET_ADMIN)
                        .withSerializable("groupInfo", groupInfo)
                        .withBoolean("changeOwner", true)
                        .navigation(instance, REQUEST_CHANGE_OWNER)
                }
            }
            R.id.ll_join_limit -> {
                if (groupInfo?.groupType != GroupInfo.TYPE_NORMAL) {
                    // 团队群不能修改这个选项
                    return
                }
                val dialog = BottomListDialog.create(
                    this,
                    joinAdapter,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                dialog.also {
                    it.setTitle(getString(R.string.chat_group_join_limit))
                    it.setOnItemClickListener { parent, _, position, _ ->
                        val option = parent.getItemAtPosition(position) as ManageOption
                        option.action.invoke()
                        it.cancel()
                    }
                    it.show()
                }
            }
            R.id.ll_add_limit -> {
                val dialog = BottomListDialog.create(
                    this,
                    addAdapter,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                dialog.also {
                    it.setTitle(getString(R.string.chat_group_add_limit))
                    it.setOnItemClickListener { parent, _, position, _ ->
                        val option = parent.getItemAtPosition(position) as ManageOption
                        option.action.invoke()
                        it.cancel()
                    }
                    it.show()
                }
            }
            R.id.ll_mute_list -> {
                ARouter.getInstance().build(MainModule.MUTE_LIST)
                    .withLong("groupId", groupId)
                    .navigation(instance, REQUEST_MUTE_LIST)
            }
            R.id.exit_group -> {
                val content = getString(
                    R.string.chat_dialog_tips_exit_group,
                    AppConfig.APP_ACCENT_COLOR_STR,
                    groupInfo?.getDisplayName()
                )
                EasyDialog.Builder()
                    .setHeaderTitle(getString(R.string.biz_tips))
                    .setContent(Html.fromHtml(content))
                    .setBottomLeftText(getString(R.string.biz_cancel))
                    .setBottomRightText(getString(R.string.biz_confirm))
                    .setBottomRightClickListener {
                        it.dismiss()
                        viewModel.exitGroup(groupInfo?.server?.address, groupId)
                    }
                    .create(this)
                    .show()
            }
            R.id.disband_group -> {
                val content = getString(
                    R.string.chat_dialog_tips_disband_group,
                    AppConfig.APP_ACCENT_COLOR_STR,
                    groupInfo?.getDisplayName()
                )
                EasyDialog.Builder()
                    .setHeaderTitle(getString(R.string.biz_tips))
                    .setContent(Html.fromHtml(content))
                    .setBottomLeftText(getString(R.string.biz_cancel))
                    .setBottomRightText(getString(R.string.biz_confirm))
                    .setBottomRightClickListener {
                        it.dismiss()
                        viewModel.disbandGroup(groupInfo?.server?.address, groupId)
                    }
                    .create(this)
                    .show()
            }
        }
    }

    inner class GroupUserBean(
        /**
         * 0：群成员展示
         * 1：添加群成员操作
         * 2：删除群成员操作
         */
        val op: Int,
        val user: Contact?,
    ) : Serializable
}