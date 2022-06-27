package com.fzm.chat.contact

import android.content.Intent
import android.os.CountDownTimer
import android.text.Html
import android.view.Gravity
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.arch.connection.socket.ConnectionManager
import com.fzm.arch.connection.utils.urlKey
import com.fzm.chat.R
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.base.FunctionModules
import com.fzm.chat.biz.base.enableWallet
import com.fzm.chat.biz.bus.BusEvent
import com.fzm.chat.biz.bus.LiveDataBus
import com.fzm.chat.biz.bus.event.ChangeTabEvent
import com.fzm.chat.biz.utils.callPhone
import com.fzm.chat.biz.utils.encryptPhone
import com.fzm.chat.biz.utils.encryptString
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.*
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.data.po.*
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.core.rtc.calling.RTCCalling
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.databinding.ActivityContactInfoBinding
import com.fzm.chat.databinding.PopupGroupUserMuteBinding
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.oa.hasCompany
import com.fzm.chat.router.rtc.RtcModule
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.ui.MediaGalleryActivity
import com.fzm.chat.utils.StringUtils
import com.fzm.chat.widget.MutePopupWindow
import com.fzm.widget.SwitchView
import com.fzm.widget.dialog.EasyDialog
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.*
import com.zjy.architecture.util.ActivityUtils
import com.zjy.architecture.util.other.BarUtils
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2020/12/23
 * Description:
 */
@Route(path = MainModule.CONTACT_INFO)
class ContactInfoActivity : BizActivity(), View.OnClickListener {

    companion object {
        const val REQUEST_CHANGE_SERVER = 100
    }

    @JvmField
    @Autowired
    var groupId: Long = 0L

    @JvmField
    @Autowired
    var role: Int = GroupUser.LEVEL_USER

    @JvmField
    @Autowired
    var address: String? = null

    @JvmField
    @Autowired
    var disableSend = false

    private var friendUser: FriendUser? = null
    private var groupUser: GroupUser? = null
    private var groupInfo: GroupInfo? = null

    private var counter: MuteCountDown? = null
    private var hasCompany = false

    private val viewModel by viewModel<ContactViewModel>()
    private val connectionManager by inject<ConnectionManager>()
    private val delegate by inject<LoginDelegate>()
    private val binding by init<ActivityContactInfoBinding>()

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        notchSupport(window)
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.biz_color_primary_dark), 0)
        BarUtils.setStatusBarLightMode(this, true)
        window?.navigationBarColor = ContextCompat.getColor(this, R.color.biz_color_primary)
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
    }

    override fun initData() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.companyUser.observe(this) {
            hasCompany = it.hasCompany
        }
        viewModel.userInfo.observe(this) {
            friendUser = it
            binding.ivAvatar.load(it.avatar, R.mipmap.default_avatar_round)
            if (it.remark.isEmpty()) {
                binding.tvRemark.text = it.getDisplayName()
                binding.tvName.gone()
            } else {
                binding.tvRemark.text = it.remark
                binding.tvName.text =
                    getString(R.string.chat_tips_nickname_placeholder, it.nickname)
                binding.tvName.visible()
            }
            binding.tvAddress.text = getString(R.string.chat_tips_address_placeholder, it.address)

            checkSendAndTransfer(it, true)
            when {
                it.address == delegate.getAddress() -> {
                    binding.slFriendSetting.gone()
                    binding.slServerGroup.gone()
                    binding.slBlock.gone()
                    binding.addFriend.gone()
                    binding.deleteFriend.gone()

                    binding.tvRemark.setCompoundDrawables(null, null, null, null)
                }
                it.isBlock -> {
                    binding.slFriendSetting.gone()
                    binding.slServerGroup.gone()
                    binding.slBlock.visible()
                    binding.addFriend.gone()
                    binding.deleteFriend.gone()
                    // 拉黑信息
                    binding.tvBlockState.setText(R.string.chat_remove_black_list)
                    binding.tvBlockMessage.setText(R.string.chat_user_black_message)

                    binding.tvRemark.setCompoundDrawables(null, null, null, null)
                }
                else -> {
                    binding.slFriendSetting.visible()
                    binding.slServerGroup.visible()
                    binding.slBlock.visible()
                    binding.addFriend.setVisible(!it.isFriend)
                    binding.deleteFriend.setVisible(it.isFriend)
                    // 拉黑按钮
                    binding.tvBlockState.setText(R.string.chat_add_black_list)
                    binding.tvBlockMessage.text = ""

                    val drawable = ContextCompat.getDrawable(this, R.drawable.ic_edit)?.apply {
                        setBounds(0, 0, minimumWidth, minimumHeight)
                    }
                    binding.tvRemark.setCompoundDrawables(null, null, drawable, null)
                    binding.tvRemark.compoundDrawablePadding = 20

                    binding.svNoDisturb.isOpened = it.noDisturb
                    binding.svStickTop.isOpened = it.stickTop

                    if (!viewModel.hasCompany()) {
                        var group: Server? = null
                        for (gid in it.groups) {
                            group = viewModel.servers.value?.find { server -> server.id == gid }
                            if (group != null) break
                        }
                        if (group == null) {
                            // 如果没有分组则显示默认分组
                            group = viewModel.servers.value?.firstOrNull()
                        }
                        if (group != null) {
                            binding.tvServerName.text = group.name
                            binding.tvServerAddress.text = group.address.urlKey()
                            connectionManager.observeSocketState(this) {
                                val id = if (connectionManager.getChatSocket(group.address.urlKey())?.isAlive == true) {
                                    R.drawable.ic_server_connect
                                } else {
                                    R.drawable.ic_server_disconnect
                                }

                                val status = ResourcesCompat.getDrawable(resources, id, null)?.apply {
                                    setBounds(0, 0, minimumWidth, minimumHeight)
                                }
                                binding.tvServerName.setCompoundDrawables(null, null, status, null)
                            }
                        }
                    } else {
                        val companyInfo = viewModel.companyUser.value?.company
                        if (companyInfo != null) {
                            binding.tvServerName.text = companyInfo.name
                            binding.tvServerAddress.text = companyInfo.imServer.urlKey()
                            connectionManager.observeSocketState(this) {
                                val id = if (connectionManager.getChatSocket(companyInfo.imServer.urlKey())?.isAlive == true) {
                                    R.drawable.ic_server_connect
                                } else {
                                    R.drawable.ic_server_disconnect
                                }

                                val status = ResourcesCompat.getDrawable(resources, id, null)?.apply {
                                    setBounds(0, 0, minimumWidth, minimumHeight)
                                }
                                binding.tvServerName.setCompoundDrawables(null, null, status, null)
                            }
                        }
                    }

                }
            }
            if (groupId != 0L) {
                // 从群里点进来，需要先限制显示内容
                setupAddFriend(groupInfo, friendUser)
            } else {
                // 更新好友头像
                MessageSubscription.onUpdateContact(it)
            }
        }
        viewModel.companyUserInfo.observe(this) { user ->
            binding.companyCard.root.gone()
            if (user != null && user.name.isNotEmpty()) {
                // 团队姓名为空，说明对方隐藏了团队信息
                binding.companyCard.root.visible()
                binding.companyCard.tvStaffName.text = user.name
                binding.companyCard.tvCompanyName.text = user.company?.name
                binding.companyCard.ivPhone.setImageResource(R.drawable.ic_contact_info_visible)
                binding.companyCard.ivShortNum.setImageResource(R.drawable.ic_contact_info_visible)
                if (user.phone.isNullOrEmpty()) {
                    binding.companyCard.callPhone.gone()
                    binding.companyCard.dividerPhone.gone()
                    binding.companyCard.tvPhone.text = ""
                } else {
                    binding.companyCard.callPhone.visible()
                    binding.companyCard.dividerPhone.visible()
                    binding.companyCard.tvPhone.text = user.phone?.let { encryptPhone(it) }
                }
                if (user.shortPhone.isNullOrEmpty()) {
                    binding.companyCard.callShortNum.gone()
                    binding.companyCard.dividerShortNum.gone()
                    binding.companyCard.tvShortNum.text = ""
                } else {
                    binding.companyCard.callShortNum.visible()
                    binding.companyCard.dividerShortNum.visible()
                    binding.companyCard.tvShortNum.text = user.shortPhone?.let { encryptString(it, 0, it.length) }
                }
                binding.companyCard.tvEmailName.text = user.email ?: "无"
                binding.companyCard.tvDepName.text = user.depName
                binding.companyCard.tvPosName.text = user.position ?: "无"

                binding.companyCard.callPhone.setOnClickListener {
                    if (binding.companyCard.tvPhone.text != user.phone) {
                        binding.companyCard.tvPhone.text = user.phone
                        binding.companyCard.ivPhone.setImageResource(R.drawable.ic_contact_info_phone)
                    } else {
                        instance.callPhone(user.phone)
                    }
                }
                binding.companyCard.callShortNum.setOnClickListener {
                    if (binding.companyCard.tvShortNum.text != user.shortPhone) {
                        binding.companyCard.tvShortNum.text = user.shortPhone
                        binding.companyCard.ivShortNum.setImageResource(R.drawable.ic_contact_info_phone)
                    } else {
                        instance.callPhone(user.shortPhone)
                    }
                }
            } else {
                binding.companyCard.root.gone()
            }
        }
        viewModel.groupRelativeInfo.observe(this) {
            binding.slMute.visible()
            groupInfo = it.first
            groupUser = it.second
            val groupInfo = it.first
            val groupUser = it.second
            counter?.cancel()
            if (groupUser.nickname.isNotEmpty()) {
                binding.tvGroupNickname.visible()
                binding.tvGroupNickname.text = getString(R.string.chat_tips_group_nickname_placeholder, groupUser.nickname)
            } else {
                binding.tvGroupNickname.gone()
            }
            if (groupUser.role == GroupUser.LEVEL_USER) {
                if (groupInfo.isMuteAll) {
                    binding.tvMuteTime.setText(R.string.chat_tips_mute_state2)
                } else {
                    when {
                        groupUser.muteTime == ChatConst.MUTE_FOREVER -> {
                            binding.tvMuteTime.setText(R.string.chat_tips_mute_state4)
                        }
                        groupUser.muteTime > System.currentTimeMillis() -> {
                            counter = MuteCountDown(groupUser.muteTime - System.currentTimeMillis(), 1000L)
                            counter?.start()
                        }
                        else -> binding.tvMuteTime.setText(R.string.chat_tips_mute_state1)
                    }
                }
            } else {
                binding.tvMuteTime.setText(R.string.chat_tips_mute_state1)
            }
            setupAddFriend(groupInfo, friendUser)
        }
        viewModel.modifyGroup.observe(this) {
            toast(R.string.chat_tips_modify_group_success)
            refreshUserInfo()
        }
//        viewModel.addFriendResult.observe(this) {
//            toast(R.string.chat_tips_add_friend_success)
//            refreshUserInfo()
//        }
        viewModel.deleteFriendResult.observe(this) {
            toast(R.string.chat_tips_delete_friend_success)
            dismiss()
            ARouter.getInstance().build(AppModule.MAIN).navigation()
        }
        viewModel.blockUserResult.observe(this) {
            toast(R.string.chat_add_black_success)
            dismiss()
            ARouter.getInstance().build(AppModule.MAIN).navigation()
        }
        viewModel.unBlockUserResult.observe(this) {
            toast(R.string.chat_remove_black_success)
            refreshUserInfo()
        }
        viewModel.muteResult.observe(this) {
            if (groupId != 0L) {
                viewModel.getGroupRelativeInfo(groupId, address ?: "")
            }
        }
    }

    private fun setupAddFriend(info: GroupInfo?, userInfo: FriendUser?) {
        if (info != null && userInfo != null) {
            val self = userInfo.address == delegate.getAddress()
            if (info.groupRole == GroupUser.LEVEL_USER) {
                binding.tvAddress.setVisible(info.canAddFriend)
                binding.slFriendSetting.setVisible(!self && (info.canAddFriend || userInfo.isFriend))
                binding.slServerGroup.setVisible(!self && (info.canAddFriend || userInfo.isFriend))
                binding.slBlock.setVisible(!self && (info.canAddFriend || userInfo.isFriend || userInfo.isBlock))
                binding.addFriend.setVisible(!self && !userInfo.isFriend && !userInfo.isBlock && info.canAddFriend)
            } else {
                // 群主和管理员任何时候都可以添加群成员好友
                binding.tvAddress.visible()
                binding.slFriendSetting.setVisible(!self)
                binding.slServerGroup.setVisible(!self)
                binding.slBlock.setVisible(!self)
                binding.addFriend.setVisible(
                    !self && !userInfo.isFriend && !userInfo.isBlock
                )
            }
            groupUser?.also { user ->
                // 更新好友头像
                MessageSubscription.onUpdateContact(
                    GroupUser(
                        info.gid.toString(), user.address, user.role, user.flag,
                        user.nickname, user.muteTime, userInfo.nickname, userInfo.avatar,
                        userInfo.remark, null
                    )
                )
            }
        } else {
            binding.tvAddress.gone()
            binding.slFriendSetting.gone()
            binding.slServerGroup.gone()
            binding.slBlock.gone()
            binding.addFriend.gone()
        }
        userInfo?.also {
            checkSendAndTransfer(
                it,
                //如果[群内可以加好友]或[身份是管理员或群主]则不需要是好友就能显示发消息和转账
                ((info?.canAddFriend ?: false) || info.groupRole > GroupUser.LEVEL_USER)
            )
        }
    }

    /**
     * 确认发送和转账按钮的状态
     *
     * @param info                  用户信息
     * @param showWhenNotFriends    不是好友情况下是否显示（群里禁止加好友的情况下）
     */
    private fun checkSendAndTransfer(info: FriendUser, showWhenNotFriends: Boolean) {
        val self = info.address == delegate.getAddress()
        binding.sendMsg.setVisible(!self && !disableSend && (info.isFriend || showWhenNotFriends) && !info.isBlock)
        binding.voiceCall.setVisible(!self && (info.isFriend || showWhenNotFriends) && !info.isBlock)
        binding.videoCall.setVisible(!self && (info.isFriend || showWhenNotFriends) && !info.isBlock)
        if (FunctionModules.enableWallet) {
            binding.transfer.setVisible(!self && (info.isFriend || showWhenNotFriends) && !info.isBlock)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUserInfo()
    }

    private fun refreshUserInfo() {
        address?.also {
            viewModel.getUser(it)
            if (groupId != 0L) {
                viewModel.getGroupRelativeInfo(groupId, it)
            }
        }
    }

    override fun setEvent() {
        binding.ivAvatar.setOnClickListener(this)
        binding.svNoDisturb.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
            override fun toggleToOn(view: SwitchView?) {
                address?.also {
                    viewModel.changeNoDisturb(it, true)
                    view?.toggleSwitch(true)
                }
            }

            override fun toggleToOff(view: SwitchView?) {
                address?.also {
                    viewModel.changeNoDisturb(it, false)
                    view?.toggleSwitch(false)
                }
            }
        })
        binding.svStickTop.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
            override fun toggleToOn(view: SwitchView?) {
                address?.also {
                    viewModel.changeStickTop(it, true)
                    view?.toggleSwitch(true)
                }
            }

            override fun toggleToOff(view: SwitchView?) {
                address?.also {
                    viewModel.changeStickTop(it, false)
                    view?.toggleSwitch(false)
                }
            }
        })
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.ivAvatar.setOnClickListener(this)
        binding.tvRemark.setOnClickListener(this)
        binding.llChatHistory.setOnClickListener(this)
        binding.llFiles.setOnClickListener(this)
        binding.llServerGroup.setOnClickListener(this)
        binding.llBlock.setOnClickListener(this)
        binding.sendMsg.setOnClickListener(this)
        binding.transfer.setOnClickListener(this)
        binding.voiceCall.setOnClickListener(this)
        binding.videoCall.setOnClickListener(this)
        binding.deleteFriend.setOnClickListener(this)
        binding.addFriend.setOnClickListener(this)
        binding.llMute.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_avatar -> {
                val intent = Intent(this, MediaGalleryActivity::class.java).apply {
                    putExtra("images", arrayListOf(friendUser?.avatar ?: ""))
                    putExtra("index", 0)
                    putExtra("placeholder", R.mipmap.default_avatar_big)
                    putExtra("showGallery", true)
                }
                startActivity(
                    intent, ActivityOptionsCompat.makeSceneTransitionAnimation(
                        instance, v, "shareImage"
                    ).toBundle()
                )
            }
            R.id.tv_remark -> {
                if (friendUser?.isBlock != true/* && friendUser?.isFriend == true*/) {
                    ARouter.getInstance().build(MainModule.CONTACT_REMARK)
                        .withString("address", friendUser?.address)
                        .withString("remark", friendUser?.remark)
                        .navigation()
                }
            }
            R.id.ll_chat_history -> {
                ARouter.getInstance().build(MainModule.SEARCH_LOCAL_SCOPED)
                    .withInt("scope", SearchScope.CHAT_LOG)
                    .withSerializable("chatTarget", ChatTarget(ChatConst.PRIVATE_CHANNEL, address ?: ""))
                    .withBoolean("popKeyboard", true)
                    .navigation()
            }
            R.id.ll_files -> {
                ARouter.getInstance().build(MainModule.FILE_MANAGEMENT)
                    .withString("target", address)
                    .withInt("channelType", ChatConst.PRIVATE_CHANNEL)
                    .navigation()
            }
            R.id.ll_block -> {
                friendUser?.apply {
                    if (isBlock) {
                        viewModel.unBlockUser(address)
                    } else {
                        val content = getString(
                            R.string.chat_add_black_warn,
                            AppConfig.APP_ACCENT_COLOR_STR,
                            getDisplayName()
                        )
                        val dialog = EasyDialog.Builder()
                            .setHeaderTitle(getString(R.string.biz_tips))
                            .setBottomLeftText(getString(R.string.biz_cancel))
                            .setBottomRightText(getString(R.string.biz_confirm))
                            .setContent(Html.fromHtml(content))
                            .setBottomLeftClickListener(null)
                            .setBottomRightClickListener {
                                it.dismiss()
                                viewModel.blockUser(address)
                            }.create(this@ContactInfoActivity)
                        dialog.show()
                    }
                }
            }
            R.id.ll_server_group -> {
                if (!hasCompany) {
                    ARouter.getInstance().build(MainModule.CHOOSE_SERVER_GROUP).navigation(this, REQUEST_CHANGE_SERVER)
                }
            }
            R.id.ll_mute -> {
                // 至少为管理员才能操作禁言并且不能操作自己
                if (role > GroupUser.LEVEL_USER && friendUser?.address != delegate.getAddress()) {
                    val mutePopup = MutePopupWindow(this, PopupGroupUserMuteBinding.inflate(layoutInflater).root)
                    var timer: CountDownTimer? = null
                    if (groupUser?.muteTime ?: 0L == ChatConst.MUTE_FOREVER) {
                        mutePopup.setCountDownText(getString(R.string.chat_tips_mute_state4), true)
                        mutePopup.setTitle(groupUser?.getDisplayName(), ContextCompat.getColor(instance, R.color.biz_color_accent))
                    } else if (groupUser?.muteTime ?: 0L > System.currentTimeMillis()) {
                        timer = object : CountDownTimer(
                            (groupUser?.muteTime ?: 0L) - System.currentTimeMillis(),
                            1000L
                        ) {
                            override fun onTick(millisUntilFinished: Long) {
                                mutePopup.setCountDownText(
                                    getString(
                                        R.string.chat_tips_mute_state3,
                                        StringUtils.formatMutedTime(millisUntilFinished)
                                    ), true
                                )
                            }

                            override fun onFinish() {
                                mutePopup.setCountDownText("", false)
                                mutePopup.showCancelButton(groupUser?.muteTime ?: 0L > System.currentTimeMillis())
                            }
                        }
                        timer.start()
                        mutePopup.setTitle(groupUser?.getDisplayName(), ContextCompat.getColor(instance, R.color.biz_color_accent))
                    }
                    mutePopup.setOnTimeSelectListener { time ->
                        address?.also {
                            viewModel.muteUser(groupId, time, it)
                        }
                    }
                    mutePopup.setOnDismissListener { timer?.cancel() }
                    mutePopup.showCancelButton(groupUser?.muteTime ?: 0L > System.currentTimeMillis())
                    mutePopup.showAtLocation(binding.root, Gravity.CENTER, 0, 0)
                }
            }
            R.id.send_msg -> {
                ActivityUtils.popUpTo("com.fzm.chat.app.MainActivity")
                ARouter.getInstance().build(MainModule.CHAT)
                    .withInt("channelType", ChatConst.PRIVATE_CHANNEL)
                    .withString("address", address)
                    .navigation()
                LiveDataBus.of(BusEvent::class.java).changeTab().setValue(ChangeTabEvent(0, 0))
            }
            R.id.transfer -> {
                ARouter.getInstance().build(WalletModule.TRANSFER)
                    .withInt("transferType", ChatConst.TRANSFER)
                    .withString("target", address)
                    .navigation()
            }
            R.id.voice_call -> {
                ARouter.getInstance().build(RtcModule.VIDEO_CALL)
                    .withString("targetId", address)
                    .withInt("callType", RTCCalling.TYPE_CALL)
                    .withInt("rtcType", RTCCalling.TYPE_AUDIO_CALL)
                    .navigation()
            }
            R.id.video_call -> {
                ARouter.getInstance().build(RtcModule.VIDEO_CALL)
                    .withString("targetId", address)
                    .withInt("callType", RTCCalling.TYPE_CALL)
                    .withInt("rtcType", RTCCalling.TYPE_VIDEO_CALL)
                    .navigation()
            }
            R.id.delete_friend -> {
                val content = getString(
                    R.string.chat_dialog_delete_friend,
                    AppConfig.APP_ACCENT_COLOR_STR,
                    friendUser?.getDisplayName()
                )
                val dialog: EasyDialog = EasyDialog.Builder()
                    .setHeaderTitle(getString(R.string.biz_tips))
                    .setBottomLeftText(getString(R.string.biz_cancel))
                    .setBottomRightText(getString(R.string.biz_confirm))
                    .setContent(Html.fromHtml(content))
                    .setBottomLeftClickListener(null)
                    .setBottomRightClickListener { dialog ->
                        dialog.dismiss()
                        address?.also { viewModel.deleteFriend(it) }
                    }.create(this)
                dialog.show()
            }
            R.id.add_friend -> {
                address?.also {
                    ARouter.getInstance().build(MainModule.ADD_FRIEND)
                        .withString("address", it)
                        .withTransition(R.anim.biz_slide_bottom_in, 0)
                        .navigation(this)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CHANGE_SERVER) {
                data?.getParcelableExtra<ServerGroupInfo>("serverInfo")?.apply {
                    address?.also {
                        viewModel.addFriend(it, listOf(id))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        counter?.cancel()
    }

    /**
     * 倒计时控制类
     */
    inner class MuteCountDown(millisInFuture: Long, countDownInterval: Long) :
        CountDownTimer(millisInFuture, countDownInterval) {
        override fun onTick(millisUntilFinished: Long) {
            binding.tvMuteTime.text = getString(
                R.string.chat_tips_mute_state3,
                StringUtils.formatMutedTime(millisUntilFinished)
            )
        }

        override fun onFinish() {
            binding.tvMuteTime.setText(R.string.chat_tips_mute_state1)
        }
    }

}