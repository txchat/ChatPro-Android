//package com.fzm.chat.conversation
//
//import android.Manifest
//import android.content.ClipData
//import android.content.Intent
//import android.net.Uri
//import android.view.MotionEvent
//import android.view.View
//import android.view.animation.DecelerateInterpolator
//import androidx.core.app.ActivityCompat
//import androidx.core.app.SharedElementCallback
//import androidx.core.view.ViewCompat
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import androidx.transition.*
//import com.alibaba.android.arouter.facade.annotation.Autowired
//import com.alibaba.android.arouter.launcher.ARouter
//import com.fzm.chat.R
//import com.fzm.chat.biz.base.BizActivity
//import com.fzm.chat.biz.utils.onExplain
//import com.fzm.chat.conversation.adapter.MessageAdapter
//import com.fzm.chat.conversation.adapter.msg.ChatBaseItem
//import com.fzm.chat.core.data.ChatConfig
//import com.fzm.chat.core.data.ChatConst
//import com.fzm.chat.core.data.model.ChatMessage
//import com.fzm.chat.core.data.model.hasSent
//import com.fzm.chat.core.data.po.FriendUser
//import com.fzm.chat.core.data.po.MessageContent
//import com.fzm.chat.core.utils.Utils
//import com.fzm.chat.databinding.ActivityBubbleChatBinding
//import com.fzm.chat.router.main.MainModule
//import com.fzm.chat.ui.ImageGalleryActivity
//import com.fzm.chat.utils.GlideEngine
//import com.fzm.chat.widget.ChatInputView
//import com.luck.picture.lib.PictureSelector
//import com.luck.picture.lib.config.PictureMimeType
//import com.luck.picture.lib.entity.LocalMedia
//import com.luck.picture.lib.listener.OnResultCallbackListener
//import com.permissionx.guolindev.PermissionX
//import com.zjy.architecture.ext.clipboardManager
//import com.zjy.architecture.ext.notificationManager
//import com.zjy.architecture.ext.toast
//import com.zjy.architecture.util.KeyboardUtils
//import dtalk.biz.Biz
//import org.koin.androidx.viewmodel.ext.android.viewModel
//
///**
// * @author zhengjy
// * @since 2020/12/23
// * Description:
// */
//@Deprecated("暂时不用，如需使用，则需要同步ChatActivity的最新修改")
//class BubbleChatActivity : BizActivity(), ChatInputView.IChatInputView {
//
//    @JvmField
//    @Autowired
//    var address: String? = null
//
//    @JvmField
//    @Autowired
//    var name: String? = null
//
//    /**
//     * 聊天对象所在的服务器地址
//     */
//    var url: String = ""
//
//    private lateinit var layoutManager: LinearLayoutManager
//    private lateinit var mAdapter: MessageAdapter
//    private val message = mutableListOf<ChatMessage>()
//
//    private var oldScrollState: Int = 0
//
//    private val viewModel by viewModel<ChatViewModel>()
//
//    private val binding by init { ActivityBubbleChatBinding.inflate(layoutInflater) }
//
//    override val root: View
//        get() = binding.root
//
//    override fun setStatusBar() {
//
//    }
//
//    override fun initView() {
//        ARouter.getInstance().inject(this)
//        binding.ctbTitle.setTitle(name)
//        binding.inputView.also {
//            it.bind(this, this)
//        }
//        layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, true)
//        layoutManager.stackFromEnd = true
//        binding.rvMessage.layoutManager = layoutManager
//        mAdapter = MessageAdapter(message)
//        mAdapter.loadMoreModule.setOnLoadMoreListener {
//            val size = mAdapter.data.size
//            if (size > 0) {
//                viewModel.getMessageHistory(address ?: "", mAdapter.data[size - 1].datetime)
//            }
//        }
//        mAdapter.setMessageEventListener(messageEventListener)
//        binding.rvMessage.adapter = mAdapter
//        binding.rvMessage.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                if (oldScrollState == RecyclerView.SCROLL_STATE_IDLE && newState == RecyclerView.SCROLL_STATE_DRAGGING) {
//                    KeyboardUtils.hideKeyboard(root)
//                    binding.inputView.hideBottomLayout()
//                }
//                oldScrollState = newState
//            }
//        })
//    }
//
//    override fun initData() {
//        viewModel.latestMessage.observe(this) {
//            mAdapter.addData(it.toMutableList())
//            if (it.size < ChatConfig.PAGE_SIZE) {
//                mAdapter.loadMoreModule.isEnableLoadMore = false
//            } else {
//                mAdapter.loadMoreModule.isEnableLoadMore = true
//                mAdapter.loadMoreModule.loadMoreComplete()
//            }
//        }
//        viewModel.scrollBottom.observe(this) {
//            layoutManager.scrollToPosition(0)
//        }
//        viewModel.fetchUserInfoInfo(address).observe(this) {
//            setupUserInfo(it)
//        }
//        viewModel.setOnNewMessageListener { message ->
//            val log = mAdapter.data.find {
//                (it.logId == message.logId && message.hasSent)  // 列表中已有相同logId的已发送消息
//                    || it.msgId == message.msgId            // 列表中已有相同msgId的未发送消息
//            }
//            if (log == null) {
//                val index = findNewMessageIndex(message)
//                if (index != -1) {
//                    mAdapter.addData(index, message)
//                    layoutManager.scrollToPosition(0)
//                }
//            } else {
//                val index = mAdapter.data.indexOf(log)
//                mAdapter.setData(index, message)
//                layoutManager.scrollToPosition(0)
//            }
//        }
//        viewModel.setOnMessageStateChangedListener {
//            mAdapter.data.forEachIndexed { index, msg ->
//                if (it.msgId == msg.msgId) {
//                    mAdapter.data[index].state = it.state
//                    mAdapter.notifyItemChanged(index)
//                    return@forEachIndexed
//                }
//            }
//        }
//        viewModel.getMessageHistory(address ?: "")
//    }
//
//    /**
//     * 确定新消息插入的位置
//     */
//    private fun findNewMessageIndex(message: ChatMessage): Int {
//        if (mAdapter.data.isEmpty() || message.logId == 0L) {
//            return 0
//        }
//        if (message.logId > mAdapter.data.first().logId) {
//            return 0
//        }
//        if (message.logId < mAdapter.data.last().logId) {
//            return -1
//        }
//        mAdapter.data.forEachIndexed { index, item ->
//            if (item.logId == 0L) {
//                if (message.datetime > item.datetime) {
//                    return index
//                }
//            } else {
//                if (message.logId > item.logId) {
//                    return index
//                }
//            }
//        }
//        return -1
//    }
//
//    override fun setEvent() {
//
//    }
//
//    private fun setupUserInfo(user: FriendUser) {
//        this.name = user.getDisplayName()
//        if (user.servers.isNotEmpty()) {
//            this.url = user.servers[0].address
//        }
//        binding.ctbTitle.setTitle(name)
//    }
//
//    override fun onLayoutAnimation(appear: Boolean) {
//        val transition = TransitionSet().apply {
//            ordering = TransitionSet.ORDERING_TOGETHER
//            addTransition(Fade(Fade.OUT))
//            addTransition(ChangeBounds())
//            addTransition(Fade(Fade.IN))
//            this.duration = 200
//            interpolator = DecelerateInterpolator()
//        }
//        TransitionManager.beginDelayedTransition(binding.root, transition)
//    }
//
//    override fun onSend(view: View, content: String) {
//        if (url.isEmpty()) {
//            toast(R.string.chat_tips_servers_empty)
//            return
//        }
//        val message = ChatMessage.create(
//            viewModel.getAddress(),
//            address,
//            Biz.MsgType.Text,
//            MessageContent.text(content)
//        )
//        viewModel.sendMessage(url, message)
//    }
//
//    override fun onSelectMedia(view: View) {
//        PermissionX.init(this)
//            .permissions(Manifest.permission.READ_EXTERNAL_STORAGE)
//            .onExplain(this)
//            .request { allGranted, _, _ ->
//                if (allGranted) {
//                    PictureSelector.create(this)
//                        .openGallery(PictureMimeType.ofImage())
//                        .isCompress(true)
//                        .isCamera(false)
//                        .theme(R.style.chat_picture_style)
//                        .imageEngine(GlideEngine.createGlideEngine())
//                        .forResult(object : OnResultCallbackListener<LocalMedia> {
//                            override fun onResult(result: MutableList<LocalMedia>) {
//                                onPhotoSelected(result)
//                            }
//
//                            override fun onCancel() {
//
//                            }
//                        })
//                } else {
//                    this.toast(com.fzm.chat.biz.R.string.biz_permission_not_granted)
//                }
//            }
//    }
//
//    override fun onCapture(view: View) {
//
//    }
//
//    override fun onSnapChat(view: View) {
//
//    }
//
//    override fun onSelectFile(view: View) {
//
//    }
//
//    override fun scrollMessageToBottom() {
//        layoutManager.scrollToPosition(0)
//    }
//
//    override fun switchInputType(type: Int, confirm: (Boolean) -> Unit) {
//        if (type == ChatInputView.INPUT_KEYBOARD) {
//            confirm.invoke(true)
//        } else if (type == ChatInputView.INPUT_VOICE) {
//            PermissionX.init(this)
//                .permissions(Manifest.permission.RECORD_AUDIO)
//                .onExplain(this)
//                .request { allGranted, _, _ ->
//                    if (allGranted) {
//                        confirm.invoke(true)
//                    } else {
//                        confirm.invoke(false)
//                        toast(R.string.biz_permission_not_granted)
//                    }
//                }
//        }
//    }
//
//    override fun onAudioRecorderFinished(seconds: Float, filePath: String?) {
//        toast(filePath ?: "")
//    }
//
//    override fun onResume() {
//        super.onResume()
//        address?.also {
//            viewModel.clearUnread(it, ChatConst.PRIVATE_CHANNEL)
//        }
//        notificationManager?.cancel(address.hashCode())
//        ChatConfig.CURRENT_TARGET = address
//    }
//
//    override fun onPause() {
//        super.onPause()
//        address?.also {
//            viewModel.clearUnread(it, ChatConst.PRIVATE_CHANNEL)
//        }
//        ChatConfig.CURRENT_TARGET = null
//    }
//
//    private fun onPhotoSelected(list: List<LocalMedia>) {
//        list.forEach {
//            if (it.isCompressed) {
//                val message = ChatMessage.create(
//                    viewModel.getAddress(),
//                    address,
//                    Biz.MsgType.Image,
//                    MessageContent.image(null, it.compressPath, Utils.getImageSize(it.compressPath))
//                )
//                viewModel.uploadImage(url, it.compressPath, message)
//            } else {
//                val uri = try {
//                    Uri.parse(it.path)
//                } catch (e: Exception) {
//                    toast(R.string.chat_tips_img_not_exist)
//                    return
//                }
//                val message = ChatMessage.create(
//                    viewModel.getAddress(),
//                    address,
//                    Biz.MsgType.Image,
//                    MessageContent.image(null, it.compressPath, Utils.getImageSize(it.compressPath))
//                )
//                viewModel.uploadImage(url, uri, message)
//            }
//        }
//    }
//
//    override fun onActivityReenter(resultCode: Int, data: Intent?) {
//        super.onActivityReenter(resultCode, data)
//        handleExitTransaction(resultCode, data)
//    }
//
//    private fun handleExitTransaction(resultCode: Int, data: Intent?) {
//        if (resultCode == RESULT_OK && data != null) {
//            val exitPos = data.getIntExtra(ImageGalleryActivity.EXIT_IMG_POS, -1)
//            setExitSharedElement(exitPos)
//        }
//    }
//
//    private fun setExitSharedElement(position: Int) {
//        if (position == -1) {
//            return
//        }
//        // 因为列表是倒置的所以要转换下position
//        val imageIndex = mAdapter.data.filter { it.msgType == Biz.MsgType.Image_VALUE }.asReversed()[position]
//        val view = mAdapter.getViewByPosition(mAdapter.data.indexOf(imageIndex), R.id.iv_image)
//        ActivityCompat.setExitSharedElementCallback(this, object : SharedElementCallback() {
//            override fun onMapSharedElements(names: MutableList<String?>, sharedElements: MutableMap<String?, View?>) {
//                names.clear()
//                sharedElements.clear()
//                if (view != null) {
//                    names.add(ViewCompat.getTransitionName(view))
//                    sharedElements[ViewCompat.getTransitionName(view)] = view
//                    setExitSharedElementCallback(object : SharedElementCallback() {})
//                } else {
//                    // 如果图片不在当前界面内，则用默认转场动画
//                    names.add(ViewCompat.getTransitionName(binding.emptyShare))
//                    sharedElements[ViewCompat.getTransitionName(binding.emptyShare)] = binding.emptyShare
//                    setExitSharedElementCallback(object : SharedElementCallback() {})
//                }
//            }
//        })
//    }
//
//    private val messageEventListener = object : ChatBaseItem.ChatMessageClickListener {
//        override fun onTouchChatMainView(view: View, event: MotionEvent?): Boolean {
//            return false
//        }
//
//        override fun onResendClick(view: View, message: ChatMessage) {
//            viewModel.resendMessage(url, message)
//        }
//
//        override fun onChatLayoutClick(view: View, message: ChatMessage, item: ChatBaseItem) {
//            toast("点击${message.msg.content}")
//        }
//
//        override fun onChatLayoutDoubleTap(view: View, message: ChatMessage) {
//            toast("双击${message.msg.content}")
//        }
//
//        override fun onChatLayoutLongClick(view: View, message: ChatMessage): Boolean {
//            val clipData = ClipData.newPlainText("message", message.msg.content)
//            clipboardManager?.setPrimaryClip(clipData)
//            toast("已经复制消息内容")
//            return false
//        }
//
//        override fun onAvatarClick(view: View, message: ChatMessage) {
//            ARouter.getInstance().build(MainModule.CONTACT_INFO)
//                .withString("address", message.from)
//                .withBoolean("disableSend", true)
//                .navigation()
//        }
//
//        override fun onAvatarLongClick(view: View, id: String, username: String) {
//            toast("长按头像")
//        }
//
//        override fun onMessageSelectedChanged(message: ChatMessage) {
//
//        }
//    }
//
//    override fun onBackPressedSupport() {
//        if (binding.inputView.hideBottomLayout()) {
//            return
//        }
//        super.onBackPressedSupport()
//    }
//}