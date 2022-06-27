package com.fzm.chat.widget

import android.content.Context
import android.util.AttributeSet
import android.view.*
import androidx.appcompat.widget.AppCompatTextView

/**
 * @author zhengjy
 * @since 2021/12/21
 * Description:
 */
internal class FixedSelectableTextView : AppCompatTextView {

    private var lastDownTime = 0L

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        setTextIsSelectable(true)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var handle = false
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> lastDownTime = System.currentTimeMillis()
            MotionEvent.ACTION_UP -> {
                val upTime = System.currentTimeMillis()
                handle = if (upTime - lastDownTime < ViewConfiguration.getLongPressTimeout()) {
                    performClick()
                    true
                } else false
            }
        }
        return handle || super.onTouchEvent(event)
    }
}