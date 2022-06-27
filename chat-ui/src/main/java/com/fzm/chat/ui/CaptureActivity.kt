package com.fzm.chat.ui

import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.cjt2325.cameralibrary.JCameraView
import com.cjt2325.cameralibrary.listener.ErrorListener
import com.cjt2325.cameralibrary.listener.JCameraListener
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.utils.ImageUtils
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.ActivityCaptureBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.other.BarUtils
import java.io.File

/**
 * @author zhengjy
 * @since 2021/03/19
 * Description:
 */
@Route(path = MainModule.CAPTURE)
class CaptureActivity : BizActivity() {

    /**
     * 拍摄模式
     * 1：照片  2：视频  3：照片和视频
     */
    @JvmField
    @Autowired
    var mode = 1

    private val binding by init { ActivityCaptureBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        BarUtils.setStatusBarColor(
            this,
            ContextCompat.getColor(this, android.R.color.black),
            0
        )
        BarUtils.setStatusBarLightMode(this, false)
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        binding.cameraView.setSaveVideoPath(getVideoPath().absolutePath)
    }

    override fun initData() {
        //设置只能录像或只能拍照或两种都可以（默认两种都可以）
        when (mode) {
            1 -> binding.cameraView.setFeatures(JCameraView.BUTTON_STATE_ONLY_CAPTURE)
            2 -> binding.cameraView.setFeatures(JCameraView.BUTTON_STATE_ONLY_RECORDER)
            else -> binding.cameraView.setFeatures(JCameraView.BUTTON_STATE_BOTH)
        }
        //设置视频质量
        binding.cameraView.setMediaQuality(JCameraView.MEDIA_QUALITY_MIDDLE)

    }

    override fun setEvent() {
        // JCameraView监听
        binding.cameraView.setErrorLisenter(object : ErrorListener {
            override fun onError() {
                //打开Camera失败回调
                toast("open camera error")
            }

            override fun AudioPermissionError() {
                //没有录取权限回调
                toast("audio permission error")
            }
        })

        binding.cameraView.setJCameraLisenter(object : JCameraListener {
            override fun captureSuccess(bitmap: Bitmap) {
                // 获取图片bitmap
                val intent = Intent()
                intent.putExtra("type", 1)
                intent.putExtra(
                    "result",
                    ImageUtils.saveBitmapToGallery(instance, bitmap, AppConfig.APP_NAME_EN)
                )
                setResult(RESULT_OK, intent)
                finish()
            }

            override fun recordSuccess(url: String, firstFrame: Bitmap) {
                // 获取视频路径
                val intent = Intent()
                intent.putExtra("type", 2)
                intent.putExtra("video", url)
                intent.putExtra("duration", Utils.getVideoDuration(instance, url))
                setResult(RESULT_OK, intent)
                finish()
            }
        })
        // 左边按钮点击事件
        binding.cameraView.setLeftClickListener { onBackPressedSupport() }
    }

    private fun getVideoPath() = getExternalFilesDir("Video")
        ?: File(filesDir.absolutePath + File.separator + "Video")
}