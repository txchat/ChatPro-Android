package com.fzm.chat.conversation

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.arch.connection.socket.ConnectionManager
import com.fzm.arch.connection.utils.urlKey
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.bus.BusEvent
import com.fzm.chat.biz.bus.LiveDataBus
import com.fzm.chat.biz.bus.event.ChangeTabEvent
import com.fzm.chat.biz.utils.defaultAvatar
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.getGroupType
import com.fzm.chat.core.data.comparator.RecentMsgComparator
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.*
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.databinding.FragmentPersonMessageBinding
import com.fzm.chat.databinding.LayoutEmptyFriendsMessageBinding
import com.fzm.chat.databinding.LayoutEmptyGroupMessageBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.utils.StringUtils
import com.fzm.chat.widget.SessionOperationPopup
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.load
import dtalk.biz.Biz
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2020/12/09
 * Description:
 */
class SessionListFragment : BizFragment() {

    companion object {
        fun create(channel: Int) : SessionListFragment {
            return SessionListFragment().apply {
                arguments = bundleOf("channel" to channel)
            }
        }
    }

    @JvmField
    @Autowired
    var channel: Int = ChatConst.PRIVATE_CHANNEL

    private val viewModel by viewModel<SessionViewModel>()
    private val connect by inject<ConnectionManager>()
    private val manager by inject<ContactManager>()
    private lateinit var mAdapter: BaseQuickAdapter<RecentContactMsg, BaseViewHolder>
    private val comparator = RecentMsgComparator()

    private val listPopup by lazy { SessionOperationPopup(requireContext(), viewModel) }

    private val stickState by lazy { ColorStateList.valueOf(resources.getColor(R.color.biz_color_primary_dark)) }
    private val notStickState by lazy { ColorStateList.valueOf(resources.getColor(R.color.biz_color_primary)) }
    private val pressedState by lazy { ColorStateList.valueOf(resources.getColor(R.color.biz_color_bg_grey)) }

    /**
     * 屏蔽会话点击事件
     */
    private var blockClick = false

    /**
     * 是否正在打开会话
     */
    private var openingSession = false

    private var touchX = 0
    private var touchY = 0
    private val binding by init<FragmentPersonMessageBinding>()

    private var offsetY = 0
    private var firstVisiblePos = 0
    private val layoutManager by lazy { LinearLayoutManager(requireContext()) }

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        ARouter.getInstance().inject(this)
        binding.rvMessage.layoutManager = layoutManager
        binding.rvMessage.setHasFixedSize(true)
        binding.rvMessage.itemAnimator = null
        binding.rvMessage.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
                offsetY = (recyclerView.findViewHolderForLayoutPosition(firstVisiblePos) as? BaseViewHolder)?.itemView?.top?:0
            }
        })
        mAdapter = object : BaseQuickAdapter<RecentContactMsg, BaseViewHolder>(
            R.layout.item_person_message,
            mutableListOf()
        ) {
            @SuppressLint("ClickableViewAccessibility")
            override fun convert(holder: BaseViewHolder, item: RecentContactMsg) {
                setupDisturbView(holder, item)
                if (item.isStickTop()) {
                    ViewCompat.setBackgroundTintList(holder.itemView, stickState)
                } else {
                    ViewCompat.setBackgroundTintList(holder.itemView, notStickState)
                }
                holder.getView<ChatAvatarView>(R.id.iv_avatar)
                    .load(item.getDisplayImage(), defaultAvatar(item.getType()))
                val tvName = holder.getView<TextView>(R.id.tv_name)
                tvName.text = item.getDisplayName()
                if (item.getTime() == 0L) {
                    holder.setGone(R.id.tv_time, true)
                } else {
                    holder.setGone(R.id.tv_time, false)
                    holder.setText(R.id.tv_time, StringUtils.timeFormat(requireContext(), item.getTime()))
                }
                holder.setGone(R.id.iv_disturb, !item.isNoDisturb())
                ViewCompat.setBackgroundTintMode(holder.itemView, PorterDuff.Mode.DARKEN)

                if (channel == ChatConst.GROUP_CHANNEL) {
                    val servers = item.getServerList()
                    if (servers.isNotEmpty()) {
                        val socket = connect.getChatSocket(servers[0].address.urlKey())
                        val drawable:Drawable?
                        val color: Int
                        when {
                            socket == null -> {
                                drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_server_not_exist, null)
                                color = R.color.biz_text_grey_light
                                holder.getView<ChatAvatarView>(R.id.iv_avatar).blackAndWhite(true)
                            }
                            socket.isAlive -> {
                                drawable = null
                                color = R.color.biz_text_grey_dark
                                holder.getView<ChatAvatarView>(R.id.iv_avatar).blackAndWhite(false)
                            }
                            else -> {
                                drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_server_disconnect, null)
                                color = R.color.biz_text_grey_dark
                                holder.getView<ChatAvatarView>(R.id.iv_avatar).blackAndWhite(false)
                            }
                        }
                        drawable?.apply { setBounds(0, 0, minimumWidth, minimumHeight) }
                        tvName.setCompoundDrawables(null, null, drawable, null)
                        tvName.setTextColor(ResourcesCompat.getColor(resources, color, null))
                    } else {
                        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_server_not_exist, null)
                        tvName.setCompoundDrawables(null, null, drawable, null)
                        tvName.setTextColor(ResourcesCompat.getColor(resources, R.color.biz_text_grey_light, null))
                        holder.getView<ChatAvatarView>(R.id.iv_avatar).blackAndWhite(true)
                    }
                    when (item.getGroupType()) {
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
                }
                val llName = holder.getView<View>(R.id.ll_name)
                llName.post {
                    llName.layoutParams = (llName.layoutParams as RelativeLayout.LayoutParams).also {
                        it.marginEnd = holder.getView<View>(R.id.tv_time).width + 20.dp
                    }
                }

                holder.itemView.setOnTouchListener { _, event ->
                    touchX = event.x.toInt()
                    touchY = event.y.toInt()
                    return@setOnTouchListener false
                }
            }

            override fun convert(holder: BaseViewHolder, item: RecentContactMsg, payloads: List<Any>) {
                if (payloads.isEmpty()) {
                    convert(holder, item)
                    return
                }
                val bundle = payloads[0] as Bundle
                bundle.getInt(RecentContactMsg.TAG, -1).also {
                    if (it == -1) return@also
                    when (item.getGroupType()) {
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
                }
                bundle.getInt(RecentContactMsg.STATUS, -1).also {
                    if (it == -1) return@also
                    if (channel == ChatConst.GROUP_CHANNEL) {
                        val tvName = holder.getView<TextView>(R.id.tv_name)
                        val servers = item.getServerList()
                        if (servers.isNotEmpty()) {
                            val socket = connect.getChatSocket(servers[0].address.urlKey())
                            val drawable:Drawable?
                            val color: Int
                            when {
                                socket == null -> {
                                    drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_server_not_exist, null)
                                    color = R.color.biz_text_grey_light
                                    holder.getView<ChatAvatarView>(R.id.iv_avatar).blackAndWhite(true)
                                }
                                socket.isAlive -> {
                                    drawable = null
                                    color = R.color.biz_text_grey_dark
                                    holder.getView<ChatAvatarView>(R.id.iv_avatar).blackAndWhite(false)
                                }
                                else -> {
                                    drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_server_disconnect, null)
                                    color = R.color.biz_text_grey_dark
                                    holder.getView<ChatAvatarView>(R.id.iv_avatar).blackAndWhite(false)
                                }
                            }
                            drawable?.apply { setBounds(0, 0, minimumWidth, minimumHeight) }
                            tvName.setCompoundDrawables(null, null, drawable, null)
                            tvName.setTextColor(ResourcesCompat.getColor(resources, color, null))
                        } else {
                            val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_server_not_exist, null)
                            tvName.setCompoundDrawables(null, null, drawable, null)
                            tvName.setTextColor(ResourcesCompat.getColor(resources, R.color.biz_text_grey_light, null))
                            holder.getView<ChatAvatarView>(R.id.iv_avatar).blackAndWhite(true)
                        }
                    }
                }
                bundle.getString(RecentContactMsg.AVATAR)?.also {
                    holder.getView<ChatAvatarView>(R.id.iv_avatar).load(it, defaultAvatar(item.getType()))
                }
                bundle.getString(RecentContactMsg.NAME)?.also {
                    holder.getView<TextView>(R.id.tv_name).text = it
                    val llName = holder.getView<View>(R.id.ll_name)
                    llName.post {
                        llName.layoutParams = (llName.layoutParams as RelativeLayout.LayoutParams).also { lp ->
                            lp.marginEnd = holder.getView<View>(R.id.tv_time).width + 20.dp
                        }
                    }
                }
                bundle.getInt(RecentContactMsg.AT, -1).also {
                    if (it != -1) {
                        if (item.hasDraft && item.beAtMsg()) {
                            setupDisturbView(holder, item)
                        } else {
                            setSessionTips(holder, item)
                        }
                    }
                }
                bundle.getString(RecentContactMsg.DRAFT)?.also {
                    if (it.isNotEmpty()) {
                        setSessionTips(holder, item)
                    }
                }
                bundle.getInt(RecentContactMsg.UNREAD, -1).also {
                    if (it != -1) {
                        setupDisturbView(holder, item)
                    }
                }
                bundle.getString(RecentContactMsg.CONTENT)?.also {
                    setupDisturbView(holder, item)
                }
                bundle.getLong(RecentContactMsg.TIME, -1).also {
                    if (it != -1L) {
                        if (it == 0L) {
                            holder.setGone(R.id.tv_time, true)
                        } else {
                            holder.setGone(R.id.tv_time, false)
                            holder.setText(R.id.tv_time, StringUtils.timeFormat(requireContext(), it))
                        }
                        val llName = holder.getView<View>(R.id.ll_name)
                        llName.post {
                            llName.layoutParams = (llName.layoutParams as RelativeLayout.LayoutParams).also { lp ->
                                lp.marginEnd = holder.getView<View>(R.id.tv_time).width + 20.dp
                            }
                        }
                    }
                }
                bundle.getInt(RecentContactMsg.NO_DISTURB, -1).also {
                    setupDisturbView(holder, item)
                    if (it != -1) holder.setGone(R.id.iv_disturb, it != 1)
                }
                bundle.getInt(RecentContactMsg.STICK_TOP, -1).also {
                    if (it != -1) {
                        if (it == 1) {
                            ViewCompat.setBackgroundTintList(holder.itemView, stickState)
                        } else {
                            ViewCompat.setBackgroundTintList(holder.itemView, notStickState)
                        }
                    }
                }
            }
        }
        mAdapter.setOnItemClickListener { _, v, position ->
            if (blockClick) return@setOnItemClickListener
            if (openingSession) return@setOnItemClickListener
            openingSession = true
            val container = v.findViewById<View>(R.id.ll_container)
            ARouter.getInstance().build(MainModule.CHAT)
                .withInt("channelType", mAdapter.data[position].getType())
                .withString("address", mAdapter.data[position].getId())
                .withOptionsCompat(
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        requireActivity(),
                        container,
                        container.transitionName
                    )
                )
                .navigation(requireContext())
            ViewCompat.setBackgroundTintList(v, pressedState)
        }
        mAdapter.setOnItemLongClickListener { _, v, position ->
            if (blockClick) return@setOnItemLongClickListener true
            val address = mAdapter.data[position].getId()
            val flag = mAdapter.data[position].getFlags()
            val name = mAdapter.data[position].getDisplayName()
            val channelType = mAdapter.data[position].getType()
            listPopup.setSessionInfo(address, name, flag, channelType)
                .setPosition(touchX, touchY)
                .setOnDismissListener {
                    if (mAdapter.data[position].isStickTop()) {
                        ViewCompat.setBackgroundTintList(v, stickState)
                    } else {
                        ViewCompat.setBackgroundTintList(v, notStickState)
                    }
                }
                .show(v)
            ViewCompat.setBackgroundTintList(v, pressedState)
            return@setOnItemLongClickListener true
        }
        mAdapter.setDiffCallback(ItemCallback())
        binding.rvMessage.adapter = mAdapter

        if (channel == ChatConst.PRIVATE_CHANNEL) {
            viewModel.personSession.observe(viewLifecycleOwner) {
                if (!mAdapter.isUseEmpty) {
                    mAdapter.isUseEmpty = true
                }
                val newList: MutableList<RecentContactMsg> = it.sortedWith(comparator).toMutableList()
                val f = firstVisiblePos
                val o = offsetY
                mAdapter.setNewDiffList(newList) {
                    layoutManager.scrollToPositionWithOffset(f, o)
                }
                onSessionsChangedListener?.invoke(newList)
            }
            val bind = LayoutEmptyFriendsMessageBinding.inflate(layoutInflater)
            bind.tvStartChat.setOnClickListener {
                LiveDataBus.of(BusEvent::class.java).changeTab().postValue(ChangeTabEvent(1, 0))
            }
            mAdapter.setEmptyView(bind.root)
        } else {
            connect.observeSocketState(viewLifecycleOwner) { (key, status) ->
                mAdapter.data.forEachIndexed { index, session ->
                    if (session.getServerList().firstOrNull()?.address?.urlKey() == key) {
                        mAdapter.notifyItemChanged(index, bundleOf(RecentContactMsg.STATUS to status))
                    }
                }
            }
            viewModel.groupSession.observe(viewLifecycleOwner) {
                if (!mAdapter.isUseEmpty) {
                    mAdapter.isUseEmpty = true
                }
                val newList: MutableList<RecentContactMsg> = it.sortedWith(comparator).toMutableList()
                val f = firstVisiblePos
                val o = offsetY
                mAdapter.setNewDiffList(newList) {
                    layoutManager.scrollToPositionWithOffset(f, o)
                }
                onSessionsChangedListener?.invoke(newList)
            }
            val bind = LayoutEmptyGroupMessageBinding.inflate(layoutInflater)
            bind.tvCreateGroup.setOnClickListener {
                ARouter.getInstance().build(MainModule.CREATE_INVITE_GROUP).navigation()
            }
            mAdapter.setEmptyView(bind.root)
        }
        mAdapter.isUseEmpty = false
    }

    private fun setSessionTips(holder: BaseViewHolder, item: RecentContactMsg): Boolean {
        if (item.beAtMsg()) {
            holder.setGone(R.id.tv_ait, false)
            holder.setText(R.id.tv_ait, "[有人@我]")
            // 显示@不影响消息显示
            return true
        } else if (item.hasDraft) {
            holder.setGone(R.id.tv_ait, false)
            holder.setText(R.id.tv_ait, "[草稿]")
            holder.setText(R.id.tv_message, item.getDraftText())
            // 显示草稿时不显示消息内容
            return false
        } else {
            holder.setGone(R.id.tv_ait, true)
            // 没有草稿和@信息
            return true
        }
    }

    private fun setupDisturbView(holder: BaseViewHolder, item: RecentContactMsg) {
        val showSessionContent = setSessionTips(holder, item)
        val tvMessage = holder.getView<TextView>(R.id.tv_message)
        tvMessage.tag = item.getId()
        if (item.isNoDisturb()) {
            holder.setGone(R.id.tv_msg_num, true)
            if (item.unreadNum() == 0) {
                holder.setGone(R.id.msg_dot, true)
                if (showSessionContent) {
                    if (item.getType() == ChatConst.PRIVATE_CHANNEL || item.getRecentLog().msgType == Biz.MsgType.Notification_VALUE) {
                        tvMessage.text = item.getContent()
                    } else {
                        if (viewModel.current.value?.address == item.getRecentLog().from) {
                            tvMessage.text = getString(R.string.chat_tips_sender_me, item.getContent())
                        } else {
                            lifecycleScope.launch {
                                val sender = manager.getGroupUserInfoFast(item.getId(), item.getRecentLog().from)
                                if (tvMessage.tag == item.getId()) {
                                    tvMessage.text = "${sender.getDisplayName().cutName()}:${item.getContent()}"
                                }
                            }
                        }
                    }
                }
            } else {
                holder.setGone(R.id.msg_dot, false)
                if (showSessionContent) {
                    if (item.getType() == ChatConst.PRIVATE_CHANNEL || item.getRecentLog().msgType == Biz.MsgType.Notification_VALUE) {
                        tvMessage.text = getString(R.string.chat_tips_unread_num, item.unreadNum(), item.getContent())
                    } else {
                        if (viewModel.current.value?.address == item.getRecentLog().from) {
                            tvMessage.text = getString(
                                R.string.chat_tips_unread_num,
                                item.unreadNum(),
                                getString(R.string.chat_tips_sender_me, item.getContent())
                            )
                        } else {
                            lifecycleScope.launch {
                                val sender = manager.getGroupUserInfoFast(item.getId(), item.getRecentLog().from)
                                if (tvMessage.tag == item.getId()) {
                                    tvMessage.text = getString(
                                        R.string.chat_tips_unread_num,
                                        item.unreadNum(),
                                        "${sender.getDisplayName().cutName()}:${item.getContent()}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            holder.setGone(R.id.msg_dot, true)
            if (item.unreadNum() == 0) {
                holder.setGone(R.id.tv_msg_num, true)
            } else {
                holder.setGone(R.id.tv_msg_num, false)
                val num = if (item.unreadNum() > 99) "99+" else item.unreadNum().toString()
                holder.setText(R.id.tv_msg_num, num)
                holder.setBackgroundResource(R.id.tv_msg_num, R.drawable.shape_red_dot)
            }
            if (showSessionContent) {
                if (item.getType() == ChatConst.PRIVATE_CHANNEL || item.getRecentLog().msgType == Biz.MsgType.Notification_VALUE) {
                    holder.setText(R.id.tv_message, item.getContent())
                } else {
                    if (viewModel.current.value?.address == item.getRecentLog().from) {
                        tvMessage.text = getString(R.string.chat_tips_sender_me, item.getContent())
                    } else {
                        lifecycleScope.launch {
                            val sender = manager.getGroupUserInfoFast(item.getId(), item.getRecentLog().from)
                            if (tvMessage.tag == item.getId()) {
                                tvMessage.text = "${sender.getDisplayName().cutName()}:${item.getContent()}"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ImageView.blackAndWhite(enable: Boolean) {
        colorFilter = if (enable) {
            ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        } else {
            null
        }
    }

    private fun String.cutName(): String {
        if (length > 6) {
            return "${substring(0, 5)}…"
        }
        return this
    }

    override fun onResume() {
        super.onResume()
        openingSession = false
        if (!mAdapter.hasEmptyView()) {
            for (i in 0 until mAdapter.itemCount) {
                val v = layoutManager.findViewByPosition(i)
                if (v != null) {
                    if (mAdapter.data[i].isStickTop()) {
                        if (ViewCompat.getBackgroundTintList(v) != stickState) {
                            ViewCompat.setBackgroundTintList(v, stickState)
                        }
                    } else {
                        if (ViewCompat.getBackgroundTintList(v) != notStickState) {
                            ViewCompat.setBackgroundTintList(v, notStickState)
                        }
                    }
                }
            }
        }
    }

    override fun initData() {

    }

    override fun setEvent() {

    }

    fun setBlockClick(block: Boolean) {
        this.blockClick = block
    }

    private var onSessionsChangedListener: ((List<RecentContactMsg>) -> Unit)? = null

    fun setSessionsChangedListener(listener: (List<RecentContactMsg>) -> Unit) {
        this.onSessionsChangedListener = listener
    }

    inner class ItemCallback : DiffUtil.ItemCallback<RecentContactMsg>() {
        override fun areItemsTheSame(old: RecentContactMsg, new: RecentContactMsg): Boolean {
            return old.getId() == new.getId()
        }

        override fun areContentsTheSame(old: RecentContactMsg, new: RecentContactMsg): Boolean {
            return old.hashCode() == new.hashCode()
        }

        override fun getChangePayload(oldItem: RecentContactMsg, newItem: RecentContactMsg): Any? {
            val bundle = Bundle()
            if (oldItem.getDisplayImage() != newItem.getDisplayImage()) {
                bundle.putString(RecentContactMsg.AVATAR, newItem.getDisplayImage())
            }
            if (oldItem.getDisplayName() != newItem.getDisplayName()) {
                bundle.putString(RecentContactMsg.NAME, newItem.getDisplayName())
            }
            if (oldItem.beAtMsg() != newItem.beAtMsg()) {
                bundle.putInt(RecentContactMsg.AT, if (newItem.beAtMsg()) 1 else 0)
            }
            if (oldItem.getDraftTime() != newItem.getDraftTime()) {
                bundle.putString(RecentContactMsg.DRAFT, newItem.getDraftText())
            }
            if (oldItem.unreadNum() != newItem.unreadNum()) {
                bundle.putInt(RecentContactMsg.UNREAD, newItem.unreadNum())
            }
            if (oldItem.getContent() != newItem.getContent()) {
                bundle.putString(RecentContactMsg.CONTENT, newItem.getContent())
            }
            if (oldItem.getTime() != newItem.getTime()) {
                bundle.putLong(RecentContactMsg.TIME, newItem.getTime())
            }
            if (oldItem.isNoDisturb() != newItem.isNoDisturb()) {
                bundle.putInt(RecentContactMsg.NO_DISTURB, if (newItem.isNoDisturb()) 1 else 0)
            }
            if (oldItem.isStickTop() != newItem.isStickTop()) {
                bundle.putInt(RecentContactMsg.STICK_TOP, if (newItem.isStickTop()) 1 else 0)
            }
            if (oldItem.getGroupType() != newItem.getGroupType()) {
                bundle.putInt(RecentContactMsg.TAG, newItem.getGroupType())
            }
            if (bundle.isEmpty) {
                return null
            }
            return bundle
        }
    }

    private fun BaseQuickAdapter<RecentContactMsg, BaseViewHolder>.setNewDiffList(list: MutableList<RecentContactMsg>?, commitCallback: Runnable? = null) {
        if (hasEmptyView()) {
            // If the current view is an empty view, set the new data directly without diff
            setNewInstance(list)
            return
        }
        getDiffer().submitList(list, commitCallback)
    }
}