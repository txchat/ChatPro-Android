package com.fzm.chat.biz.webview

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AttributeSet
import android.util.Base64
import android.view.LayoutInflater
import androidx.fragment.app.FragmentActivity
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.databinding.LayoutBizWebViewBinding
import com.fzm.chat.biz.utils.ImageUtils
import com.fzm.chat.biz.utils.defaultCustomTabIntent
import com.fzm.widget.webview.WebViewGroupWrapper
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.toast
import com.zjy.architecture.ext.visible

/**
 * @author zhengjy
 * @since 2020/07/31
 * Description:
 */
class BizWebView : WebViewGroupWrapper {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var showProgress = true

    private var isInit = false

    /**
     * 首次打开加载的链接
     */
    private var initUrl = ""

    init {
        val binding = LayoutBizWebViewBinding.inflate(LayoutInflater.from(context), this, true)
        initWebView(binding.webView)
        setOnProgressChangeListener {
            if (showProgress) {
                if (it == 100) {
                    binding.progressBar.gone()
                    showProgress = false
                } else {
                    binding.progressBar.visible()
                    binding.progressBar.progress = it
                }
            }
        }
        mWebView.setDownloadListener { url, _, _, _, _ ->
            if (url.startsWith("data:image/")) {
                val data = url.split(",")
                if (data.size > 1) {
                    saveBitmap(data[1])
                } else {
                    activity.toast("图片格式错误")
                }
                return@setDownloadListener
            }
            defaultCustomTabIntent(activity).launchUrl(activity, Uri.parse(url))
            if (url == initUrl) activity.finish()
        }
    }

    private fun saveBitmap(data: String) {
        try {
            val bytes = Base64.decode(data, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ImageUtils.saveBitmapToGallery(activity, bitmap, AppConfig.APP_NAME_EN)
            activity.toast("图片保存成功")
        } catch (e: Exception) {
            activity.toast("图片保存失败")
        }
    }

    override val activity: FragmentActivity
        get() = fragmentActivity
                ?: throw RuntimeException("activity is null, please call setActivity() first.")

    private var fragmentActivity: FragmentActivity? = null

    fun setActivity(activity: FragmentActivity?) {
        fragmentActivity = activity
    }

    /**
     * 设置WebViewGroup
     */
    fun setup(url: String, block: BizWebView.() -> Unit) {
        if (!isInit) {
            initUrl = url
            block()
            isInit = true
        }
    }
}