package com.fzm.chat.biz.base

import android.os.Bundle
import android.view.View
import com.fzm.chat.biz.R
import com.zjy.architecture.base.BaseFragment

/**
 * @author zhengjy
 * @since 2022/03/01
 * Description:空白加载页面
 */
class EmptyLoadingFragment : BaseFragment() {

    companion object {
        fun create(): EmptyLoadingFragment {
            return EmptyLoadingFragment()
        }
    }

    override val layoutId: Int
        get() = R.layout.layout_empty_loading

    override fun initView(view: View, savedInstanceState: Bundle?) {

    }

    override fun initData() {

    }

    override fun setEvent() {

    }
}