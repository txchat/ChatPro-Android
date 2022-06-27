package com.fzm.chat.biz.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import com.fzm.chat.biz.R
import com.fzm.widget.RoundRectImageView
import com.zjy.architecture.ext.dp

/**
 * @author zhengjy
 * @since 2019/07/22
 * Description:右下角有小图标的ImageView
 */
class ChatAvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : RoundRectImageView(context, attrs) {

    // 右下角icon大小
    private var mIconSize: Float
    private var mChangeAlpha: Boolean

    // 右下角icon图标
    private var mIconRes: Int
    private var mIcon: Bitmap? = null
    private val mPaint = Paint()

    private fun initIcon() {
        if (mIconRes != -1) {
            val drawable = AppCompatResources.getDrawable(context, mIconRes)
            if (drawable != null) {
                mIcon = drawable2Bitmap(drawable, mIconSize.toInt(), mIconSize.toInt())
            }
        } else {
            mIcon = null
        }
    }

    fun setIconSize(iconSize: Int) {
        mIconSize = iconSize.toFloat()
        initIcon()
        invalidate()
    }

    fun setIconRes(iconRes: Int) {
        mIconRes = iconRes
        initIcon()
        invalidate()
    }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        alpha = if (mChangeAlpha && pressed) {
            0.8f
        } else {
            1f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mIcon != null) {
            canvas.drawBitmap(mIcon!!, (measuredWidth - mIcon!!.width).toFloat(), (measuredHeight - mIcon!!.height).toFloat(), mPaint)
        }
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ChatAvatarView)
        mIconSize = ta.getDimension(R.styleable.ChatAvatarView_iconSize, 10.dp.toFloat())
        mIconRes = ta.getResourceId(R.styleable.ChatAvatarView_iconSrc, -1)
        mChangeAlpha = ta.getBoolean(R.styleable.ChatAvatarView_changeAlpha, false)
        ta.recycle()
        initIcon()
    }
}