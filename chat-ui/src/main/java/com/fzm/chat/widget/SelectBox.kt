package com.fzm.chat.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox

/**
 * @author zhengjy
 * @since 2021/01/21
 * Description:
 */
internal class SelectBox : AppCompatCheckBox {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun toggle() {

    }
}