package com.fzm.chat.biz.widget.pullheader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.zjy.architecture.ext.dp

/**
 * @author zhengjy
 * @since 2021/08/10
 * Description:
 */
class ExtendPoint @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mPercent = 0f

    private var mMaxRadius: Float = 4.dp.toFloat()

    private var mMaxDist = 60f

    private var mPaintAlpha = 0f

    private val mPaint: Paint = Paint()

    fun setMaxRadius(maxRadius: Int) {
        this.mMaxRadius = maxRadius.toFloat()
    }

    fun setMaxDist(maxDist: Float) {
        this.mMaxDist = maxDist
    }

    fun setPercent(percent: Float) {
        if (percent != this.mPercent) {
            this.mPercent = percent.coerceAtMost(1f)
            invalidate()
        }
    }

    fun setPaintAlpha(alpha: Float) {
        if (alpha != this.mPaintAlpha) {
            this.mPaintAlpha = alpha.coerceAtMost(1f)
            mPaint.alpha = (mPaintAlpha * 255).toInt()
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        if (mPercent > 0.5f) {
            val afterPercent = (mPercent - 0.5f) / 0.5f
            val radius = mMaxRadius - mMaxRadius / 3 * afterPercent
            canvas.drawCircle(centerX, centerY, radius, mPaint)
            canvas.drawCircle(
                centerX - afterPercent * mMaxDist,
                centerY,
                mMaxRadius * 2 / 3,
                mPaint
            )
            canvas.drawCircle(
                centerX + afterPercent * mMaxDist,
                centerY,
                mMaxRadius * 2 / 3,
                mPaint
            )
        } else {
            val radius = mPercent * 2 * mMaxRadius
            canvas.drawCircle(centerX, centerY, radius, mPaint)
        }
    }

    init {
        mPaint.isAntiAlias = true
        mPaint.color = Color.GRAY
    }

    class ExtendBehavior constructor(
        context: Context,
        attrs: AttributeSet
    ) : CoordinatorLayout.Behavior<ExtendPoint>(context, attrs) {
        override fun layoutDependsOn(
            parent: CoordinatorLayout,
            child: ExtendPoint,
            dependency: View
        ): Boolean {
            return dependency is WechatPullContent
        }

        override fun onDependentViewChanged(
            parent: CoordinatorLayout,
            child: ExtendPoint,
            dependency: View
        ): Boolean {
            val pull = dependency as WechatPullContent
            val openRate = pull.getBehavior().openRate
            if (pull.getPercent() > openRate) {
                val bgRate = (pull.getPercent() - openRate) / (1f - openRate)
                child.alpha = (1f - bgRate).coerceAtLeast(0f)
                val pointRate = (pull.getPercent() - openRate) / openRate
                child.setPaintAlpha((1f - pointRate).coerceAtLeast(0f))
            } else {
                child.alpha = 1f
                child.setPaintAlpha(1f)
                child.setPercent(pull.getPercent() / openRate)
            }
            child.layoutParams = child.layoutParams.apply {
                height = (dependency.translationY + pull.getMaxRadius() * pull.getPercent()).toInt()
            }
            return true
        }
    }
}