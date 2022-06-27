package com.fzm.widget

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView

/**
 * @author zhengjy
 * @since 2018/07/12
 * Description:圆角矩形ImageView
 */
open class RoundRectImageView : AppCompatImageView {

    private val simplePaint = Paint()

    // 圆角半径
    private var mRadius = 0f
    private var mRectSrc: Rect = Rect()
    private var mRectDest: Rect = Rect()

    private var mBitmap: Bitmap? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RoundRectImageView)
            mRadius = typedArray.getDimension(R.styleable.RoundRectImageView_cornerRadius, dp2px(5))
            typedArray.recycle()
        }
        scaleType = ScaleType.CENTER_CROP
    }

    override fun onDraw(canvas: Canvas) {
//        val bitmap = mBitmap
        // TODO：支持Gif显示，但可能会有一点性能问题，2021年7月1日 16:12:42
        val bitmap = getRoundBitmap(drawable)
        if (bitmap != null) {
            mRectSrc.set(0, 0, bitmap.width, bitmap.height)
            mRectDest.set(0, 0, width, height)
            canvas.drawBitmap(bitmap, mRectSrc, mRectDest, simplePaint)
        } else {
            super.onDraw(canvas)
        }
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        initializeBitmap()
        invalidate()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        initializeBitmap()
        invalidate()
    }

    override fun setImageResource(@DrawableRes resId: Int) {
        super.setImageResource(resId)
        initializeBitmap()
        invalidate()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        initializeBitmap()
        invalidate()
    }

    private fun initializeBitmap() {
        mBitmap = getRoundBitmap(drawable)
    }

    /**
     * 获取圆角矩形图片方法
     *
     */
    private fun getRoundBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null || drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            return null
        }
        val output = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val paint = Paint().also { it.isAntiAlias = true }
        val canvas = Canvas(output)
        val rect = Rect(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val rectF = RectF(rect)
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawRoundRect(rectF, mRadius, mRadius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(drawable2Bitmap(drawable), rect, rect, paint)
        return output
    }

    protected fun drawable2Bitmap(drawable: Drawable): Bitmap {
        return drawable2Bitmap(drawable, drawable.intrinsicWidth, drawable.intrinsicHeight)
    }

    protected fun drawable2Bitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(
            width, height,
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }


    private fun dp2px(dp: Int): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        )
    }
}