package com.fzm.chat.ui

import android.view.View
import androidx.fragment.app.commit
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.R
import com.fzm.chat.bean.mnem.BlockListFragment
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.databinding.ActivityBlockListBinding
import com.fzm.chat.router.main.MainModule

/**
 * @author zhengjy
 * @since 2021/01/21
 * Description:
 */
@Route(path = MainModule.BLOCK_LIST_ATY)
class BlockListActivity : BizActivity() {

    private val binding by init { ActivityBlockListBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        supportFragmentManager.commit { add(R.id.fcv_container, BlockListFragment()) }
    }

    override fun initData() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
    }

    override fun setEvent() {

    }
}