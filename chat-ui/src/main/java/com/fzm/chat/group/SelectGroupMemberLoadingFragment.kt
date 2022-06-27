package com.fzm.chat.group

import android.os.Bundle
import android.view.View
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.databinding.FragmentSelectGroupMemberLoadingBinding

class SelectGroupMemberLoadingFragment : BizFragment() {

    private val binding by init<FragmentSelectGroupMemberLoadingBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {

    }

    override fun initData() {
    }

    override fun setEvent() {
    }
}