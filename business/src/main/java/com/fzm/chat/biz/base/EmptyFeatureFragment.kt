package com.fzm.chat.biz.base

import android.os.Bundle
import android.view.View
import com.fzm.chat.biz.R
import com.zjy.architecture.base.BaseFragment

/**
 * @author zhengjy
 * @since 2020/07/30
 * Description:待开发Fragment
 */
class EmptyFeatureFragment : BaseFragment() {

    companion object {
        fun create(): EmptyFeatureFragment {
            return EmptyFeatureFragment()
        }
    }

    override val layoutId: Int
        get() = R.layout.fragment_empty_feature

    override fun initView(view: View, savedInstanceState: Bundle?) {

    }

    override fun initData() {

    }

    override fun setEvent() {

    }
}