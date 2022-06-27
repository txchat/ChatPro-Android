package com.fzm.chat.conversation

import android.os.Bundle
import android.view.View
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.databinding.FragmentGroupMessageBinding

/**
 * @author zhengjy
 * @since 2020/12/09
 * Description:
 */
class GroupMsgFragment : BizFragment() {

    private val binding by init<FragmentGroupMessageBinding>()
    
    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        
    }

    override fun initData() {
        
    }

    override fun setEvent() {
        
    }
}