package com.fzm.chat.animation

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsetsAnimation
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.core.view.NestedScrollingParent3

@RequiresApi(Build.VERSION_CODES.R)
class InsetsAnimationRelativeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), NestedScrollingParent3 {

    private val delegate = InsetsNestedScrollingParent(this)

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        if (!isEnabled) return false
        return delegate.onStartNestedScroll(child, target, axes, type)
    }

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        if (!isEnabled) return
        delegate.onNestedScrollAccepted(child, target, axes, type)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        if (!isEnabled) return
        delegate.onStopNestedScroll(target, type)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        if (!isEnabled) return
        delegate.onNestedScroll(
            target,
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type,
            consumed
        )
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        if (!isEnabled) return
        delegate.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        if (!isEnabled) return
        delegate.onNestedPreScroll(target, dx, dy, consumed, type)
    }

    override fun dispatchWindowInsetsAnimationPrepare(animation: WindowInsetsAnimation) {
        super.dispatchWindowInsetsAnimationPrepare(animation)
        if (!isEnabled) return
        delegate.dispatchWindowInsetsAnimationPrepare(animation)
    }
}

