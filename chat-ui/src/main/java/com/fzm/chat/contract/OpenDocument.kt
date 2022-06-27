package com.fzm.chat.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

/**
 * @author zhengjy
 * @since 2021/08/23
 * Description:
 */
class OpenDocument : ActivityResultContract<Array<String>, Uri?>() {

    override fun createIntent(context: Context, input: Array<String>): Intent {
        // 不能使用系统自带的OpenDocument，要用Intent.createChooser()包裹原始intent，否则获取的uri只能访问一次
        return Intent.createChooser(
            Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_MIME_TYPES, input)
                .setType("*/*"), "File Chooser"
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
    }
}