package com.fzm.widget.fab

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat

/**
 * @author zhengjy
 * @since 2022/02/25
 * Description:
 */
class FloatingScrollBehavior(context: Context?, attrs: AttributeSet?) :
    CoordinatorLayout.Behavior<View>(context, attrs) {

    private val interpolator = LinearInterpolator()
    private var outAnimating = false
    private var inAnimating = false

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        // 确保滚动方向为垂直方向
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        if (dy > 0) {
            // 向下滑动
            animateOut(child)
        } else if (dy < 0) {
            // 向上滑动
            animateIn(child)
        }
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        super.onNestedScroll(
            coordinatorLayout,
            child,
            target,
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type
        )
        if (dyConsumed > 0) {
            // 向下滑动
            animateOut(child);
        } else if (dyConsumed < 0) {
            // 向上滑动
            animateIn(child);
        }
    }

    // FAB移出屏幕动画（隐藏动画）
    private fun animateOut(fab: View) {
        if (outAnimating) return
        val layoutParams = fab.layoutParams as CoordinatorLayout.LayoutParams
        val bottomMargin = layoutParams.bottomMargin
        fab.animate()
            .translationY((fab.height + bottomMargin).toFloat())
            .setInterpolator(interpolator)
            .setDuration(150)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    outAnimating = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    outAnimating = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    outAnimating = false
                }

                override fun onAnimationRepeat(animation: Animator) {}
            })
            .start()
    }

    // FAB移入屏幕动画（显示动画）
    private fun animateIn(fab: View) {
        if (inAnimating) return
        fab.animate()
            .translationY(0f)
            .setInterpolator(interpolator)
            .setDuration(150)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    inAnimating = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    inAnimating = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    inAnimating = false
                }

                override fun onAnimationRepeat(animation: Animator) {}
            }).start()
    }
}