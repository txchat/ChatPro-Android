package com.fzm.chat.contact

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.biz.widget.HighlightTextView
import com.fzm.chat.core.data.comparator.PinyinComparator
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.databinding.FragmentUserListBinding
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.visible
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/01/21
 * Description:
 */
abstract class UserListFragment : BizFragment() {

    protected val viewModel by viewModel<ContactListViewModel>()
    protected lateinit var mAdapter: BaseQuickAdapter<FriendUser, BaseViewHolder>
    protected val users = mutableListOf<FriendUser>()
    protected val comparator = PinyinComparator()
    protected open val mSelectable: Boolean = false

    private val selectedMap = mutableMapOf<String, Boolean>()
    private lateinit var manager: LinearLayoutManager

    protected val binding by init<FragmentUserListBinding>()
    
    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        manager = LinearLayoutManager(requireContext())
        binding.rvFriends.layoutManager = manager
        mAdapter = object :
            BaseQuickAdapter<FriendUser, BaseViewHolder>(R.layout.item_chat_contact, users) {
            override fun convert(holder: BaseViewHolder, item: FriendUser) {
                holder.setText(R.id.tag, item.getFirstLetter())
                val position = users.indexOf(item)
                if (position > 0) {
                    val last = users[position - 1].getFirstLetter()
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
                val cbSelect = holder.getView<CheckBox>(R.id.cb_select)
                if (mSelectable) {
                    cbSelect.visible()
                    cbSelect.setOnClickListener {
                        selectedMap[item.getId()] = cbSelect.isChecked
                        mOnSelectListener?.invoke(item, cbSelect.isChecked)
                    }
                    holder.getView<View>(R.id.ll_item).setOnClickListener {
                        if (!item.preSelected) {
                            cbSelect.performClick()
                        }
                    }
                    cbSelect.isChecked = item.preSelected || selectedMap[item.getId()] ?: false
                    cbSelect.isEnabled = !item.preSelected
                } else {
                    cbSelect.gone()
                }

                holder.getView<ChatAvatarView>(R.id.iv_avatar)
                    .load(item.getDisplayImage(), R.mipmap.default_avatar_round)
                holder.getView<HighlightTextView>(R.id.tv_name).highlightSearchText(item.getDisplayName(), viewModel.keywords.value)
            }
        }
        binding.rvFriends.adapter = mAdapter
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

    private var mOnSelectListener: ((FriendUser, Boolean) -> Unit)? = null

    /**
     * 好友选中取消监听
     */
    fun setOnSelectListener(listener: (FriendUser, Boolean) -> Unit) {
        mOnSelectListener = listener
    }

    /**
     * 设置搜索关键字
     */
    fun setSearchKey(keyword: String) {
        viewModel.setSearchKey(keyword)
    }

    /**
     * 外部用于清除好友选中状态
     */
    fun clearCheck(user: FriendUser) {
        selectedMap.remove(user.getId())
        val index = users.indexOf(user)
        if (index >= 0) {
            (mAdapter.getViewByPosition(index, R.id.cb_select) as? CheckBox)?.isChecked = false
        }
    }

    private fun getPositionForSection(section: Int): Int {
        for (i in users.indices) {
            val sortStr = users[i].getFirstLetter()
            val firstChar = sortStr[0]
            if (firstChar.toInt() == section) {
                return i
            }
        }
        return -1
    }

    override fun initData() {
        viewModel.loading.observe(viewLifecycleOwner) {
            if (!it.loading) {
                binding.refresh.finishRefresh()
            }
        }
    }

    override fun setEvent() {
        binding.refresh.setEnableLoadMore(false)
        binding.refresh.setOnRefreshListener {
            loadUserList()
        }
    }

    abstract fun loadUserList()
}