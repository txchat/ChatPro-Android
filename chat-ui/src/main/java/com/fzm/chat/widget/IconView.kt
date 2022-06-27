package com.fzm.chat.widget

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * 由于ui提供的图标为iconfont格式，写了一个自定义textview提供imageview相似功能
 * 支持使用iconfont显示图标
 * 支持iconfont动画播放
 *
 * @author chengtao
 * @setAnimResource 传入动画资源
 */
class IconView : AppCompatTextView, Runnable {
    private var animResource: IntArray? = null
    private var animSize = 0
    private var duration = 0
    private var animPosition = 0
    private var isPlaying = false

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        this.typeface = Typeface.createFromAsset(context.assets, "chat_icon.ttf")
    }

    fun setIconText(resId: Int) {
        super.setBackground(null)
        setText(resId)
    }

    fun setIconBackground(resId: Int) {
        text = ""
        super.setBackgroundResource(resId)
    }

    fun setAnimResource(duration: Int, resIds: IntArray?) {
        animResource = resIds
        animSize = animResource?.size ?: 0
        this.duration = duration
    }

    fun play() {
        if (animResource != null) {
            if (!isPlaying) {
                animPosition = 0
                isPlaying = true
                postDelayed(this, duration.toLong())
            }
        }
    }

    fun stop() {
        animResource?.also {
            removeCallbacks(this)
            isPlaying = false
            animPosition = animSize - 1
            setText(it[animPosition])
        }
    }

    override fun run() {
        animResource?.also {
            if (isPlaying) {
                setText(it[animPosition])
                animPosition++
                if (animPosition >= animSize) {
                    animPosition = 0
                }
                postDelayed(this, duration.toLong())
            }
        }
    }
}