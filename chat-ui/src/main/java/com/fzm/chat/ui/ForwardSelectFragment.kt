package com.fzm.chat.ui

import android.content.Intent
import android.net.Uri
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.arch.connection.utils.urlKey
import com.fzm.chat.R
import com.fzm.chat.biz.ui.DeepLinkHelper
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.bean.PreSendParams
import com.fzm.chat.core.data.bean.Server
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.*
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.core.data.po.isInGroup
import com.fzm.chat.core.data.po.isMuteAll
import com.fzm.chat.core.logic.MessageSender
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.oa.OAModule
import com.fzm.chat.router.oa.OAService
import com.fzm.chat.router.route
import com.fzm.chat.vm.ContactSelectViewModel
import com.fzm.chat.widget.ForwardDialog
import com.zjy.architecture.ext.toast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * @author zhengjy
 * @since 2021/06/18
 * Description:转发选择联系人页面
 */
class ForwardSelectFragment : ContactSelectFragment() {

    companion object {
        const val ACTION_FORWARD_MESSAGE = "forwardMessage"

        fun create(preSend: PreSendParams?, messages: ArrayList<ChatMessage>?): ForwardSelectFragment {
            return ForwardSelectFragment().apply {
                arguments = bundleOf(
                    "preSend" to preSend,
                    "messages" to messages
                )
            }
        }
    }

    @JvmField
    @Autowired
    var preSend: PreSendParams? = null

    @JvmField
    @Autowired
    var messages: ArrayList<ChatMessage>? = null

    override val viewModel: ForwardSelectViewModel
        get() = requireActivity().getViewModel()

    override fun initData() {
        super.initData()
        viewModel.checkSendResult.observe(viewLifecycleOwner) { contact ->
            if (contact == null) {
                toast(R.string.chat_tips_can_not_forward_group)
                return@observe
            }
            if (contact.getId() == viewModel.getAddress()) {
                toast(R.string.chat_tips_can_not_forward_self)
                return@observe
            }
            if (contact.getServerList().isEmpty()) {
                toast(R.string.chat_tips_can_not_forward_no_server)
                return@observe
            }
            val messages = if (preSend != null) {
                viewModel.generateMessage(preSend!!, contact)
            } else if (!messages.isNullOrEmpty()) {
                viewModel.generateMessage(messages!!, contact)
            } else {
                emptyList()
            }
            if (messages.isEmpty()) {
                toast(R.string.chat_tips_forward_msg_empty)
                return@observe
            }
            ForwardDialog(requireContext(), messages, contact) {
                viewModel.forwardMessage(it, contact)
            }.show()
        }
        viewModel.forwardResult.observe(viewLifecycleOwner) {
            ARouter.getInstance().build(AppModule.MAIN)
                .withParcelable(
                    "route",
                    Uri.parse("${DeepLinkHelper.APP_LINK}?type=chatNotification&address=${it.getId()}&channelType=${it.getType()}")
                )
                .withFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .navigation(requireActivity())
        }
    }

    override fun setEvent() {
        super.setEvent()
        mAdapter.setOnItemClickListener { _, _, position ->
            val contact = contactList[position].contact
            if (contact.getType() == ChatConst.GROUP_CHANNEL) {
                val servers = contact.getServerList()
                if (servers.isEmpty() || manager.getChatSocket(servers[0].address.urlKey()) == null) {
                    toast(R.string.chat_tips_can_not_forward_group_no_server)
                    return@setOnItemClickListener
                }
            }
            viewModel.checkSendPermission(contact)
        }

        binding.ctlSelectOa.root.setOnClickListener {
            openTeamMemberSelectPage(mapOf("action" to ACTION_FORWARD_MESSAGE))
        }
    }

    override fun onSelectTeamMembers(users: List<String>) {
        viewModel.forwardMessage(users[0])
    }

    class ForwardSelectViewModel(
        private val delegate: LoginDelegate,
        private val contactManager: ContactManager,
        private val messageSender: MessageSender
    ) : ContactSelectViewModel(delegate) {

        private val oaService by route<OAService>(OAModule.SERVICE)

        private val _forwardResult by lazy { MutableLiveData<Contact>() }
        val forwardResult: LiveData<Contact>
            get() = _forwardResult

        private val _checkSendResult by lazy { MutableLiveData<Contact>() }
        val checkSendResult: LiveData<Contact>
            get() = _checkSendResult

        /**
         * 检查该联系人是否允许发送消息
         */
        fun checkSendPermission(contact: Contact) {
            launch(CoroutineExceptionHandler { _, _ -> dismiss() }) {
                loading(true)
                val canSend = if (contact.getType() == ChatConst.PRIVATE_CHANNEL) {
                    when (contact) {
                        is FriendUser -> {
                            val user = oaService?.getCompanyUser(contact.getId())
                            if (user != null) {
                                contact.servers.add(
                                    0,
                                    Server("company", user.company!!.name, user.company!!.imServer)
                                )
                            }
                        }
                        is RecentFriendMsg -> {
                            val user = oaService?.getCompanyUser(contact.getId())
                            if (user != null) {
                                contact.servers?.add(
                                    0,
                                    Server("company", user.company!!.name, user.company!!.imServer)
                                )
                            }
                        }
                    }
                    // 不加好友也能转发
                    true
                } else {
                    if (contact is GroupInfo) {
                        checkGroup(contact)
                    } else {
                        val group = contactManager.getGroupInfo(contact.getId())
                        checkGroup(group)
                    }
                }
                dismiss()
                _checkSendResult.value = if (canSend) contact else null
            }
        }

        fun forwardMessage(address: String) {
            launch {
                loading(true)
                val contact = contactManager.getUserInfo(address, save = true)
                checkSendPermission(contact)
                dismiss()
            }
        }

        private suspend fun checkGroup(group: GroupInfo) : Boolean {
            if (!group.isInGroup) {
                // 不在群中不能发消息
                return false
            }
            val groupUser = contactManager.getGroupUserInfo(
                group.getId(),
                delegate.getAddress() ?: ""
            ) as GroupUser
            if (group.isMuteAll && groupUser.role == GroupUser.LEVEL_USER) {
                // 群被全体禁言了，且不是群主或管理员不能发消息
                return false
            }
            if (groupUser.muteTime > System.currentTimeMillis()) {
                // 被单独禁言了不能发消息
                return false
            }
            return true
        }

        /**
         * 合并转发
         */
        fun generateMessage(preSend: PreSendParams, contact: Contact): List<ChatMessage> {
            return listOf(
                ChatMessage.create(
                    delegate.getAddress(),
                    contact.getId(),
                    contact.getType(),
                    preSend.msgType,
                    preSend.msg,
                    preSend.source
                ))
        }

        /**
         * 逐条转发
         */
        fun generateMessage(messages: List<ChatMessage>, contact: Contact): List<ChatMessage> {
            val result = mutableListOf<ChatMessage>()
            messages.filter { it.canForward }.forEach {
                result.add(
                    ChatMessage.create(
                        delegate.getAddress(),
                        contact.getId(),
                        contact.getType(),
                        it.msgType,
                        it.msg.apply {
                            atList = null
                            if (ChatConfig.FILE_ENCRYPT) {
                                // 开启文件加密时，mediaUrl置空，需要重新加密上传文件
                                mediaUrl = null
                            }
                        },
                        it.source
                    ))
            }
            return result
        }

        fun forwardMessage(messages: List<ChatMessage>, contact: Contact) {
            val servers = contact.getServerList()
            if (servers.isEmpty()) {
                return
            }
            loading(true)
            messages.forEach {
                messageSender.send(servers[0].address, it)
            }
            _forwardResult.value = contact
            dismiss()
        }
    }
}