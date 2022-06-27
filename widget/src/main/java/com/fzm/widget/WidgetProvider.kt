package com.fzm.widget

import android.content.Context
import androidx.core.content.FileProvider

/**
 * @author zhengjy
 * @since 2020/07/30
 * Description:
 */
class WidgetProvider : FileProvider() {
    companion object {
        fun authority(context: Context) : String {
            return "${context.packageName}.widgetfileprovider"
        }
    }
}