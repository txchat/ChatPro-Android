package com.fzm.chat.group

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.biz.widget.HighlightTextView
import com.fzm.chat.core.data.comparator.PinyinComparator
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.databinding.FragmentGroupUserListBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.visible
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/05/20
 * Description:
 */
class GroupUserListFragment : BizFragment() {

    companion object {

        /**
         * 展示所有群成员
         */
        const val ACTION_LIST = "ACTION_LIST"

        /**
         * 单选群成员
         */
        const val ACTION_SINGLE = "ACTION_SINGLE"

        /**
         * 多选群成员
         */
        const val ACTION_MULTI = "ACTION_MULTI"

        /**
         * 创建群列表页面
         *
         * @param gid           群id
         * @param action        列表操作模式
         * @param level         群成员显示等级
         * @param role          当前等级
         * @param excludeSelf   是否排除自己
         */
        fun create(
            gid: Long?,
            action: String = ACTION_LIST,
            level: Int = GroupUser.LEVEL_OWNER,
            role: Int = GroupUser.LEVEL_USER,
            excludeSelf: Boolean = false
        ): GroupUserListFragment {
            return GroupUserListFragment().apply {
                arguments = bundleOf(
                    "groupId" to gid,
                    "action" to action,
                    "level" to level,
                    "role" to role,
                    "excludeSelf" to excludeSelf
                )
            }
        }
    }

    private val comparator = PinyinComparator()
    private val viewModel by viewModel<GroupViewModel>()
    private val users = mutableListOf<GroupUser>()
    private val selectedUsers = mutableListOf<GroupUser>()

    private var gid: Long = 0L
    private var action: String = ""
    private var level: Int = 0
    private var role: Int = 0
    private var excludeSelf: Boolean = false

    private var searchKey: String? = null

    private lateinit var mAdapter: BaseQuickAdapter<GroupUser, BaseViewHolder>
    private lateinit var manager: LinearLayoutManager

    private val binding by init<FragmentGroupUserListBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        gid = arguments?.getLong("groupId", 0L)?:0L
        action = arguments?.getString("action") ?: ACTION_LIST
        level = arguments?.getInt("level", GroupUser.LEVEL_OWNER) ?: GroupUser.LEVEL_OWNER
        role = arguments?.getInt("role", GroupUser.LEVEL_USER) ?: GroupUser.LEVEL_USER
        excludeSelf = arguments?.getBoolean("excludeSelf") ?: false

        manager = LinearLayoutManager(requireContext())
        binding.rvGroupUsers.layoutManager = manager
        mAdapter = object :
            BaseQuickAdapter<GroupUser, BaseViewHolder>(R.layout.item_chat_group_users, users) {
            override fun convert(holder: BaseViewHolder, item: GroupUser) {
                holder.setText(R.id.tag, item.getFirstLetter())
                val position = users.indexOf(item)
                if (item.role > GroupUser.LEVEL_USER) {
                    holder.setGone(R.id.tv_role, false)
                    if (item.role == GroupUser.LEVEL_ADMIN) {
                        holder.setTextColor(R.id.tv_role, resources.getColor(R.color.biz_orange_tips))
                        holder.setBackgroundResource(R.id.tv_role, R.drawable.shape_orange_r4)
                        holder.setText(R.id.tv_role, R.string.chat_tips_member_type_admin)
                    } else if (item.role == GroupUser.LEVEL_OWNER) {
                        holder.setTextColor(R.id.tv_role, resources.getColor(R.color.biz_color_accent))
                        holder.setBackgroundResource(R.id.tv_role, R.drawable.shape_blue_r4)
                        holder.setText(R.id.tv_role, R.string.chat_tips_member_type_owner)
                    }
                    if (position > 0) {
                        val last = data[position - 1].role
                        if (last > GroupUser.LEVEL_USER) {
                            holder.setGone(R.id.tag, true)
                        } else {
                            holder.setGone(R.id.tag, false)
                            holder.setText(R.id.tag, R.string.chat_tips_member_type)
                        }
                    } else {
                        holder.setGone(R.id.tag, false)
                        holder.setText(R.id.tag, R.string.chat_tips_member_type)
                    }
                } else {
                    holder.setGone(R.id.tv_role, true)
                    if (position > 0) {
                        val last = users[position - 1]
                        if (last.role > GroupUser.LEVEL_USER) {
                            holder.setGone(R.id.tag, false)
                            holder.setText(R.id.tag, item.getFirstLetter())
                        } else {
                            val lastTag = last.getFirstLetter()
                            val current = item.getFirstLetter()
                            if (lastTag == current) {
                                holder.setGone(R.id.tag, true)
                            } else {
                                holder.setGone(R.id.tag, false)
                                holder.setText(R.id.tag, item.getFirstLetter())
                            }
                        }
                    } else {
                        holder.setGone(R.id.tag, false)
                        holder.setText(R.id.tag, item.getFirstLetter())
                    }
                }
                val cbSelect = holder.getView<CheckBox>(R.id.cb_select)
                when (action) {
                    ACTION_LIST -> {
                        cbSelect.gone()
                        holder.getView<View>(R.id.ll_item).setOnClickListener {
                            ARouter.getInstance().build(MainModule.CONTACT_INFO)
                                .withString("address", item.getId())
                                .withLong("groupId", gid)
                                .withInt("role", role)
                                .navigation()
                        }
                    }
                    ACTION_SINGLE -> {
                        cbSelect.gone()
                        holder.getView<View>(R.id.ll_item).setOnClickListener {
                            onSingleSelectedListener?.invoke(item)
                        }
                    }
                    ACTION_MULTI -> {
                        cbSelect.visible()
                        cbSelect.setOnClickListener {
                            if (selectedUsers.contains(item)) {
                                selectedUsers.remove(item)
                            } else {
                                selectedUsers.add(item)
                            }
                        }
                        holder.getView<View>(R.id.ll_item).setOnClickListener {
                            cbSelect.performClick()
                        }
                    }
                }

                holder.getView<ChatAvatarView>(R.id.iv_avatar)
                    .load(item.getDisplayImage(), R.mipmap.default_avatar_round)
                holder.getView<HighlightTextView>(R.id.tv_name).highlightSearchText(item.getDisplayName(), searchKey)
            }
        }
        binding.rvGroupUsers.adapter = mAdapter
        binding.sideBar.setTextView(binding.dialog)
        //设置右侧SideBar触摸监听
        binding.sideBar.setOnTouchingLetterChangedListener { s ->
            //该字母首次出现的位置
            val position = getPositionForSection(s[0].toInt())
            if (position != -1) {
                manager.scrollToPositionWithOffset(position, 0)
            }
        }
    }

    override fun initData() {
        getGroupUserList(null, true)
    }

    private var groupUserList: LiveData<List<GroupUser>> = MutableLiveData()

    fun getGroupUserList(keywords: String?, refresh: Boolean = false) {
        searchKey = keywords
        groupUserList.removeObservers(viewLifecycleOwner)
        groupUserList = viewModel.getGroupUserList(gid, keywords, refresh)
        groupUserList.observe(viewLifecycleOwner) {
            mAdapter.setList(
                it.asSequence()
                    .filter { user -> user.role <= level }
                    .filter { user ->
                        !excludeSelf || user.address != viewModel.getAddress()
                    }
                    .sortedWith(comparator)
                    .toList()
            )
        }
    }

    override fun setEvent() {
        
    }

    private var onSingleSelectedListener: ((GroupUser) -> Unit)? = null

    fun setOnSingleSelectedListener(listener: ((GroupUser) -> Unit)?) {
        this.onSingleSelectedListener = listener
    }

    fun getSelectedUsers(): List<GroupUser> = selectedUsers

    private fun getPositionForSection(section: Int): Int {
        for (i in users.indices) {
            if (users[i].role == GroupUser.LEVEL_USER) {
                val sortStr = users[i].getFirstLetter()
                val firstChar = sortStr[0]
                if (firstChar.toInt() == section) {
                    return i
                }
            }
        }
        return -1
    }
}