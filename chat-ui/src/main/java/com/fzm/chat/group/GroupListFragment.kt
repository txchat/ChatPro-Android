package com.fzm.chat.group

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.arch.connection.utils.urlKey
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.bus.BusEvent
import com.fzm.chat.biz.bus.LiveDataBus
import com.fzm.chat.biz.bus.event.ChangeTabEvent
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.comparator.PinyinComparator
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.databinding.FragmentGroupListBinding
import com.fzm.chat.databinding.LayoutEmptyGroupListBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.ext.load
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/05/07
 * Description:
 */
class GroupListFragment : BizFragment() {

    companion object {
        fun create(url: String): GroupListFragment {
            return GroupListFragment().apply {
                arguments = bundleOf("url" to url)
            }
        }
    }

    private var mUrl: String = ""

    private val viewModel by viewModel<GroupViewModel>()
    private lateinit var mAdapter: BaseQuickAdapter<GroupInfo, BaseViewHolder>
    private val groups = mutableListOf<GroupInfo>()
    private val comparator = PinyinComparator()

    private val binding by init<FragmentGroupListBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mUrl = arguments?.getString("url", "") ?: ""
        binding.refresh.setEnableLoadMore(false)
        val manager = LinearLayoutManager(requireContext())
        binding.rvGroupList.layoutManager = manager
        mAdapter =
            object : BaseQuickAdapter<GroupInfo, BaseViewHolder>(R.layout.item_chat_group, groups) {
                override fun convert(holder: BaseViewHolder, item: GroupInfo) {
                    holder.setText(R.id.tag, item.getFirstLetter())
                    val position = groups.indexOf(item)
                    if (position > 0) {
                        val last = groups[position - 1].getFirstLetter()
                        val current = item.getFirstLetter()
                        if (last == current) {
                            holder.setGone(R.id.tag, true)
                        } else {
                            holder.setGone(R.id.tag, false)
                            holder.setText(R.id.tag, item.getFirstLetter())
                        }
                    } else {
                        holder.setGone(R.id.tag, false)
                        holder.setText(R.id.tag, item.getFirstLetter())
                    }
                    when (item.groupType) {
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
                    holder.getView<ChatAvatarView>(R.id.iv_avatar)
                        .load(item.getDisplayImage(), R.mipmap.default_avatar_room)
                    holder.setText(R.id.tv_name, item.getDisplayName())
                }
            }
        mAdapter.setOnItemClickListener { _, _, position ->
            ARouter.getInstance().build(MainModule.CHAT)
                .withString("address", groups[position].getId())
                .withInt("channelType", groups[position].getType())
                .navigation()
            LiveDataBus.of(BusEvent::class.java).changeTab().postDelay(ChangeTabEvent(0, 1), 300)
        }
        binding.rvGroupList.adapter = mAdapter
        val bind = LayoutEmptyGroupListBinding.inflate(layoutInflater)
        bind.tvCreateGroup.setOnClickListener {
            ARouter.getInstance().build(MainModule.CREATE_INVITE_GROUP).navigation()
        }
        mAdapter.setEmptyView(bind.root)

        binding.sideBar.setTextView(binding.dialog)
        //设置右侧SideBar触摸监听
        binding.sideBar.setOnTouchingLetterChangedListener { s ->
            //该字母首次出现的位置
            val position = getPositionForSection(s[0].toInt())
            if (position != -1) {
                manager.scrollToPositionWithOffset(position, 0)
            }
        }
        refresh()
    }

    private fun getPositionForSection(section: Int): Int {
        for (i in groups.indices) {
            val sortStr = groups[i].getFirstLetter()
            val firstChar = sortStr[0]
            if (firstChar.toInt() == section) {
                return i
            }
        }
        return -1
    }

    fun refresh() {
        viewModel.getGroupList(mUrl)
    }

    override fun initData() {
        ChatDatabaseProvider.provide()
            .groupDao()
            .getGroupListByServer(mUrl.urlKey(), Contact.RELATION)
            .observe(viewLifecycleOwner) {
                groups.clear()
                groups.addAll(it)
                mAdapter.setList(groups.sortedWith(comparator))
            }
    }

    override fun setEvent() {
        binding.refresh.setOnRefreshListener {
            refresh()
            binding.refresh.finishRefresh(true)
        }
    }
}