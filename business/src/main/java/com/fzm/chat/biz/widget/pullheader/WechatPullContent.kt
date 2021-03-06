package com.fzm.chat.biz.widget.pullheader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.LinearLayout
import android.widget.OverScroller
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.get
import com.fzm.chat.biz.R
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.haptic

/**
 * @author zhengjy
 * @since 2021/08/05
 * Description:
 */
class WechatPullContent : LinearLayout {

    companion object {
        const val CLOSED = 1
        const val OPENING = 2
        const val OPENED = 3
        const val CLOSING = 4
    }

    private val maxRadius = 30.dp

    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    private lateinit var rect: RectF
    private var percent = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ResourcesCompat.getColor(resources, R.color.biz_color_primary, null)
        style = Paint.Style.FILL
    }

    internal fun setPercent(percent: Float) {
        this.percent = percent
        invalidate()
    }

    internal fun getPercent(): Float {
        return this.percent
    }

    internal fun getMaxRadius(): Int {
        return this.maxRadius
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (!this::rect.isInitialized) {
            rect = RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
        }
        canvas?.drawRoundRect(rect, maxRadius * percent, maxRadius * percent, paint)
    }

    fun getBehavior(): ContentScrollBehavior {
        return (layoutParams as CoordinatorLayout.LayoutParams).behavior as ContentScrollBehavior
    }

    fun addOnStateChangedListener(listener: OnStateChangedListener) {
        getBehavior().addOnStateChangedListener(listener)
    }

    fun enablePull(enable: Boolean) {
        if (getBehavior().enablePull != enable) {
            getBehavior().enablePull = enable
            if (!enable) {
                closeWithNoAnimation()
            }
        }
    }

    fun getOpenRate() = getBehavior().openRate

    fun getState() = getBehavior().currentState

    fun isOpen() = getBehavior().isOpened

    fun open() {
        if (isOpen()) return
        getBehavior().stopAutoScroll()
        getBehavior().currentState = OPENING
        getBehavior().startAutoScroll(translationY.toInt(), getBehavior().maxOffset)
    }

    fun close() {
        if (!isOpen()) return
        getBehavior().stopAutoScroll()
        getBehavior().currentState = CLOSING
        getBehavior().startAutoScroll(translationY.toInt(), 0, 300)
    }

    fun closeWithNoAnimation() {
        if (!isOpen()) return
        getBehavior().currentState = CLOSING
        translationY = 0f
        setPercent(0f)
        getBehavior().currentState = CLOSED
    }

    class ContentScrollBehavior constructor(
        context: Context,
        attrs: AttributeSet
    ) : CoordinatorLayout.Behavior<WechatPullContent>(context, attrs) {

        companion object {
            const val DEFAULT_DURATION = 500
        }

        private lateinit var contentView: WechatPullContent

        private var scroller: OverScroller? = null

        private val scrollRunnable = object : Runnable {
            override fun run() {
                scroller?.let { scroller ->
                    if (scroller.computeScrollOffset()) {
                        contentView.translationY = scroller.currY.toFloat()
                        ViewCompat.postOnAnimation(contentView, this)
                    } else {
                        currentState = if (scroller.currY != 0) OPENED else CLOSED
                    }
                    val percent = scroller.currY.toFloat() / maxOffset
                    contentView.setPercent(percent)
                    setViewAlpha(percent)
                    listeners.forEach { it.onScrollOffset(contentView, scroller.currY, maxOffset) }
                }
            }
        }

        /**
         * Header??????????????????
         */
        internal var maxOffset = 0

        /**
         * Header????????????
         */
        internal var isOpened = false
            private set

        /**
         * ??????Header?????????????????????
         */
        internal val openRate = 0.25f

        /**
         * ??????????????????
         */
        internal var enablePull = true

        /**
         * ????????????
         */
        private var shouldVibrate = true

        private val listeners = mutableListOf<OnStateChangedListener>()

        /**
         * ????????????
         */
        internal var currentState = CLOSED
            set(value) {
                if (field != value) {
                    field = value
                    when (value) {
                        CLOSED -> {
                            isOpened = false
                            shouldVibrate = true
                        }
                        OPENED -> {
                            isOpened = true
                            shouldVibrate = false
                        }
                    }
                    listeners.forEach { listener -> listener.onStateChanged(value) }
                }
            }

        /**
         * ?????????????????????????????????????????????????????????
         */
        private var interrupt = false

        fun isOpened() = isOpened

        internal fun addOnStateChangedListener(listener: OnStateChangedListener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }

        private val titleHeight = /*50.dp*/0

        override fun onLayoutChild(
            parent: CoordinatorLayout,
            child: WechatPullContent,
            layoutDirection: Int
        ): Boolean {
            if (maxOffset == 0) {
                // ??????????????????????????????????????????
                parent.onLayoutChild(child, layoutDirection)
                // ?????? top ???????????? HeaderView?????????
                ViewCompat.offsetTopAndBottom(child, 0)
                contentView = child
            }
            // ???????????????????????????maxOffset?????????header????????????????????????
            maxOffset = (parent[0] as WechatPullHeader)[0].measuredHeight - titleHeight
            return false
        }

        internal fun startAutoScroll(current: Int, target: Int, duration: Int = DEFAULT_DURATION) {
            if (!this::contentView.isInitialized) return
            if (scroller == null) {
                scroller = OverScroller(contentView.context)
            }
            scroller?.also { scroller ->
                if (scroller.isFinished) {
                    contentView.removeCallbacks(scrollRunnable)
                    scroller.startScroll(0, current, 0, target - current, duration)
                    ViewCompat.postOnAnimation(contentView, scrollRunnable)
                }
            }
        }

        internal fun stopAutoScroll(toStable: Boolean = false) {
            scroller?.also { scroller ->
                if (!scroller.isFinished) {
                    if (toStable) {
                        // ????????????????????????????????????????????????
                        if (currentState == OPENING) {
                            currentState = OPENED
                        } else if (currentState == CLOSING) {
                            currentState = CLOSED
                        }
                    }
                    scroller.abortAnimation()
                    contentView.removeCallbacks(scrollRunnable)
                }
            }
        }

        private fun setViewAlpha(percent: Float) {
            // ?????????????????????
//            contentView.alpha = ((1f - percent) * 0.3f + 0.8f).coerceAtMost(1f)
        }

        override fun onStartNestedScroll(
            coordinatorLayout: CoordinatorLayout,
            child: WechatPullContent,
            directTargetChild: View,
            target: View,
            axes: Int,
            type: Int
        ): Boolean {
            interrupt = true
            stopAutoScroll()
            return (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
        }

        override fun onNestedPreScroll(
            coordinatorLayout: CoordinatorLayout,
            child: WechatPullContent,
            target: View,
            dx: Int,
            dy: Int,
            consumed: IntArray,
            type: Int
        ) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
            // ?????????????????????
            if (dy > 0 && type == ViewCompat.TYPE_TOUCH) {
                val newTransY = child.translationY - dy
                if (newTransY >= 0) {
                    // ????????????????????????????????????????????????????????????
                    // ??????????????????????????????????????????????????? WechatPullContent
                    consumed[1] = dy // consumed[0/1] ???????????????????????????x/y????????????????????????
                    child.translationY = newTransY
                } else {
                    // ????????????????????????????????? WechatPullContent ??????????????????
                    // ???????????????????????? WechatPullContent ???????????????
                    consumed[1] = child.translationY.toInt()
                    child.translationY = 0f
                    shouldVibrate = true
                }
                if (child.translationY == 0f && currentState != CLOSED) {
                    // ?????????????????????
                    child.post {
                        currentState = CLOSING
                        currentState = CLOSED
                    }
                }
                val percent = child.translationY / maxOffset
                child.setPercent(percent)
                setViewAlpha(percent)
                listeners.forEach { it.onScrollOffset(child, child.translationY.toInt(), maxOffset) }
            }
        }

        override fun onNestedScroll(
            coordinatorLayout: CoordinatorLayout,
            child: WechatPullContent,
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            type: Int,
            consumed: IntArray
        ) {
            super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed)
            if (!enablePull) return
            if (dyUnconsumed < 0 && type == ViewCompat.TYPE_TOUCH) {
                // ????????????????????????????????????
                val newTransY = child.translationY - dyUnconsumed
                if (newTransY <= 0) {
                    child.translationY = 0f
                } else {
                    child.translationY = minOf(newTransY, maxOffset.toFloat())
                }
                if (child.translationY == maxOffset.toFloat() && currentState != OPENED) {
                    // ?????????????????????
                    child.post {
                        currentState = OPENING
                        currentState = OPENED
                    }
                }
                val percent = child.translationY / maxOffset
                if (!isOpened && shouldVibrate && percent > openRate) {
                    child.haptic(HapticFeedbackConstants.LONG_PRESS)
                    shouldVibrate = false
                }
                child.setPercent(percent)
                setViewAlpha(percent)
                listeners.forEach { it.onScrollOffset(child, child.translationY.toInt(), maxOffset) }
            }
        }

        override fun onStopNestedScroll(
            coordinatorLayout: CoordinatorLayout,
            child: WechatPullContent,
            target: View,
            type: Int
        ) {
            super.onStopNestedScroll(coordinatorLayout, child, target, type)
            if (currentState == OPENED) {
                if (child.translationY > 0 && child.translationY < maxOffset) {
                    stopAutoScroll()
                    currentState = CLOSING
                    startAutoScroll(child.translationY.toInt(), 0, 300)
                }
            } else {
                if (!shouldVibrate) {
                    // ??????????????????????????????????????????????????????shouldVibrate
                    shouldVibrate = true
                }
                if (currentState != CLOSING || interrupt) {
                    interrupt = false
                    // ????????????????????????????????????????????????????????????
                    if (child.translationY <= maxOffset * openRate) {
                        stopAutoScroll()
                        startAutoScroll(child.translationY.toInt(), 0, 300)
                    } else {
                        stopAutoScroll()
                        currentState = OPENING
                        startAutoScroll(child.translationY.toInt(), maxOffset)
                    }
                }
            }
        }

        override fun onNestedPreFling(
            coordinatorLayout: CoordinatorLayout,
            child: WechatPullContent,
            target: View,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (currentState == OPENING || currentState == OPENED) {
                if (velocityY > 5000) {
                    interrupt = false
                    currentState = CLOSING
                    startAutoScroll(child.translationY.toInt(), 0, 300)
                    return true
                }
            }
            return child.translationY > 0
        }
    }

    interface OnStateChangedListener {
        fun onStateChanged(state: Int)

        fun onScrollOffset(view: View, offset: Int, maxOffset: Int)
    }
}