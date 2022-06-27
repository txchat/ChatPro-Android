package com.fzm.chat.biz.widget.pullheader

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.zjy.architecture.ext.dp
import kotlin.math.min

/**
 * @author zhengjy
 * @since 2021/08/05
 * Description:
 */
class WechatPullHeader : RelativeLayout {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    class HeaderScrollBehavior constructor(
        context: Context,
        attrs: AttributeSet
    ) : CoordinatorLayout.Behavior<WechatPullHeader>(context, attrs) {

        private val titleHeight = /*50.dp*/0

        override fun layoutDependsOn(
            parent: CoordinatorLayout,
            child: WechatPullHeader,
            dependency: View
        ): Boolean {
            return dependency is WechatPullContent
        }

        override fun onDependentViewChanged(
            parent: CoordinatorLayout,
            child: WechatPullHeader,
            dependency: View
        ): Boolean {
            val pull = dependency as WechatPullContent
            if (pull.isOpen()) {
                val headerHeight = child.getChildAt(0).measuredHeight
                if (pull.translationY == 0f) {
                    // 内容复位，则header也复位
                    child.translationY = 0f
                } else {
                    child.translationY = pull.translationY - headerHeight
                }
            } else {
                val headerHeight = child.getChildAt(0).measuredHeight
                val offset = (headerHeight - titleHeight).toFloat()
                val percent = min(dependency.translationY, offset) / offset
                child.pivotX = child.measuredWidth / 2f
                child.pivotY = 100.dp.toFloat()
                child.scaleX = percent * 0.3f + 0.7f
                child.scaleY = percent * 0.3f + 0.7f
                child.alpha = percent * 0.5f + 0.5f
                // 如果改变了 child 的大小位置必须返回 true 来刷新
            }

            return true
        }

        override fun onNestedPreScroll(
            coordinatorLayout: CoordinatorLayout,
            child: WechatPullHeader,
            target: View,
            dx: Int,
            dy: Int,
            consumed: IntArray,
            type: Int
        ) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
            // 只处理手指上滑
            if (dy > 0 && type == ViewCompat.TYPE_TOUCH) {
                val newTransY = target.translationY - dy
                if (newTransY >= 0) {
                    // 完全消耗滑动距离后没有完全贴顶或刚好贴顶
                    // 那么就声明消耗所有滑动距离，并上移 WechatPullContent
                    consumed[1] = dy // consumed[0/1] 分别用于声明消耗了x/y方向多少滑动距离
                    target.translationY = newTransY
                } else {
                    // 如果完全消耗那么会导致 WechatPullContent 超出可视区域
                    // 那么只消耗恰好让 WechatPullContent 贴顶的距离
                    consumed[1] = child.translationY.toInt()
                    child.translationY = 0f
                }
            }
        }
    }
}