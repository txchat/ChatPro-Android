package com.fzm.chat.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.alibaba.android.arouter.core.LogisticsCenter
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.router.main.MainModule

/**
 * @author zhengjy
 * @since 2021/10/06
 * Description:
 */
class SelectContactCard : ActivityResultContract<Int, Contact?>() {

    companion object {
        const val PRIVATE = 1
        const val GROUP = 1 shl 1
        const val SESSION = 1 shl 2
    }

    override fun createIntent(context: Context, input: Int): Intent {
        val postCard = ARouter.getInstance().build(MainModule.CONTACT_SELECT)
        LogisticsCenter.completion(postCard)
        return Intent(context, postCard.destination).apply {
            putExtra("channelFilter", input)
            putExtra("action", "contactCard")
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Contact? {
        return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.getSerializableExtra(
            "contact"
        ) as? Contact
    }
}