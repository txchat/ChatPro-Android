package com.fzm.chat.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.alibaba.android.arouter.core.LogisticsCenter
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.router.oa.OAModule

class CompanyMemberSelector : ActivityResultContract<String, List<String>?>() {

    override fun createIntent(context: Context, input: String): Intent {
        val postCard = ARouter.getInstance().build(OAModule.WEB)
        LogisticsCenter.completion(postCard)
        return Intent(context, postCard.destination).apply { putExtra("url", input) }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): List<String>? {
        if (intent == null || resultCode != Activity.RESULT_OK) return null
        return intent.getStringArrayListExtra("users")?.toList()
    }
}