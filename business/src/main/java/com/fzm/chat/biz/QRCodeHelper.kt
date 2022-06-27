package com.fzm.chat.biz

import android.content.ClipData
import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.router.biz.BizModule
import com.zjy.architecture.ext.clipboardManager

/**
 * @author zhengjy
 * @since 2019/05/15
 * Description:
 */
object QRCodeHelper {

    interface Processor {

        val priority: Int get() = 0

        fun process(context: Context, result: String?): Boolean
    }

    private val processors = mutableListOf<Processor>()

    fun registerQRProcessor(processor: Processor) {
        if (!processors.contains(processor)) {
            processors.add(processor)
        }
    }

    fun unregisterQRProcessor(processor: Processor) {
        processors.remove(processor)
    }

    fun process(context: Context, result: String?): Boolean {
        processors.sortedByDescending { it.priority }.forEach {
            try {
                if (it.process(context, result)) return true
            } catch (e: Exception) {

            }
        }
        try {
            val uri = Uri.parse(result)
            if (uri.scheme != null && uri.scheme!!.toLowerCase().startsWith("http")) {
                ARouter.getInstance().build(BizModule.WEB_ACTIVITY)
                    .withString("url", result)
                    .navigation()
                return true
            } else {
                throw Exception()
            }
        } catch (e: Exception) {
            context.clipboardManager?.setPrimaryClip(ClipData.newPlainText("QRCode", result))
            Toast.makeText(
                context,
                context.getString(R.string.biz_tips_qr_unrecongnize),
                Toast.LENGTH_SHORT
            ).show()
        }
        return false
    }
}