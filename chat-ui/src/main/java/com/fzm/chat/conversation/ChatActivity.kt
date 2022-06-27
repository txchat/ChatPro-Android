package com.fzm.chat.conversation

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.SharedElementCallback
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.*
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.forjrking.lubankt.Luban
import com.fzm.arch.connection.socket.ConnectionManager
import com.fzm.arch.connection.utils.urlKey
import com.fzm.chat.R
import com.fzm.chat.adapter.DialogOption
import com.fzm.chat.adapter.DialogOptionAdapter
import com.fzm.chat.animation.RootViewDeferringInsetsCallback
import com.fzm.chat.animation.TranslateDeferringInsetsAnimationCallback
import com.fzm.chat.bean.SimpleFileBean
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.base.FunctionModules
import com.fzm.chat.biz.base.enableOA
import com.fzm.chat.biz.utils.onExplain
import com.fzm.chat.contract.OpenDocument
import com.fzm.chat.contract.SelectContactCard
import com.fzm.chat.conversation.adapter.MessageAdapter
import com.fzm.chat.conversation.adapter.msg.ChatBaseItem
import com.fzm.chat.core.at.AtManager
import com.fzm.chat.core.data.*
import com.fzm.chat.core.data.bean.*
import com.fzm.chat.core.data.model.*
import com.fzm.chat.core.data.po.*
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.core.logic.ServerManager
import com.fzm.chat.core.media.DownloadManager2
import com.fzm.chat.core.rtc.calling.RTCCalling
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.ActivityChatBinding
import com.fzm.chat.databinding.PopupGroupUserMuteBinding
import com.fzm.chat.media.manager.MediaManager
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.main.SimpleTx
import com.fzm.chat.router.oa.CompanyUser
import com.fzm.chat.router.oa.OAModule
import com.fzm.chat.router.oa.OAService
import com.fzm.chat.router.oa.isEmpty
import com.fzm.chat.router.redpacket.RedPacketModule
import com.fzm.chat.router.route
import com.fzm.chat.router.rtc.RtcModule
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.ui.MediaGalleryActivity
import com.fzm.chat.utils.ForwardHelper
import com.fzm.chat.utils.GlideEngine
import com.fzm.chat.utils.StringUtils
import com.fzm.chat.widget.*
import com.fzm.widget.dialog.EasyDialog
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.listener.OnResultCallbackListener
import com.permissionx.guolindev.PermissionX
import com.zjy.architecture.base.instance
import com.zjy.architecture.data.Result
import com.zjy.architecture.ext.*
import com.zjy.architecture.util.*
import com.zjy.architecture.util.other.BadgeUtil
import com.zjy.architecture.util.other.BarUtils
import dtalk.biz.Biz
import dtalk.biz.msg.Msg
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.collections.ArrayList

/**
 * @author zhengjy
 * @since 2020/12/23
 * Description:
 */
@Route(path = MainModule.CHAT)
class ChatActivity : BizActivity(), KeyboardHeightObserver, ChatInputView.IChatInputView,
    View.OnClickListener, AtManager.OnOpenAitListListener {

    companion object {
        private const val REQUEST_CODE_CAPTURE = 100
    }

    @JvmField
    @Autowired
    var channelType: Int = ChatConst.PRIVATE_CHANNEL

    @JvmField
    @Autowired
    var address: String? = null

    @JvmField
    @Autowired
    var name: String? = null

    @JvmField
    @Autowired
    var fromMsgId: String? = null

    /**
     * 聊天对象所在的服务器地址
     */
    val url: String
        get() = when {
            companyServer.isNotEmpty() -> companyServer
            contactServer.isNotEmpty() -> {
                if (showServerTips) {
                    if (channelType == ChatConst.PRIVATE_CHANNEL) {
                        toast("通过普通服务器发送")
                    }
                    showServerTips = false
                }
                contactServer
            }
            else -> {
                ""
            }
        }

    private var reference: Reference? = null

    private var showServerTips = true

    /**
     * 联系人服务器地址
     */
    private var contactServer = ""

    /**
     * 联系人的企业服务器地址
     */
    private var companyServer = ""

    private var groupInfo: GroupInfo? = null
    private var rawName: String? = null

    private var contact: Contact? = null

    /**
     * 输入框原先的状态
     */
    private var isInputEnabled = true

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var mAdapter: MessageAdapter

    private val keyboardHeightProvider by lazy { KeyboardHeightProvider(this) }
    private var oldScrollState: Int = 0

    private var touchMsgX = 0
    private var touchMsgY = 0

    private val viewModel by viewModel<ChatViewModel>()
    private val connect by inject<ConnectionManager>()
    private val oaService by route<OAService>(OAModule.SERVICE)

    private val binding by init {
        window?.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        findViewById<View>(android.R.id.content).transitionName = "session_card"
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        window?.sharedElementEnterTransition = buildContainerTransform()
        window?.sharedElementExitTransition = buildContainerTransform()
        ActivityChatBinding.inflate(layoutInflater)
    }

    override val root: View
        get() = binding.root

    override val darkStatusColor: Boolean = true

    /**
     * 未读消息数量
     */
    private var unReadMsgCount = 0

    /**
     * 新消息数量
     */
    private var newMsgCount = 0

    /**
     * 用于记录进入页面后收到和发送的消息总数
     */
    private var totalNewMsgCount = 0

    /**
     * 是否显示更多新消息按钮
     */
    private var canShowNewMsg = false

    /**
     * 用于控制显示隐藏未读消息按钮
     */
    private val unreadArrays = arrayOf(false, false)

    private val focusDialog by lazy {
        MessageFocusDialogFragment.create(address!!.toLong(), groupInfo?.groupRole ?: 0)
    }

    private lateinit var filePicker: ActivityResultLauncher<Array<String>>
    private lateinit var selectContactCard: ActivityResultLauncher<Int>

    private val rtcAdapter by lazy {
        DialogOptionAdapter(this).apply {
            setData(listOf(
                DialogOption("视频通话") {
                    startRTCCall(RTCCalling.TYPE_VIDEO_CALL)
                },
                DialogOption("语音通话") {
                    startRTCCall(RTCCalling.TYPE_AUDIO_CALL)
                }
            ))
        }
    }

    private val cardAdapter by lazy {
        DialogOptionAdapter(this).apply {
            setData(listOf(
                DialogOption("个人名片") {
                    selectContactCard.launch(SelectContactCard.PRIVATE)
                },
                DialogOption("群名片") {
                    selectContactCard.launch(SelectContactCard.GROUP)
                }
            ))
        }
    }

    private var atManager: AtManager? = null

    private lateinit var atDialog: AtSelectorDialogFragment

    override fun onOpenAitList() {
        if (!this::atDialog.isInitialized && address != null) {
            atDialog = AtSelectorDialogFragment.create(address!!.toLong(), groupInfo.groupRole)
            atDialog.setAtSelectListener {
                atManager?.onUserSelected(it, true)
                binding.inputView.postDelayed({
                    KeyboardUtils.showKeyboard(binding.inputView.getEditText())
                }, 200)
            }
        }
        atDialog.show(supportFragmentManager, "AT_SELECTOR")
    }

    private fun buildContainerTransform(): MaterialContainerTransform {
        return MaterialContainerTransform().apply {
            addTarget(android.R.id.content)
            setAllContainerColors(resources.getColor(R.color.biz_color_primary))
            duration = 300
        }
    }

    private fun registerContracts() {
        filePicker = registerForActivityResult(OpenDocument()) {
            if (it == null) return@registerForActivityResult
            val result = it.toString()
            lifecycleScope.launch {
                val info = FileUtils.queryFile(instance, result)
                val md5 = FileUtils.getMd5(instance, result)
                if (info != null) {
                    val size = info.getLong(MediaStore.MediaColumns.SIZE)
                    if (size == 0L) {
                        toast("不能发送空文件")
                        return@launch
                    }
                    sendFileMsg(
                        result,
                        info.getString(MediaStore.MediaColumns.DISPLAY_NAME) ?: "",
                        info.getLong(MediaStore.MediaColumns.SIZE),
                        md5
                    )
                } else {
                    toast(R.string.chat_tips_file_not_exist)
                }
            }
        }
        selectContactCard = registerForActivityResult(SelectContactCard()) { contact ->
            if (contact == null) return@registerForActivityResult
            val type = if (contact.getType() == ChatConst.PRIVATE_CHANNEL) 1 else 2
            val server = if (contact.getType() == ChatConst.PRIVATE_CHANNEL) null else contact.getServerList().firstOrNull()?.address
            val content = MessageContent.contactCard(type, contact.getId(), contact.getRawName(), contact.getDisplayImage(), server, viewModel.getAddress())
            val message = ChatMessage.create(viewModel.getAddress(), address, channelType, Biz.MsgType.ContactCard, content)
            viewModel.sendMessage(url, message)
        }
    }

    override fun setSystemBar() {
        BarUtils.setStatusBarColor(this, resources.getColor(R.color.biz_color_primary_dark), 0)
        BarUtils.setStatusBarLightMode(this, true)
        window?.navigationBarColor = resources.getColor(R.color.biz_color_primary)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        ARouter.getInstance().inject(this)
        registerContracts()
        binding.tvTitle.text = name
        keyboardHeightProvider.setKeyboardHeightObserver(this)
        binding.inputView.also {
            it.setKeyboardHeight(AppPreference.keyboardHeight)
            it.bind(this, this, channelType)
        }
        layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, true)
        // 如果使用stackFromEnd的话，windowInset动画执行结束后RecyclerView会偏移
        // 也可以覆盖onEnd方法在动画结束时，手动将RecyclerView滚动到底部
        // layoutManager.stackFromEnd = true
        binding.rvMessage.layoutManager = layoutManager
        mAdapter = MessageAdapter()
        mAdapter.loadMoreModule.setOnLoadMoreListener {
            val size = mAdapter.data.size
            if (size > 0) {
                viewModel.getMessageHistory(address ?: "", channelType, mAdapter.data[size - 1].datetime)
            }
        }
        mAdapter.setMessageEventListener(messageEventListener)
        binding.llMsgScrollUp.setOnClickListener {
            if (mAdapter.data.size >= unReadMsgCount + totalNewMsgCount) {
                binding.rvMessage.smoothScrollToPosition(unReadMsgCount + totalNewMsgCount)
                showUnReadMessage(false)
            } else {
                viewModel.getMessageHistory(
                    address ?: "",
                    channelType,
                    mAdapter.data[mAdapter.data.size - 1].datetime,
                    unReadMsgCount + totalNewMsgCount - mAdapter.data.size,
                    true
                )
            }
        }
        binding.llMsgScrollDown.setOnClickListener {
            showNewMoreMessage()
            binding.rvMessage.smoothScrollToPosition(0)
        }
        binding.rvMessage.adapter = mAdapter
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val deferringInsetsListener = object : RootViewDeferringInsetsCallback(
                persistentInsetTypes = WindowInsets.Type.systemBars(),
                deferredInsetTypes = WindowInsets.Type.ime()
            ) {
                override fun isExpand() = binding.inputView.isExpand()

                override fun closePanel() {
                    binding.inputView.hideBottomLayout(true)
                }
            }
            binding.root.setWindowInsetsAnimationCallback(deferringInsetsListener)
            binding.root.setOnApplyWindowInsetsListener(deferringInsetsListener)
            binding.rlContainer.setWindowInsetsAnimationCallback(
                object : TranslateDeferringInsetsAnimationCallback(
                    view = binding.rlContainer,
                    persistentInsetTypes = WindowInsets.Type.systemBars(),
                    deferredInsetTypes = WindowInsets.Type.ime(),
                ) {
                    override fun isExpand() = binding.inputView.isExpand()
                }
            )
            binding.flBottom.setWindowInsetsAnimationCallback(
                object : TranslateDeferringInsetsAnimationCallback(
                    view = binding.flBottom,
                    persistentInsetTypes = WindowInsets.Type.systemBars(),
                    deferredInsetTypes = WindowInsets.Type.ime(),
                    dispatchMode = DISPATCH_MODE_CONTINUE_ON_SUBTREE
                ) {
                    override fun isExpand() = binding.inputView.isExpand()
                }
            )
        } else {
            binding.root.setOnApplyWindowInsetsListener { v, insets ->
                v.updatePadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
                return@setOnApplyWindowInsetsListener insets
            }
            binding.rvMessage.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (oldScrollState == RecyclerView.SCROLL_STATE_IDLE && newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        KeyboardUtils.hideKeyboard(root)
                        binding.inputView.hideBottomLayout()
                    }
                    oldScrollState = newState
                }
            })
        }
        binding.rvMessage.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstPosition = layoutManager.findFirstVisibleItemPosition()
                val lastPosition = layoutManager.findLastVisibleItemPosition()
                // 如果lastPosition不是最老的那条消息
                // 并且未读消息超过1屏幕，显示定位按钮
                if (lastPosition < mAdapter.data.size - 1 && unReadMsgCount > lastPosition - firstPosition + 1) {
                    showUnReadMessage(true)
                } else {
                    showUnReadMessage(false)
                }
                //如果手动翻动2页或者浏览到第一条未读消息，隐藏定位按钮
                if (mAdapter.data.size - totalNewMsgCount > 2 * ChatConfig.PAGE_SIZE || lastPosition >= unReadMsgCount + totalNewMsgCount) {
                    showUnReadMessage(false)
                }
                // 当底部不可见消息超过1条时，显示新消息按钮
                canShowNewMsg = firstPosition > 1
                showNewMoreMessage()
            }
        })
        if (channelType == ChatConst.PRIVATE_CHANNEL) {
            binding.tvServer.gone()
        } else {
            binding.tvServer.visible()
            connect.observeSocketState(this) {
                updateSocketState()
            }
        }
    }

    override fun initData() {
        viewModel.setCurrentTarget(address)
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.unReadCount.observe(this) {
            unReadMsgCount = it
        }
        viewModel.scrollPosition.observe(this) {
            binding.rvMessage.smoothScrollToPosition(unReadMsgCount + totalNewMsgCount)
            showUnReadMessage(false)
        }
        viewModel.locatePosition.observe(this) {
            binding.rvMessage.scrollToPosition(it)
            binding.rvMessage.post {
                val holder = binding.rvMessage.findViewHolderForAdapterPosition(it)
                ObjectAnimator.ofInt(
                    holder?.itemView,
                    "backgroundColor",
                    resources.getColor(R.color.biz_color_primary_dark),
                    resources.getColor(R.color.biz_color_accent)
                ).apply {
                    duration = 500
                    repeatCount = 3
                    repeatMode = ValueAnimator.REVERSE
                    setEvaluator(ArgbEvaluator())
                    start()
                }
            }
        }

        viewModel.latestMessage.observe(this) {
            mAdapter.addData(it.toMutableList())
            if (it.size < ChatConfig.PAGE_SIZE) {
                mAdapter.loadMoreModule.isEnableLoadMore = false
            } else {
                mAdapter.loadMoreModule.isEnableLoadMore = true
                mAdapter.loadMoreModule.loadMoreComplete()
            }
            it.filter { video -> video.msgType == Biz.MsgType.Video_VALUE }.forEach { msg ->
                DownloadManager2.downloadToApp(msg).observe(this) { result ->
                    msg.progress = result.progress()
                    mAdapter.notifyItemChanged(
                        mAdapter.data.indexOf(msg),
                        bundleOf(ChatMessage.MSG_PROGRESS to msg.progress)
                    )
                }
            }
        }
        viewModel.scrollBottom.observe(this) {
            layoutManager.scrollToPosition(0)
        }
        viewModel.deleteMessage.observe(this) { msg ->
            EasyDialog.Builder()
                .setHeaderTitle(getString(R.string.biz_tips))
                .setContent(getString(R.string.chat_dialog_delete_message))
                .setBottomLeftText(getString(R.string.biz_cancel))
                .setBottomRightText(getString(R.string.biz_confirm))
                .setBottomRightClickListener {
                    it.dismiss()
                    viewModel.deleteMessage(msg)
                    MessageSubscription.onDeleteMessage(msg)
                }
                .create(this)
                .show()
        }
        viewModel.muteUser.observe(this) {
            val mutePopup = MutePopupWindow(instance, PopupGroupUserMuteBinding.inflate(layoutInflater).root)
            var timer: CountDownTimer? = null
            if (it.muteTime == ChatConst.MUTE_FOREVER) {
                mutePopup.setCountDownText(getString(R.string.chat_tips_mute_state4), true)
                mutePopup.setTitle(it.name, ContextCompat.getColor(instance, R.color.biz_color_accent))
            } else if (it.muteTime > System.currentTimeMillis()) {
                timer = object : CountDownTimer(it.muteTime - System.currentTimeMillis(), 1000L) {
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
                            mutePopup.showCancelButton(it.muteTime > System.currentTimeMillis())
                        }
                    }
                timer.start()
                mutePopup.setTitle(it.name, ContextCompat.getColor(instance, R.color.biz_color_accent))
            }
            mutePopup.setOnTimeSelectListener { time ->
                it.muteTime = time
                viewModel.muteUser(it)
            }
            mutePopup.setOnDismissListener { timer?.cancel() }
            mutePopup.showCancelButton(it.muteTime > System.currentTimeMillis())
            mutePopup.showAtLocation(binding.rvMessage, Gravity.CENTER, 0, 0)
        }
        viewModel.muteResult.observe(this) {
            mAdapter.data.forEach { msg ->
                if (msg.sender?.getId() == it.address) {
                    (msg.sender as GroupUser).muteTime = it.muteTime
                }
            }
            mAdapter.setList(mAdapter.data)
        }
        viewModel.muteTips.observe(this) {
            toast(it)
        }
        viewModel.selectMode.observe(this) {
            binding.titleLeft.setVisible(!it)
            binding.titleRight.setVisible(!it)
            binding.flChat.setVisible(!it)
            binding.cancelSelect.setVisible(it)
            binding.llMessageOptions.setVisible(it)
            if (it) {
                // 保存进入选择模式前的输入框状态
                isInputEnabled = binding.inputView.isEnabled
                binding.inputView.isEnabled = false
                binding.rlAnimation?.isEnabled = false
                KeyboardUtils.hideKeyboard(binding.inputView)
            } else {
                binding.inputView.isEnabled = isInputEnabled
                binding.rlAnimation?.isEnabled = isInputEnabled
            }
            mAdapter.setSelectable(it)
        }
        viewModel.forwardMsg.observe(this) {
            lifecycleScope.launch {
                val source = MessageSource.create(
                    channelType,
                    viewModel.getAddress() ?: "",
                    viewModel.toFriendUser().getRawName(),
                    address ?: "",
                    rawName ?: ""
                )
                val message = it.clone().apply { this.source = source }
                ForwardHelper.checkForwardFile(instance, arrayListOf(message)) { error, list ->
                    if (error != null) {
                        toast(error)
                    } else {
                        ARouter.getInstance().build(MainModule.CONTACT_SELECT)
                            .withSerializable("messages", list)
                            .navigation()
                    }
                }
            }
        }
        viewModel.refMsg.observe(this) {
            val topic = if (it.reference != null) it.reference!!.topic else it.logId
            setReferenceMsg(Reference(topic, it.logId).apply { refMsg = it })
            if (!it.isSendType && channelType == ChatConst.GROUP_CHANNEL) {
                atManager?.apply {
                    insertAitMember(
                        it.from,
                        it.sender?.getRawName() ?: it.from,
                        binding.inputView.getEditText().selectionStart
                    )
                }
            }
            binding.inputView.postDelayed({
                KeyboardUtils.showKeyboard(binding.inputView.getEditText())
            }, 150)
        }
        if (channelType == ChatConst.PRIVATE_CHANNEL) {
            viewModel.fetchUserInfo(address).observe(this) {
                if (it == null) {
                    setupUserInfo(FriendUser.empty(address))
                } else {
                    setupUserInfo(it)
                }
            }
            if (FunctionModules.enableOA) {
                viewModel.fetchCompanyUser(address).observe(this) {
                    if (it == null) {
                        setupCompanyUser(CompanyUser.EMPTY_COMPANY_USER)
                    } else {
                        setupCompanyUser(it)
                    }
                }
            }
        } else {
            atManager = AtManager(address ?: "")
            atManager?.setOnOpenAitListListener(this)
            binding.inputView.setAtManager(atManager)
            viewModel.fetchGroupInfo(address).observe(this) {
                groupInfo = it ?: GroupInfo.empty(address?.toLong() ?: 0L)
                groupInfo?.apply {
                    setupGroupInfo(this)
                }
            }
        }
        viewModel.setOnMessageListener { msgOption ->
            when (msgOption.option) {
                Option.ADD_MSG -> {
                    val message = msgOption.message
                    val log = mAdapter.data.find {
                        (it.logId == message.logId && message.hasSent)  // 列表中已有相同logId的已发送消息
                            || it.msgId == message.msgId            // 列表中已有相同msgId的未发送消息
                    }
                    if (log == null) {
                        addNewMessage(message)
                    } else {
                        val index = mAdapter.data.indexOf(log)
                        mAdapter.setData(index, message)
                        layoutManager.scrollToPosition(0)
                        clearMsgState()
                    }
                }
                Option.REMOVE_MSG -> {
                    val message = msgOption.message
                    val list = mAdapter.data
                    val itr = list.iterator()
                    while (itr.hasNext()) {
                        val msg = itr.next()
                        if (message.msgId == msg.msgId) {
                            val index = list.indexOf(msg)
                            itr.remove()
                            mAdapter.configMessageTime(mAdapter.data)
                            mAdapter.notifyItemRemoved(index)
                            if (index > 0) {
                                // 删除一条消息之后，它上面的消息showTime不变，下一条可能发生变化
                                mAdapter.notifyItemChanged(
                                    index - 1,
                                    bundleOf(ChatMessage.MSG_TIME to "")
                                )
                            }
                        } else if (msg.reference?.ref == message.logId) {
                            msg.reference?.refMsg = null
                            mAdapter.notifyItemChanged(
                                list.indexOf(msg),
                                bundleOf(ChatMessage.MSG_REFERENCE to msg.reference)
                            )
                        }
                    }
                }
                Option.REVOKE_MSG -> {
                    val message = msgOption.message
                    checkCancelReference(message)
                    mAdapter.data.forEachIndexed { index, item ->
                        if (item.logId == message.logId) {
                            mAdapter.setData(index, message)
                        } else if (item.reference?.ref == message.logId) {
                            item.reference?.refMsg = message
                            mAdapter.notifyItemChanged(
                                index,
                                bundleOf(ChatMessage.MSG_REFERENCE to item.reference)
                            )
                        }
                    }
                }
                Option.UPDATE_STATE -> {
                    val message = msgOption.message
                    val log = mAdapter.data.find {
                        (it.logId == message.logId && message.hasSent)  // 列表中已有相同logId的已发送消息
                            || it.msgId == message.msgId            // 列表中已有相同msgId的未发送消息
                    }
                    if (log == null) {
                        addNewMessage(message)
                    } else {
                        updateMessageState(log, message.state)
                    }
                }
                Option.UPDATE_CONTENT -> {
                    mAdapter.data.forEachIndexed { index, msg ->
                        if (msgOption.message.msgId == msg.msgId) {
                            msg.msg = msgOption.message.msg
                            mAdapter.notifyItemChanged(index)
                            return@forEachIndexed
                        }
                    }
                }
                Option.UPDATE_FOCUS -> {
                    mAdapter.data.forEachIndexed { index, msg ->
                        if (msgOption.focus.logId == msg.logId) {
                            msg.hasFocused = msgOption.focus.hasFocused
                            if (msgOption.focus.num > msg.focusUserNum) {
                                msg.focusNum = msgOption.focus.num
                                mAdapter.notifyItemChanged(index, bundleOf(ChatMessage.MSG_FOCUS to msg.focusUserNum))
                            }
                            return@forEachIndexed
                        }
                    }
                }
                Option.UPDATE_CONTACT -> {
                    val sender = msgOption.sender
                    if (sender.getType() == ChatConst.GROUP_CHANNEL) return@setOnMessageListener
                    mAdapter.data.forEachIndexed { index, msg ->
                        if (sender.getId() == msg.from) {
                            val bundle = Bundle()
                            if (sender.getDisplayName() != msg.sender?.getDisplayName()) {
                                bundle.putString(ChatMessage.MSG_NICKNAME, sender.getDisplayName())
                            }
                            if (sender.getDisplayImage() != msg.sender?.getDisplayImage()) {
                                bundle.putString(ChatMessage.MSG_AVATAR, sender.getDisplayImage())
                            }
                            if (!bundle.isEmpty) {
                                msg.sender = sender
                                mAdapter.notifyItemChanged(index, bundle)
                            }
                        }
                    }
                }
            }
        }
        if (fromMsgId.isNullOrEmpty()) {
            viewModel.getMessageHistory(address ?: "", channelType)
        } else {
            // 跳转到指定logId消息
            viewModel.getMessageHistoryToLogId(address ?: "", channelType, fromMsgId!!)
        }
        viewModel.getUnreadCount(address ?: "", channelType)
        MediaManager.setOnPlayStateChangedListener(this) { playing ->
            if (playing) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                val index = mAdapter.data.indexOf(currentAudioMessage)
                if (index != -1) {
                    (mAdapter.getViewByPosition(index, R.id.icon_voice) as? IconView)?.stop()
                }
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        loadDraft()
    }

    private fun addNewMessage(message: ChatMessage) {
        newMsgCount++
        totalNewMsgCount++
        val index = findNewMessageIndex(message)
        if (index != -1) {
            mAdapter.addData(index, message)
            if (canShowNewMsg) {
                showNewMoreMessage()
            } else {
                layoutManager.scrollToPosition(0)
            }
            clearMsgState()
        }
    }

    private fun updateMessageState(message: ChatMessage, state: Int) {
        message.state = state
        // 局部刷新会引起滚动动画有些问题
        lifecycleScope.launch {
            while (binding.rvMessage.isAnimating) {
                delay(50)
            }
            launch {
                // 等动画结束之后再刷新
                mAdapter.notifyItemChanged(
                    mAdapter.data.indexOf(message),
                    bundleOf(ChatMessage.MSG_STATE to state)
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateSocketState() {
        val server = viewModel.servers.value?.firstOrNull { it.address.urlKey() == url.urlKey() }
        if (server != null) {
            binding.tvServer.text = "${server.name}(${server.address.urlKey()})"
        }
        val socket = connect.getChatSocket(url)
        when {
            socket == null -> {
                binding.inputView.gone()
                binding.llGroupChatServer.visible()
                binding.tvAddServer.mediumBold()
                binding.tvAddServer.setOnClickListener {
                    ARouter.getInstance().build(MainModule.EDIT_SERVER_GROUP).navigation()
                }
                binding.tvServer.setTextColor(resources.getColor(R.color.biz_text_grey_light))
                binding.tvServer.setBackgroundResource(R.drawable.shape_bg_grey_text)
            }
            socket.isAlive -> {
                binding.inputView.visible()
                binding.llGroupChatServer.gone()
                binding.tvServer.setTextColor(resources.getColor(R.color.biz_green_tips))
                binding.tvServer.setBackgroundResource(R.drawable.shape_bg_green_text)
            }
            else -> {
                binding.inputView.visible()
                binding.llGroupChatServer.gone()
                binding.tvServer.setTextColor(resources.getColor(R.color.biz_red_tips))
                binding.tvServer.setBackgroundResource(R.drawable.shape_bg_red_text)
            }
        }
    }

    private var doingDownAnimation = false

    /**
     * 显示或隐藏未读按钮
     */
    private fun showUnReadMessage(show: Boolean) {
        if (show) {
            if (unreadArrays[0]) return
            unreadArrays[0] = true
            if (!binding.llMsgScrollUp.isVisible) {
                val translate = PropertyValuesHolder.ofFloat("translationX", 150.dp.toFloat(), 0f)
                val alpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
                ObjectAnimator.ofPropertyValuesHolder(binding.llMsgScrollUp, translate, alpha).apply {
                    duration = 500
                    interpolator = AccelerateDecelerateInterpolator()
                    doOnStart { binding.llMsgScrollUp.visible() }
                    start()
                }
            }
            if (unReadMsgCount > 999) {
                binding.tvMsgScrollUp.text =
                    getString(R.string.chat_tips_new_msg, "999+")
            } else {
                binding.tvMsgScrollUp.text =
                    getString(R.string.chat_tips_new_msg, unReadMsgCount.toString())
            }
        } else {
            if (unreadArrays[1]) return
            //全置为true，保证到这里后，不会再show
            unreadArrays.fill(true)
            if (binding.llMsgScrollUp.isVisible) {
                val translate = PropertyValuesHolder.ofFloat("translationX", 0f, 150.dp.toFloat())
                val alpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
                ObjectAnimator.ofPropertyValuesHolder(binding.llMsgScrollUp, translate, alpha).apply {
                    duration = 500
                    interpolator = AccelerateDecelerateInterpolator()
                    doOnEnd { binding.llMsgScrollUp.gone() }
                    start()
                }
            }
        }
    }

    /**
     * 显示或隐藏新消息按钮
     */
    private fun showNewMoreMessage() {
        if (canShowNewMsg && newMsgCount != 0) {
            if (binding.llMsgScrollDown.isGone && !doingDownAnimation) {
                val translate = PropertyValuesHolder.ofFloat("translationY", 100.dp.toFloat(), 0f)
                val alpha = PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
                ObjectAnimator.ofPropertyValuesHolder(binding.llMsgScrollDown, translate, alpha).apply {
                    duration = 500
                    interpolator = AccelerateDecelerateInterpolator()
                    doOnStart {
                        doingDownAnimation = true
                        binding.llMsgScrollDown.visible()
                    }
                    doOnEnd { doingDownAnimation = false }
                    start()
                }
            }
            if (newMsgCount > 999) {
                binding.tvMsgScrollDown.text = getString(R.string.chat_tips_new_msg, "999+")
            } else {
                binding.tvMsgScrollDown.text =
                    getString(R.string.chat_tips_new_msg, newMsgCount.toString())
            }
        } else {
            if (binding.llMsgScrollDown.isVisible && !doingDownAnimation) {
                val translate = PropertyValuesHolder.ofFloat("translationY", 0f, 100.dp.toFloat())
                val alpha = PropertyValuesHolder.ofFloat("alpha", 1f, 0f)
                ObjectAnimator.ofPropertyValuesHolder(binding.llMsgScrollDown, translate, alpha).apply {
                    duration = 500
                    interpolator = AccelerateDecelerateInterpolator()
                    doOnStart { doingDownAnimation = true }
                    doOnEnd {
                        doingDownAnimation = false
                        binding.llMsgScrollDown.gone()
                    }
                    start()
                }
            }
            newMsgCount = 0
        }
    }

    /**
     * 确定新消息插入的位置
     */
    private fun findNewMessageIndex(message: ChatMessage): Int {
        if (mAdapter.data.isEmpty() || message.logId == 0L) {
            return 0
        }
        if (message.logId > mAdapter.data.first().logId) {
            return 0
        }
        if (message.logId < mAdapter.data.last().logId) {
            return -1
        }
        mAdapter.data.forEachIndexed { index, item ->
            if (item.logId == 0L) {
                if (message.datetime > item.datetime) {
                    return index
                }
            } else {
                if (message.logId > item.logId) {
                    return index
                }
            }
        }
        return -1
    }

    override fun setEvent() {
        binding.titleLeft.setOnClickListener(this)
        binding.cancelSelect.setOnClickListener(this)
        binding.titleRight.setOnClickListener(this)
        binding.llForward.setOnClickListener(this)
        binding.llBatchForward.setOnClickListener(this)
        binding.llDelete.setOnClickListener(this)
    }

    /**
     * 开始音视频通话
     */
    private fun startRTCCall(rtcType: Int) {
        ARouter.getInstance().build(RtcModule.VIDEO_CALL)
            .withString("targetId", address)
            .withInt("callType", RTCCalling.TYPE_CALL)
            .withInt("rtcType", rtcType)
            .navigation()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.title_left -> onBackPressed()
            R.id.cancel_select -> viewModel.requestSelect(false)
            R.id.title_right -> {
                if (channelType == ChatConst.PRIVATE_CHANNEL) {
                    ARouter.getInstance().build(MainModule.CONTACT_INFO)
                        .withString("address", address)
                        .withBoolean("disableSend", true)
                        .navigation()
                } else {
                    ARouter.getInstance().build(MainModule.GROUP_INFO)
                        .withLong("groupId", address?.toLong() ?: 0L)
                        .navigation()
                }
            }
            R.id.ll_forward -> {
                val messages = ArrayList(
                    mAdapter.data
                        .filter { it.isSelected }
                        .sortedBy { it.datetime }
                )
                val source = MessageSource.create(
                    channelType,
                    viewModel.getAddress() ?: "",
                    viewModel.toFriendUser().getRawName(),
                    address ?: "",
                    rawName ?: ""
                )
                val copy = ArrayList(messages.filter {
                    it.canForward
                }.map {
                    it.clone().apply { this.source = source }
                })
                if (messages.size != copy.size) {
                    EasyDialog.Builder()
                        .setHeaderTitle(getString(R.string.biz_tips))
                        .setContent("你选择的消息中，语音、红包、转账、特殊消息不能转发给朋友，是否继续")
                        .setBottomRightText("发送")
                        .setBottomRightClickListener {
                            forward(copy)
                        }
                        .setBottomLeftText(getString(R.string.biz_cancel))
                        .create(this)
                        .show()
                } else {
                    forward(copy)
                }
            }
            R.id.ll_batch_forward -> {
                lifecycleScope.launch {
                    val preList = mAdapter.data.filter { it.isSelected && it.canBatchForward }.sortedBy { it.datetime }
                    ForwardHelper.checkForwardFile(instance, ArrayList(preList), false) { error, list ->
                        if (error != null) {
                            toast(error)
                            return@checkForwardFile
                        }
                        val forwardList = list.map { it.toForwardMsg() }
                        if (forwardList.isEmpty()) {
                            toast(R.string.chat_tips_selected_message_empty)
                            return@checkForwardFile
                        }
                        val source = MessageSource.create(
                            channelType,
                            viewModel.getAddress() ?: "",
                            viewModel.toFriendUser().getRawName(),
                            address ?: "",
                            rawName ?: ""
                        )
                        val params = PreSendParams(
                            Biz.MsgType.Forward_VALUE,
                            MessageContent.forward(instance, forwardList),
                            source
                        )
                        ARouter.getInstance().build(MainModule.CONTACT_SELECT)
                            .withSerializable("preSend", params)
                            .navigation()
                    }
                }
            }
            R.id.ll_delete -> {
                EasyDialog.Builder()
                    .setHeaderTitle(getString(R.string.biz_tips))
                    .setContent(getString(R.string.chat_dialog_delete_messages))
                    .setBottomLeftText(getString(R.string.biz_cancel))
                    .setBottomRightText(getString(R.string.biz_confirm))
                    .setBottomRightClickListener {
                        it.dismiss()
                        val messages = mAdapter.data
                            .filter { msg -> msg.isSelected }
                        if (messages.isEmpty()) {
                            toast(R.string.chat_tips_selected_message_empty)
                            return@setBottomRightClickListener
                        }
                        val logs = messages.map { msg -> msg.msgId to msg.logId }
                        viewModel.deleteMessage(channelType, address ?: "", logs)
                        messages.forEach { msg ->
                            MessageSubscription.onDeleteMessage(msg)
                        }
                        viewModel.requestSelect(false)
                    }
                    .create(this)
                    .show()
            }
        }
    }

    private fun forward(message: ArrayList<ChatMessage>) {
        lifecycleScope.launch {
            ForwardHelper.checkForwardFile(instance, message) { error, list ->
                if (error != null) {
                    toast(error)
                } else {
                    ARouter.getInstance().build(MainModule.CONTACT_SELECT)
                        .withSerializable("messages", list)
                        .navigation()
                }
            }
        }
    }

    private fun setupUserInfo(user: FriendUser) {
        this.contact = user
        this.name = user.getDisplayName()
        this.rawName = user.getRawName()
        if (user.servers.isNotEmpty()) {
            contactServer = user.servers[0].address
            if (contactServer.isNotEmpty()) {
                ServerManager.changeLocalChatServer(contactServer)
            }
        }
        binding.tvTitle.text = name
        if (user.noDisturb) {
            val drawable =
                ResourcesCompat.getDrawable(resources, R.drawable.icon_disturb, null)?.apply {
                    setBounds(0, 0, minimumWidth, minimumHeight)
                }
            binding.tvTitle.setCompoundDrawables(null, null, drawable, null)
            binding.tvTitle.compoundDrawablePadding = 5.dp
        } else {
            binding.tvTitle.setCompoundDrawables(null, null, null, null)
        }
    }

    private fun setupCompanyUser(user: CompanyUser) {
        if (user.isEmpty) return
        companyServer = user.company?.imServer ?: ""
        if (companyServer.isNotEmpty()) {
            ServerManager.changeLocalChatServer(companyServer)
        }
    }

    private fun setupGroupInfo(group: GroupInfo) {
        this.contact = group
        this.name = group.getDisplayName()
        this.rawName = group.getRawName()
        if (group.server.address.isNotEmpty()) {
            contactServer = group.server.address
            updateSocketState()
            if (contactServer.isNotEmpty()) {
                ServerManager.changeLocalChatServer(contactServer)
            }
        }
        binding.tvTitle.text = "$name(${group.memberNum})"
        if (group.noDisturb) {
            val drawable =
                ResourcesCompat.getDrawable(resources, R.drawable.icon_disturb, null)?.apply {
                    setBounds(0, 0, minimumWidth, minimumHeight)
                }
            binding.tvTitle.setCompoundDrawables(null, null, drawable, null)
            binding.tvTitle.compoundDrawablePadding = 5.dp
        } else {
            binding.tvTitle.setCompoundDrawables(null, null, null, null)
        }
        when (group.groupType) {
            GroupInfo.TYPE_NORMAL -> {
                binding.tvGroupType.gone()
            }
            GroupInfo.TYPE_TEAM -> {
                binding.tvGroupType.text = "全员"
                binding.tvGroupType.visible()
            }
            GroupInfo.TYPE_DEPART -> {
                binding.tvGroupType.text = "部门"
                binding.tvGroupType.visible()
            }
        }
        binding.titleRight.setVisible(group.isInGroup)
        binding.inputView.setVisible(group.isInGroup)
        muteTimer?.cancel()
        if (group.isInGroup) {
            if (group.groupRole == GroupUser.LEVEL_USER) {
                if (group.isMuteAll) {
                    binding.inputView.isEnabled = false
                    binding.rlAnimation?.isEnabled = false
                    binding.flMute.visible()
                    window?.navigationBarColor = resources.getColor(R.color.biz_mute_mask)
                    binding.tvMute.setText(R.string.chat_tips_mute_state2)
                } else {
                    if (group.person?.muteTime ?: 0L < System.currentTimeMillis()) {
                        binding.inputView.isEnabled = true
                        binding.rlAnimation?.isEnabled = true
                        binding.flMute.gone()
                        window?.navigationBarColor = resources.getColor(R.color.biz_color_primary)
                    } else {
                        binding.inputView.isEnabled = false
                        binding.rlAnimation?.isEnabled = false
                        binding.flMute.visible()
                        window?.navigationBarColor = resources.getColor(R.color.biz_mute_mask)
                        if (group.person?.muteTime == ChatConst.MUTE_FOREVER) {
                            binding.tvMute.setText(R.string.chat_tips_mute_state4)
                        } else {
                            muteTimer = MuteTimer((group.person?.muteTime ?: 0L) - System.currentTimeMillis())
                            muteTimer?.start()
                        }
                    }
                }
            } else {
                binding.inputView.isEnabled = true
                binding.rlAnimation?.isEnabled = true
                binding.flMute.gone()
                window?.navigationBarColor = resources.getColor(R.color.biz_color_primary)
            }
        } else {
            binding.inputView.isEnabled = false
            binding.rlAnimation?.isEnabled = false
            binding.flMute.gone()
            window?.navigationBarColor = resources.getColor(R.color.biz_color_primary)
        }
    }

    private var muteTimer: MuteTimer? = null

    inner class MuteTimer(millisInFuture: Long) : CountDownTimer(millisInFuture, 1000L) {
        override fun onTick(millisUntilFinished: Long) {
            binding.tvMute.text = getString(
                R.string.chat_tips_mute_state3,
                StringUtils.formatMutedTime(millisUntilFinished)
            )
        }

        override fun onFinish() {
            binding.inputView.isEnabled = true
            binding.flMute.gone()
            window?.navigationBarColor = resources.getColor(R.color.biz_color_primary)
        }
    }

    override fun onLayoutAnimation(appear: Boolean) {
        val transition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(Fade(Fade.OUT))
            addTransition(ChangeBounds())
            addTransition(Fade(Fade.IN))
            this.duration = 250
            interpolator = DecelerateInterpolator()
        }
        TransitionManager.beginDelayedTransition(binding.root, transition)
    }

    override fun onSend(view: View, content: String) {
        if (url.isEmpty()) {
            toast(R.string.chat_tips_servers_empty)
            return
        }
        val message = ChatMessage.create(
            viewModel.getAddress(),
            address,
            channelType,
            Biz.MsgType.Text,
            MessageContent.text(content, atManager?.aitMembers),
            reference = reference
        )
        viewModel.sendMessage(url, message)
        clearReferenceMsg()
        atManager?.reset()
    }

    override fun onSelectMedia(view: View) {
        PermissionX.init(this)
            .permissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .onExplain(this)
            .request { allGranted, _, _ ->
                if (allGranted) {
                    PictureSelector.create(this)
                        .openGallery(PictureMimeType.ofAll())
                        .isCompress(true)
                        .isGif(true)
                        .isCamera(false)
                        .theme(R.style.chat_picture_style)
                        .imageEngine(GlideEngine.createGlideEngine())
                        .forResult(object : OnResultCallbackListener<LocalMedia> {
                            override fun onResult(result: MutableList<LocalMedia>) {
                                if (result.isEmpty()) return
                                when {
                                    result[0].mimeType.startsWith("image/") -> {
                                        onPhotoSelected(result)
                                    }
                                    result[0].mimeType.startsWith("video/") -> {
                                        onVideoSelected(result)
                                    }
                                    else -> {
                                        onFileSelected(result)
                                    }
                                }
                            }

                            override fun onCancel() {

                            }
                        })
                } else {
                    this.toast(com.fzm.chat.biz.R.string.biz_permission_not_granted)
                }
            }
    }

    override fun onCapture(view: View) {
        PermissionX.init(this)
            .permissions(
                listOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO
                )
            )
            .onExplain(this)
            .request { allGranted, _, _ ->
                if (allGranted) {
                    ARouter.getInstance().build(MainModule.CAPTURE)
                        .withInt("mode", 3)
                        .navigation(this, REQUEST_CODE_CAPTURE)
                } else {
                    toast(R.string.biz_permission_not_granted)
                }
            }
    }

    override fun onSnapChat(view: View) {

    }

    override fun onSelectFile(view: View) {
        PermissionX.init(this)
            .permissions(Manifest.permission.READ_EXTERNAL_STORAGE)
            .onExplain(this)
            .request { allGranted, _, _ ->
                if (allGranted) {
                    filePicker.launch(arrayOf("*/*"))
                } else {
                    Toast.makeText(this, R.string.permission_not_granted, Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    override fun onStartRtc(view: View) {
        PermissionX.init(this)
            .permissions(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .onExplain(this)
            .request { allGranted, _, _ ->
                if (allGranted) {
                    val bottomDialog = BottomListDialog.create(instance, rtcAdapter, 360.dp)
                    bottomDialog.apply {
                        setCancelVisible(true)
                        setOnItemClickListener { parent, _, position, _ ->
                            val option = parent.getItemAtPosition(position) as DialogOption
                            option.action.invoke()
                            bottomDialog.cancel()
                            this@ChatActivity.binding.inputView.hideBottomLayout()
                        }
                        show()
                    }
                } else {
                    toast(R.string.biz_permission_not_granted)
                }
            }
    }

    override fun onTransfer(view: View, transferType: Int) {
        ARouter.getInstance().build(WalletModule.TRANSFER)
            .withInt("transferType", transferType)
            .withString("target", address)
            .withBoolean("fromChat", true)
            .navigation()
    }

    override fun onRedPacket(view: View) {
        val type = if (channelType == ChatConst.PRIVATE_CHANNEL) {
            ChatConst.RED_PACKET_SINGLE
        } else {
            ChatConst.RED_PACKET_LUCKY
        }
        ARouter.getInstance().build(RedPacketModule.SEND_RED_PACKET)
            .withInt("packetType", type)
            .withSerializable("target", ChatTarget(channelType, address!!))
            .navigation()
    }

    override fun onContactCard(view: View) {
        val bottomDialog = BottomListDialog.create(instance, cardAdapter, 360.dp)
        bottomDialog.apply {
            setCancelVisible(true)
            setOnItemClickListener { parent, _, position, _ ->
                val option = parent.getItemAtPosition(position) as DialogOption
                option.action.invoke()
                bottomDialog.cancel()
                this@ChatActivity.binding.inputView.hideBottomLayout()
            }
            show()
        }
    }

    override fun scrollMessageToBottom() {
        layoutManager.scrollToPosition(0)
    }

    override fun switchInputType(type: Int, confirm: (Boolean) -> Unit) {
        if (type == ChatInputView.INPUT_KEYBOARD) {
            confirm.invoke(true)
        } else if (type == ChatInputView.INPUT_VOICE) {
            PermissionX.init(this)
                .permissions(Manifest.permission.RECORD_AUDIO)
                .onExplain(this)
                .request { allGranted, _, _ ->
                    if (allGranted) {
                        confirm.invoke(true)
                    } else {
                        confirm.invoke(false)
                        toast(R.string.biz_permission_not_granted)
                    }
                }
        }
    }

    override fun onAudioRecorderFinished(seconds: Float, filePath: String?) {
        val message = ChatMessage.create(
            viewModel.getAddress(),
            address,
            channelType,
            Biz.MsgType.Audio,
            MessageContent.audio(null, filePath, seconds.toInt())
        )
        viewModel.sendMessage(url, message)
    }

    override fun onResume() {
        super.onResume()
        setCustomDensity(this)
        clearMsgState()
        ChatConfig.CURRENT_TARGET = address
    }

    override fun onPause() {
        super.onPause()
        ChatConfig.CURRENT_TARGET = null
        saveDraft()
    }

    /**
     * 加载当前聊天的草稿
     */
    private fun loadDraft() {
        address?.also {
            viewModel.loadDraft(it, channelType).observe(this) { draft ->
                atManager?.setEnableTextChangedListener(false)
                binding.inputView.getEditText().setText(draft.text)
                atManager?.setEnableTextChangedListener(true)
                atManager?.restoreFrom(draft.atInfo)
                draft.reference?.also { ref ->
                    if (ref.refMsg != null) {
                        setReferenceMsg(ref)
                    }
                }
            }
        }
    }

    /**
     * 保存当前聊天的草稿
     */
    private fun saveDraft() {
        address?.also {
            val end = atManager?.aitInfo?.values?.maxOfOrNull { block -> block.lastSegmentEnd } ?: -1
            val text = binding.inputView.getEditText().text
            val draft = if (end + 1 == text.length) {
                // 如果草稿以@结尾，则不对text末位进行trim，防止@块被意外破坏
                text.trimStart().toString()
            } else {
                text.trim().toString()
            }
            viewModel.saveDraft(draft, atManager?.aitInfo, reference, it, channelType)
        }
    }

    /**
     * 检查是否需要取消消息引用
     */
    private fun checkCancelReference(message: ChatMessage) {
        if (reference?.ref == message.logId) {
            clearReferenceMsg()
            EasyDialog.Builder()
                .setHeaderTitle(getString(R.string.biz_tips))
                .setContent("此消息已撤回")
                .setBottomRightText(getString(R.string.biz_confirm))
                .create(this)
                .show()
        }
    }

    /**
     * 设置消息引用
     */
    private fun setReferenceMsg(ref: Reference) {
        reference = ref
        binding.inputView.setReferenceMsg("${ref.refMsg?.sender?.getDisplayName()}：${ref.refMsg?.getContent(this)}") {
            reference = null
        }
    }

    /**
     * 清空消息引用
     */
    private fun clearReferenceMsg() {
        reference = null
        binding.inputView.clearReferenceMsg()
    }

    /**
     * 清除当前会话的消息状态，如：未读数，@状态等
     */
    private fun clearMsgState() {
        address?.also {
            notificationManager?.cancel(it.hashCode())
            viewModel.clearUnread(it, channelType)
            lifecycleScope.launch {
                if (contact?.noDisturb == false) {
                    BadgeUtil.setBadgeCount(instance, viewModel.getUnreadCount() - unReadMsgCount)
                }
            }
            if (channelType == ChatConst.GROUP_CHANNEL) {
                viewModel.clearAtMsg(it, channelType)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        muteTimer?.cancel()
    }

    private fun onPhotoSelected(list: List<LocalMedia>) {
        list.forEach {
            if (it.isCompressed) {
                val message = ChatMessage.create(
                    viewModel.getAddress(),
                    address,
                    channelType,
                    Biz.MsgType.Image,
                    MessageContent.image(null, it.compressPath, intArrayOf(it.height, it.width))
                )
                viewModel.sendMessage(url, message)
            } else {
                if (it.path.isNullOrEmpty()) {
                    toast(R.string.chat_tips_img_not_exist)
                    return
                }
                val message = ChatMessage.create(
                    viewModel.getAddress(),
                    address,
                    channelType,
                    Biz.MsgType.Image,
                    MessageContent.image(null, it.path, intArrayOf(it.height, it.width))
                )
                viewModel.sendMessage(url, message)
            }
        }
    }

    private fun onVideoSelected(list: List<LocalMedia>) {
        list.forEach {
            if (it.path.isNullOrEmpty()) {
                toast(R.string.chat_tips_vid_not_exist)
                return
            }
            val message = ChatMessage.create(
                viewModel.getAddress(),
                address,
                channelType,
                Biz.MsgType.Video,
                MessageContent.video(
                    null,
                    it.path,
                    intArrayOf(it.height, it.width),
                    (it.duration / 1000).toInt()
                )
            )
            viewModel.sendMessage(url, message)
        }
    }

    private fun onFileSelected(list: List<LocalMedia>) {
        list.forEach {
            val md5 = FileUtils.getMd5(instance, it.path)
            sendFileMsg(it.path, it.fileName, it.size, md5)
        }
    }

    private fun sendFileMsg(path: String?, name: String, size: Long, md5: String) {
        if (md5.isEmpty() || path.isNullOrEmpty()) {
            toast(R.string.chat_tips_file_not_exist)
            return
        }
        val message = ChatMessage.create(
            viewModel.getAddress(),
            address,
            channelType,
            Biz.MsgType.File,
            MessageContent.file(null, path, name, size, md5)
        )
        viewModel.sendMessage(url, message)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_CAPTURE -> {
                    val type = data?.getIntExtra("type", 0)
                    if (type == 1) {
                        onCapturePhoto(data)
                    } else if (type == 2) {
                        onCaptureVideo(data)
                    }
                }
            }
        }
    }

    private fun onCapturePhoto(data: Intent) {
        val uri = data.getParcelableExtra<Uri>("result")
        if (uri == null) {
            toast(R.string.chat_tips_img_not_exist)
            return
        }
        Luban.with(this)
            .load(uri)
            .setOutPutDir(Utils.getImageDir(this))
            .compressObserver {
                onStart = {
                    loading(true)
                }
                onSuccess = {
                    val size = Utils.getImageSize(it.absolutePath)
                    val message = ChatMessage.create(
                        viewModel.getAddress(),
                        address,
                        channelType,
                        Biz.MsgType.Image,
                        MessageContent.image(null, it.path, intArrayOf(size[0], size[1]))
                    )
                    viewModel.sendMessage(url, message)
                }
                onError = { e, _ ->
                    toast(e.message.toString())
                }
                onCompletion = {
                    dismiss()
                }
            }
            .launch()
    }

    private fun onCaptureVideo(data: Intent) {
        val path = data.getStringExtra("video")
        val duration = data.getLongExtra("duration", 0L)
        val size = Utils.getVideoSize2(this, path)
        val message = ChatMessage.create(
            viewModel.getAddress(),
            address,
            channelType,
            Biz.MsgType.Video,
            MessageContent.video(null, path, intArrayOf(size[0], size[1]), duration.toInt() / 1000)
        )
        viewModel.sendMessage(url, message)
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        handleExitTransaction(resultCode, data)
    }

    private fun handleExitTransaction(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val exitPos = data.getIntExtra(MediaGalleryActivity.EXIT_IMG_POS, -1)
            setExitSharedElement(exitPos)
        }
    }

    private fun setExitSharedElement(position: Int) {
        if (position == -1) {
            return
        }
        // 因为列表是倒置的所以要转换下position
        val imageIndex = mAdapter.data.filterMedia().asReversed()[position]
        val view = mAdapter.getViewByPosition(mAdapter.data.indexOf(imageIndex), R.id.iv_image)
        ActivityCompat.setExitSharedElementCallback(this, object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String?>,
                sharedElements: MutableMap<String?, View?>
            ) {
                names.clear()
                sharedElements.clear()
                if (view != null) {
                    names.add(ViewCompat.getTransitionName(view))
                    sharedElements[ViewCompat.getTransitionName(view)] = view
                    setExitSharedElementCallback(object : SharedElementCallback() {})
                } else {
                    // 如果图片不在当前界面内，则用默认转场动画
                    names.add(ViewCompat.getTransitionName(binding.emptyShare))
                    sharedElements[ViewCompat.getTransitionName(binding.emptyShare)] =
                        binding.emptyShare
                    setExitSharedElementCallback(object : SharedElementCallback() {})
                }
            }
        })
    }

    private val messagePopup by lazy { ChatMessagePopup(this, address, viewModel) }

    private val messageEventListener = object : ChatBaseItem.ChatMessageClickListener {
        override fun onTouchChatMainView(view: View, event: MotionEvent?): Boolean {
            event?.apply {
                touchMsgX = x.toInt()
                touchMsgY = y.toInt()
            }
            return false
        }

        override fun onResendClick(view: View, message: ChatMessage) {
            if (url.isEmpty()) {
                toast(R.string.chat_tips_servers_empty)
                return
            }
            viewModel.resendMessage(url, message)
        }

        override fun onChatLayoutClick(view: View, message: ChatMessage, item: ChatBaseItem) {
            when (Biz.MsgType.forNumber(message.msgType)) {
                Biz.MsgType.Text -> {
                    ARouter.getInstance().build(MainModule.LARGE_TEXT)
                        .withSerializable("message", message)
                        .withBoolean("isRef", false)
                        .withTransition(R.anim.biz_zoom_up_in, R.anim.biz_zoom_up_out)
                        .navigation(this@ChatActivity)
                }
                Biz.MsgType.Audio -> {
                    if (message.localExists()) {
                        playOrPauseAudio(message)
                    } else {
                        lifecycleScope.launch {
                            DownloadManager2.downloadToApp(message).observe(instance) {
                                it.dataOrNull()?.apply { playOrPauseAudio(message) }
                            }
                        }
                    }
                }
                Biz.MsgType.Image -> {
                    val list = mAdapter.data.filterMedia().reversed()
                    val intent = Intent(instance, MediaGalleryActivity::class.java).apply {
                        putExtra("messages", ArrayList(list))
                        putExtra("index", list.indexOf(message))
                        putExtra("showGallery", true)
                    }
                    startActivity(
                        intent, ActivityOptionsCompat.makeSceneTransitionAnimation(
                            instance, view, "shareImage"
                        ).toBundle()
                    )
                }
                Biz.MsgType.Video -> {
                    if (message.localExists()) {
                        val list = mAdapter.data.filterMedia().reversed()
                        val intent = Intent(instance, MediaGalleryActivity::class.java).apply {
                            putExtra("messages", ArrayList(list))
                            putExtra("index", list.indexOf(message))
                            putExtra("showGallery", true)
                        }
                        startActivity(
                            intent, ActivityOptionsCompat.makeSceneTransitionAnimation(
                                instance, view, "shareImage"
                            ).toBundle()
                        )
                    } else {
                        lifecycleScope.launch {
                            DownloadManager2.downloadToApp(message).observe(instance) {
                                message.progress = it.progress()
                                mAdapter.notifyItemChanged(
                                    mAdapter.data.indexOf(message),
                                    bundleOf(ChatMessage.MSG_PROGRESS to message.progress)
                                )
                            }
                        }
                    }
                }
                Biz.MsgType.File -> {
                    ARouter.getInstance().build(MainModule.FILE_DETAIL)
                        .withSerializable(
                            "file", SimpleFileBean(
                                message.msg.fileName,
                                message.msg.size,
                                message.msg.md5,
                                message.msg.localUrl
                            )
                        )
                        .withSerializable("message", message)
                        .navigation()
                }
                Biz.MsgType.Forward -> {
                    ARouter.getInstance().build(MainModule.FORWARD_LIST)
                        .withSerializable("message", message)
                        .navigation()
                }
                Biz.MsgType.RTCCall -> {
                    ARouter.getInstance().build(RtcModule.VIDEO_CALL)
                        .withString("targetId", address)
                        .withInt("callType", RTCCalling.TYPE_CALL)
                        .withInt("rtcType", message.msg.rtcType)
                        .navigation()
                }
                Biz.MsgType.Transfer -> {
                    if (message.msg.txStatus == SimpleTx.PENDING) {
                        toast("查询中...")
                        return
                    }
                    ARouter.getInstance().build(WalletModule.TRANSACTION_DETAIL)
                        .withString("txId", message.msg.txHash)
                        .withString("chain", message.msg.chain)
                        .withString("symbol", message.msg.tokenSymbol)
                        .withString("platform", message.msg.platform)
                        .withSerializable("message", message)
                        .navigation()
                }
                Biz.MsgType.RedPacket -> {
                    message.msg.packetId?.also { packetId ->
                        if (channelType == ChatConst.GROUP_CHANNEL) {
                            openRedPacket(packetId, message)
                        } else {
                            if (message.isSendType) {
                                ARouter.getInstance().build(RedPacketModule.RED_PACKET_DETAIL)
                                    .withString("packetId", packetId)
                                    .withBoolean("viewOnly", true)
                                    .navigation()
                            } else {
                                openRedPacket(packetId, message)
                            }
                        }
                    } ?: toast("红包id为空")
                }
                Biz.MsgType.ContactCard -> {
                    when (message.msg.contactType) {
                        1 -> {
                            ARouter.getInstance().build(MainModule.CONTACT_INFO)
                                .withString("address", message.msg.contactId)
                                .navigation()
                        }
                        2 -> {
                            ARouter.getInstance().build(MainModule.JOIN_GROUP_INFO)
                                .withLong("groupId", message.msg.contactId?.toLong() ?: 0L)
                                .withString("server", message.msg.contactServer)
                                .withString("inviterId", message.msg.contactInviter)
                                .navigation()
                        }
                        3 -> oaService?.applyJoinTeamPage(
                            instance, mapOf(
                                "id" to message.msg.contactId,
                                "server" to message.msg.contactServer,
                                "inviterId" to message.msg.contactInviter
                            )
                        )
                    }
                }
                else -> {

                }
            }
        }

        override fun onChatLayoutDoubleTap(view: View, message: ChatMessage) {
            if (!message.isSendType && !message.hasFocused && message.logId != 0L) {
                if (message.msgType == Biz.MsgType.Text_VALUE ||
                    message.msgType == Biz.MsgType.Audio_VALUE ||
                    message.msgType == Biz.MsgType.Image_VALUE ||
                    message.msgType == Biz.MsgType.Video_VALUE ||
                    message.msgType == Biz.MsgType.File_VALUE ||
                    message.msgType == Biz.MsgType.Forward_VALUE ||
                    message.msgType == Biz.MsgType.ContactCard_VALUE
                ) {
                    // 关注消息
                    viewModel.requestFocus(message)
                }
            }
        }

        override fun onChatLayoutLongClick(view: View, message: ChatMessage, item: ChatBaseItem): Boolean {
            messagePopup.setMessage(message, groupInfo?.person?.role)
                .setPosition(touchMsgX, touchMsgY)
                .setOnDismissListener {
                    item.onActionUp(view, message)
                }
                .show(view)
            return true
        }

        override fun onAvatarClick(view: View, message: ChatMessage) {
            if (channelType == ChatConst.PRIVATE_CHANNEL) {
                ARouter.getInstance().build(MainModule.CONTACT_INFO)
                    .withString("address", message.from)
                    .withBoolean("disableSend", true)
                    .navigation()
            } else {
                ARouter.getInstance().build(MainModule.CONTACT_INFO)
                    .withString("address", message.from)
                    .withLong("groupId", groupInfo?.gid ?: 0L)
                    .withInt("role", groupInfo.groupRole)
                    .navigation()
            }
        }

        override fun onAvatarLongClick(view: View, message: ChatMessage): Boolean {
            if (!message.isSendType) {
                atManager?.let {
                    it.insertAitMember(
                        message.from,
                        message.sender?.getRawName() ?: message.from,
                        binding.inputView.getEditText().selectionStart
                    )
                    KeyboardUtils.showKeyboard(binding.inputView.getEditText())
                }
            }
            return true
        }

        override fun onNotificationClick(view: View, message: ChatMessage, type: Int, index: Int) {
            when (type) {
                Msg.NotificationType.RevokeMessage_VALUE -> {
                    binding.inputView.setText(message.msg.reedit)
                    binding.inputView.post {
                        KeyboardUtils.showKeyboard(binding.inputView.getEditText())
                    }
                }
            }
        }

        override fun onReferenceClick(view: View, message: ChatMessage) {
            val ref = message.reference?.refMsg ?: return
            when (Biz.MsgType.forNumber(ref.msgType)) {
                Biz.MsgType.Text -> {
                    ARouter.getInstance().build(MainModule.LARGE_TEXT)
                        .withSerializable("message", message)
                        .withBoolean("isRef", true)
                        .withTransition(R.anim.biz_zoom_up_in, R.anim.biz_zoom_up_out)
                        .navigation(this@ChatActivity)
                }
                Biz.MsgType.Image -> {
                    val intent = Intent(instance, MediaGalleryActivity::class.java).apply {
                        putExtra("messages", arrayListOf(ref))
                        putExtra("showOptions", false)
                    }
                    view.transitionName = "shareImage"
                    startActivity(
                        intent, ActivityOptionsCompat.makeSceneTransitionAnimation(
                            instance, view, "shareImage"
                        ).toBundle()
                    )
                }
                Biz.MsgType.Video -> {
                    if (ref.localExists()) {
                        val intent = Intent(instance, MediaGalleryActivity::class.java).apply {
                            putExtra("messages", arrayListOf(ref))
                            putExtra("showOptions", false)
                        }
                        view.transitionName = "shareImage"
                        startActivity(
                            intent, ActivityOptionsCompat.makeSceneTransitionAnimation(
                                instance, view, "shareImage"
                            ).toBundle()
                        )
                    } else {
                        lifecycleScope.launch {
                            DownloadManager2.downloadToApp(ref).observe(instance) {
                                if (it is Result.Success) {
                                    val intent = Intent(instance, MediaGalleryActivity::class.java).apply {
                                        putExtra("messages", arrayListOf(ref))
                                        putExtra("showOptions", false)
                                    }
                                    view.transitionName = "shareImage"
                                    startActivity(
                                        intent, ActivityOptionsCompat.makeSceneTransitionAnimation(
                                            instance, view, "shareImage"
                                        ).toBundle()
                                    )
                                } else if (it is Result.Error) {
                                    toast(
                                        getString(
                                            R.string.chat_tips_download_fail,
                                            it.error().message
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                Biz.MsgType.File -> {
                    if (ref.localExists()) {
                        ARouter.getInstance().build(MainModule.FILE_DETAIL)
                            .withSerializable(
                                "file", SimpleFileBean(
                                    ref.msg.fileName,
                                    ref.msg.size,
                                    ref.msg.md5,
                                    ref.msg.localUrl
                                )
                            )
                            .withSerializable("message", ref)
                            .navigation()
                        return
                    }
                    lifecycleScope.launch {
                        DownloadManager2.downloadToApp(ref).observe(instance) {
                            if (it is Result.Success) {
                                ARouter.getInstance().build(MainModule.FILE_DETAIL)
                                    .withSerializable(
                                        "file", SimpleFileBean(
                                            ref.msg.fileName,
                                            ref.msg.size,
                                            ref.msg.md5,
                                            ref.msg.localUrl
                                        )
                                    )
                                    .withSerializable("message", ref)
                                    .navigation()
                            } else if (it is Result.Error) {
                                toast(
                                    getString(
                                        R.string.chat_tips_download_fail,
                                        it.error().message
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        override fun onFocusClick(view: View, message: ChatMessage) {
            if (message.isGroup) {
                focusDialog.setLogId(address!!, message.logId).show(supportFragmentManager, "FOCUS_USERS")
            }
        }

        override fun onMessageSelectedChanged(message: ChatMessage, max: Boolean) {
            if (max) toast(getString(R.string.chat_tips_reach_max_msg_select_num, ChatBaseItem.MAX_SELECT_NUM))
        }
    }

    private var currentAudioMessage: ChatMessage? = null

    private fun playOrPauseAudio(message: ChatMessage) {
        if (MediaManager.isPlaying()) {
            if (MediaManager.currentAudio() == message.msg.localUrl) {
                MediaManager.stop()
                return
            } else {
                MediaManager.stop()
            }
        }
        currentAudioMessage = message
        (mAdapter.getViewByPosition(
            mAdapter.data.indexOf(message),
            R.id.icon_voice
        ) as? IconView)?.play()
        viewModel.readAudioMessage(message)
        MediaManager.play(this, message.msg.localUrl, viewModel.preference.AUDIO_CHANNEL) {
            val current = mAdapter.data.indexOf(message)
            for (index in current - 1 downTo 0) {
                val msg = mAdapter.data[index]
                if (msg.msgType == Biz.MsgType.Audio_VALUE
                    && message.isSendType
                    && !msg.msg.isRead
                ) {
                    playOrPauseAudio(msg)
                    return@play
                }
            }
        }
        mAdapter.notifyItemChanged(
            mAdapter.data.indexOf(message),
            bundleOf(ChatMessage.MSG_READ_STATE to 1)
        )
    }

    private fun openRedPacket(packetId: String, message: ChatMessage) {
        when (message.msg.state) {
            1 -> {
                val dialog by route<DialogFragment>(
                    RedPacketModule.PACKET_DIALOG,
                    bundleOf("packetId" to packetId, "message" to message)
                )
                dialog?.show(supportFragmentManager, "OPEN_RED_PACKET")
            }
            else -> {
                ARouter.getInstance().build(RedPacketModule.RED_PACKET_DETAIL)
                    .withString("packetId", packetId)
                    .navigation()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            keyboardHeightProvider.start()
        }
    }

    override fun onKeyboardHeightChanged(height: Int, orientation: Int) {
        AppPreference.keyboardHeight = height
        binding.inputView.setKeyboardHeight(height)
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            // android11以下需要手动关闭输入面板，android11则在RootViewDeferringInsetsCallback的closePanel回调中关闭
            binding.inputView.hideBottomLayout(true)
        }
        layoutManager.scrollToPosition(0)
    }

    override fun onBackPressedSupport() {
        if (binding.inputView.hideBottomLayout()) {
            return
        }
        if (mAdapter.isSelectable()) {
            viewModel.requestSelect(false)
            return
        }
        super.onBackPressedSupport()
    }
}