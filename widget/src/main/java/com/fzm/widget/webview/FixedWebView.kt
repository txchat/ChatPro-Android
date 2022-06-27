package com.fzm.widget.webview

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.AttributeSet
import wendu.dsbridge.DWebView

/**
 * @author zhengjy
 * @since 2020/07/27
 * Description:修复Android5.0和5.1报错的问题
 */
class FixedWebView : DWebView {

    constructor(context: Context) : super(getFixedContext(context))
    constructor(context: Context, attrs: AttributeSet?) : super(getFixedContext(context), attrs)

    companion object {
        fun getFixedContext(context: Context): Context {
            return if (Build.VERSION.SDK_INT in 21..22) {
                context.createConfigurationContext(Configuration())
            } else {
                context
            }
        }
    }
}