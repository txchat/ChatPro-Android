package com.fzm.chat.bean.mnem

import android.os.Bundle
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.contact.UserListFragment
import com.fzm.chat.router.main.MainModule

/**
 * @author zhengjy
 * @since 2021/01/21
 * Description:
 */
@Route(path = MainModule.BLOCK_LIST)
class BlockListFragment : UserListFragment() {

    override fun initView(view: View, savedInstanceState: Bundle?) {
        super.initView(view, savedInstanceState)
        mAdapter.addChildClickViewIds(R.id.ll_item)
        mAdapter.setOnItemChildClickListener { _, _, position ->
            ARouter.getInstance().build(MainModule.CONTACT_INFO)
                .withString("address", users[position].address)
                .navigation()
        }
        mAdapter.setEmptyView(R.layout.layout_empty_block_list)
    }

    override fun initData() {
        super.initData()
        viewModel.blockList.observe(viewLifecycleOwner) {
            mAdapter.setList(it.sortedWith(comparator))
        }
        loadUserList()
    }

    override fun loadUserList() {
        viewModel.getBlockList()
    }
}