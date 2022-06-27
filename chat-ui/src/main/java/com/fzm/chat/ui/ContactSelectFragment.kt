package com.fzm.chat.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.arch.connection.socket.ConnectionManager
import com.fzm.arch.connection.utils.urlKey
import com.fzm.chat.R
import com.fzm.chat.bean.model.ForwardContact
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.utils.defaultAvatar
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.biz.widget.HighlightTextView
import com.fzm.chat.contract.CompanyMemberSelector
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.getGroupType
import com.fzm.chat.core.data.model.RecentContactMsg
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.databinding.FragmentContactSelectBinding
import com.fzm.chat.router.oa.OAModule
import com.fzm.chat.router.oa.OAService
import com.fzm.chat.router.oa.hasCompany
import com.fzm.chat.router.route
import com.fzm.chat.vm.ContactSelectViewModel
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.visible
import org.koin.android.ext.android.inject

/**
 * @author zhengjy
 * @since 2021/08/10
 * Description:联系人选择基础页面
 */
abstract class ContactSelectFragment : BizFragment() {

    companion object {

        private const val STATUS = "STATUS"
    }

    private lateinit var teamMemberSelect: ActivityResultLauncher<String>

    protected abstract val viewModel: ContactSelectViewModel
    protected val manager by inject<ConnectionManager>()
    private val oaService by route<OAService>(OAModule.SERVICE)

    protected lateinit var mAdapter: BaseQuickAdapter<ForwardContact, BaseViewHolder>
    protected val contactList = mutableListOf<ForwardContact>()

    protected val binding by init<FragmentContactSelectBinding>()

    open val showOA: Boolean = true

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        ARouter.getInstance().inject(this)
        binding.rvSessionContact.layoutManager = LinearLayoutManager(requireContext())
        mAdapter = object : BaseQuickAdapter<ForwardContact, BaseViewHolder>(
            R.layout.item_forward_contact,
            contactList
        ) {
            override fun convert(holder: BaseViewHolder, item: ForwardContact) {
                val position = contactList.indexOf(item)
                if (position == 0) {
                    holder.setGone(R.id.tag, false)
                    holder.setText(R.id.tag, item.getListTag())
                } else {
                    val last = contactList[position - 1].channel
                    val current = item.channel
                    if (last == current) {
                        holder.setGone(R.id.tag, true)
                    } else {
                        holder.setGone(R.id.tag, false)
                        holder.setText(R.id.tag, item.getListTag())
                    }
                }
                val tvName = holder.getView<TextView>(R.id.tv_name)
                if (item.contact.getType() == ChatConst.GROUP_CHANNEL) {
                    val servers = item.contact.getServerList()
                    if (servers.isNotEmpty()) {
                        val socket = manager.getChatSocket(servers[0].address.urlKey())
                        val drawable = when {
                            socket == null -> ResourcesCompat.getDrawable(resources, R.drawable.ic_server_not_exist, null)
                            socket.isAlive -> null
                            else -> ResourcesCompat.getDrawable(resources, R.drawable.ic_server_disconnect, null)
                        }
                        drawable?.apply { setBounds(0, 0, minimumWidth, minimumHeight) }
                        tvName.setCompoundDrawables(null, null, drawable, null)
                    } else {
                        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_server_not_exist, null)
                        tvName.setCompoundDrawables(null, null, drawable, null)
                    }
                    when (item.contact.getGroupType()) {
                        GroupInfo.TYPE_NORMAL -> holder.setGone(R.id.tv_group_type, true)
                        GroupInfo.TYPE_TEAM -> {
                            holder.setText(R.id.tv_group_type, "全员")
                            holder.setGone(R.id.tv_group_type, false)
                        }
                        GroupInfo.TYPE_DEPART -> {
                            holder.setText(R.id.tv_group_type, "部门")
                            holder.setGone(R.id.tv_group_type, false)
                        }
                    }
                } else {
                    tvName.setCompoundDrawables(null, null, null, null)
                    holder.setGone(R.id.tv_group_type, true)
                }
                holder.getView<ChatAvatarView>(R.id.iv_avatar)
                    .load(item.contact.getDisplayImage(), defaultAvatar(item.contact.getType()))
                when (item.channel) {
                    ForwardContact.RECENT_SESSION -> {
                        holder.getView<HighlightTextView>(R.id.tv_name)
                            .highlightSearchText(item.contact.getDisplayName(), viewModel.keywords)
                        holder.setGone(R.id.tv_extra, true)
                    }
                    ForwardContact.FRIEND -> {
                        val friend = item.contact as FriendUser
                        if (friend.remark.isEmpty()) {
                            holder.getView<HighlightTextView>(R.id.tv_name)
                                .highlightSearchText(item.contact.getDisplayName(), viewModel.keywords)
                            holder.setGone(R.id.tv_extra, true)
                        } else {
                            holder.getView<HighlightTextView>(R.id.tv_name)
                                .highlightSearchText(friend.remark, viewModel.keywords)
                            holder.getView<HighlightTextView>(R.id.tv_extra).highlightSearchText(
                                getString(
                                    R.string.chat_tips_nickname_placeholder,
                                    friend.nickname.ifEmpty { friend.address }), viewModel.keywords
                            )
                            holder.setText(R.id.tv_name, friend.remark)
                        }
                    }
                    ForwardContact.GROUP -> {
                        holder.getView<HighlightTextView>(R.id.tv_name)
                            .highlightSearchText(item.contact.getDisplayName(), viewModel.keywords)
                        holder.setGone(R.id.tv_extra, true)
                    }
                }
            }

            override fun convert(holder: BaseViewHolder, item: ForwardContact, payloads: List<Any>) {
                val bundle = payloads[0] as Bundle
                bundle.getInt(RecentContactMsg.TAG, -1).also {
                    if (it == -1) return@also
                    if (item.contact.getType() == ChatConst.GROUP_CHANNEL) {
                        when (item.contact.getGroupType()) {
                            GroupInfo.TYPE_NORMAL -> holder.setGone(R.id.tv_group_type, true)
                            GroupInfo.TYPE_TEAM -> {
                                holder.setText(R.id.tv_group_type, "全员")
                                holder.setGone(R.id.tv_group_type, false)
                            }
                            GroupInfo.TYPE_DEPART -> {
                                holder.setText(R.id.tv_group_type, "部门")
                                holder.setGone(R.id.tv_group_type, false)
                            }
                        }
                    } else {
                        holder.setGone(R.id.tv_group_type, true)
                    }
                }
                bundle.getInt(RecentContactMsg.STATUS, -1).also {
                    if (it == -1) return@also
                    val tvName = holder.getView<TextView>(R.id.tv_name)
                    if (item.contact.getType() == ChatConst.GROUP_CHANNEL) {
                        val servers = item.contact.getServerList()
                        if (servers.isNotEmpty()) {
                            val socket = manager.getChatSocket(servers[0].address.urlKey())
                            val drawable = when {
                                socket == null -> ResourcesCompat.getDrawable(resources, R.drawable.ic_server_not_exist, null)
                                socket.isAlive -> null
                                else -> ResourcesCompat.getDrawable(resources, R.drawable.ic_server_disconnect, null)
                            }
                            drawable?.apply { setBounds(0, 0, minimumWidth, minimumHeight) }
                            tvName.setCompoundDrawables(null, null, drawable, null)
                        } else {
                            val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_server_not_exist, null)
                            tvName.setCompoundDrawables(null, null, drawable, null)
                        }
                    } else {
                        tvName.setCompoundDrawables(null, null, null, null)
                    }
                }
            }
        }
        binding.rvSessionContact.adapter = mAdapter

        viewModel.companyUser.observe(this) { user ->
            if (user.hasCompany && showOA) {
                binding.ctlSelectOa.root.visible()
            } else {
                binding.ctlSelectOa.root.gone()
            }
        }
    }

    override fun initData() {
        viewModel.loading.observe(viewLifecycleOwner) { setupLoading(it) }
        viewModel.contact.observe(viewLifecycleOwner) {
            mAdapter.setList(it)
        }
        manager.observeSocketState(viewLifecycleOwner) { (key, status) ->
            mAdapter.data.forEachIndexed { index, item ->
                if (item.contact.getType() == ChatConst.GROUP_CHANNEL
                    && item.contact.getServerList().firstOrNull()?.address?.urlKey() == key
                ) {
                    mAdapter.notifyItemChanged(index, bundleOf(STATUS to status))
                }
            }
        }
    }

    open fun onSelectTeamMembers(users: List<String>) {

    }

    fun openTeamMemberSelectPage(map: Map<String, String>) {
        oaService?.let { service ->
            teamMemberSelect.launch(service.selectCompanyUserUrl(map))
        }
    }

    override fun setEvent() {
        teamMemberSelect = registerForActivityResult(CompanyMemberSelector()) { selectUsers ->
            if (!selectUsers.isNullOrEmpty()) {
                onSelectTeamMembers(selectUsers)
            }
        }
    }
}