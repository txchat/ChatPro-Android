package com.fzm.widget.webview

import android.net.Uri
import android.os.Build
import android.webkit.ValueCallback

/**
 * @author zhengjy
 * @since 2020/07/24
 * Description:
 */
class JSParams(
    /**
     * 文件mime类型
     */
    var acceptType: Array<String>,
    /**
     * js回调
     */
    var callback: Any?
) {

    fun onReceiveValue(value: Uri?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            (callback as ValueCallback<Array<Uri?>?>?)?.onReceiveValue(arrayOf(value))
        } else {
            (callback as ValueCallback<Uri?>?)?.onReceiveValue(value)
        }
        callback = null
    }
}