package com.fzm.chat.biz.base

import android.content.Context
import androidx.core.content.FileProvider

/**
 * @author zhengjy
 * @since 2021/03/16
 * Description:
 */
class BizFileProvider : FileProvider() {
    companion object {
        fun authority(context: Context) = "${context.packageName}.bizfileprovider"
    }
}