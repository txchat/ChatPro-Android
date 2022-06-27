package com.fzm.chat.contact

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.databinding.LayoutEmptyFriendsListBinding
import com.fzm.chat.router.main.MainModule

/**
 * @author zhengjy
 * @since 2021/05/17
 * Description:
 */
class SelectFriendFragment : UserListFragment() {

    companion object {
        fun create(selectUsers: ArrayList<String>?): SelectFriendFragment {
            return SelectFriendFragment().apply {
                arguments = bundleOf(
                    "selectUsers" to selectUsers
                )
            }
        }
    }

    private var selectUsers: List<String>? = null

    override val mSelectable: Boolean = true

    override fun initView(view: View, savedInstanceState: Bundle?) {
        super.initView(view, savedInstanceState)
        selectUsers = arguments?.getSerializable("selectUsers") as? ArrayList<String>?
        val bind = LayoutEmptyFriendsListBinding.inflate(layoutInflater)
        bind.tvInviteFriends.setOnClickListener {
            ARouter.getInstance().build(MainModule.QR_CODE).navigation()
        }
        mAdapter.setEmptyView(bind.root)
        mAdapter.isUseEmpty = false
    }

    override fun initData() {
        super.initData()
        viewModel.friendList.observe(viewLifecycleOwner) {
            it.forEach { user ->
                if (selectUsers?.contains(user.address) == true) {
                    user.preSelected = true
                }
            }
            mAdapter.setList(it.sortedWith(comparator))
            mAdapter.isUseEmpty = true
        }
    }

    override fun loadUserList() {
        viewModel.getFriendList()
    }
}