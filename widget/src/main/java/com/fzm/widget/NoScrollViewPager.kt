package com.fzm.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * @author zhengjy
 * @since 2020/08/03
 * Description:
 */
class NoScrollViewPager : ViewPager {

    private var canScroll = false

    fun setCanScroll(scrollable: Boolean) {
        this.canScroll = scrollable
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return canScroll && super.onTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return canScroll && super.onInterceptTouchEvent(ev)
    }
}