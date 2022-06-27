package com.fzm.update.provider

import android.content.Context
import androidx.core.content.FileProvider

/**
 * @author zhengjy
 * @since 2020/08/11
 * Description:
 */
class UpdateProvider : FileProvider() {
    companion object {
        fun authority(context: Context) : String {
            return "${context.packageName}.updateprovider"
        }
    }
}