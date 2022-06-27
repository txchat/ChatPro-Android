package com.fzm.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes

/**
 * @author zhengjy
 * @since 2020/12/18
 * Description:
 */
class ShadowLayout : FrameLayout {

    private var mShadowElevation: Float = 0f
    private var mShadowAlpha: Float = 1f
    private var mShadowColor: Int = 0
    private var mRadius: Float = 0f

    constructor(context: Context) : super(context) {
        init(context, null, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs, 0, 0)
    }

    constructor(
        context: Context, attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr, 0) {
        init(context, attrs, defStyleAttr, 0)
    }

    constructor(
        context: Context, attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int, @StyleRes defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs, defStyleAttr, defStyleRes)
    }

    private fun init(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int,
        @StyleRes defStyleRes: Int
    ) {
        val ta = context.obtainStyledAttributes(
            attrs,
            R.styleable.ShadowLayout,
            defStyleAttr,
            defStyleRes
        )

        mShadowElevation = ta.getDimension(
            R.styleable.ShadowLayout_sl_shadow_elevation,
            0f
        )
        mShadowAlpha = ta.getFloat(R.styleable.ShadowLayout_sl_shadow_alpha, 0.8f)
        mShadowColor = ta.getColor(R.styleable.ShadowLayout_sl_shadow_color, Color.BLACK)
        mRadius = ta.getDimension(R.styleable.ShadowLayout_sl_radius, 5.dp.toFloat())
        ta.recycle()

        if (background == null) {
            setBackgroundColor(context.resources.getColor(R.color.widget_background))
        }
        setRadiusAndShadow()
    }

    private fun setRadiusAndShadow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            elevation = mShadowElevation
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                outlineAmbientShadowColor = mShadowColor
                outlineSpotShadowColor = mShadowColor
            }
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.alpha = mShadowAlpha
                    outline.setRoundRect(0, 0, view.width, view.height, mRadius)
                }
            }
            clipToOutline = mRadius > 0
        }
        invalidate()
    }

    fun setRadius(radius: Float) {
        if (mRadius != radius) {
            mRadius = radius
            setRadiusAndShadow()
        }
    }

    fun setShadowElevation(elevation: Float) {
        if (mShadowElevation == elevation) {
            return
        }
        mShadowElevation = elevation
        invalidateInner()
    }

    fun setShadowAlpha(shadowAlpha: Float) {
        if (mShadowAlpha == shadowAlpha) {
            return
        }
        mShadowAlpha = shadowAlpha
        invalidateInner()
    }

    fun setShadowColor(shadowColor: Int) {
        if (mShadowColor == shadowColor) {
            return
        }
        setShadowColorInner(mShadowColor)
    }

    private fun invalidateInner() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            elevation = mShadowElevation
            invalidateOutline()
        }
    }

    private fun setShadowColorInner(shadowColor: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mShadowColor = shadowColor
            outlineAmbientShadowColor = mShadowColor
            outlineSpotShadowColor = mShadowColor
        }
    }
}