package com.fzm.chat.ui

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.utils.QRCodeUtils
import com.fzm.chat.biz.utils.cropImage
import com.fzm.chat.biz.utils.onExplain
import com.fzm.chat.databinding.ActivityQrScanBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.biz.QRCodeHelper
import com.fzm.chat.utils.GlideEngine
import com.king.zxing.*
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.listener.OnResultCallbackListener
import com.permissionx.guolindev.PermissionX
import com.yalantis.ucrop.UCrop
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.other.BarUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author zhengjy
 * @since 2020/12/15
 * Description:
 */
@Route(path = MainModule.QR_SCAN)
class QRScanActivity : BizActivity(), OnCaptureCallback {

    companion object {
        private val PERMISSION_CAMERA = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    }

    /**
     * 强制返回扫码结果，不经过QRCodeHelper逻辑
     */
    @JvmField
    @Autowired
    var forceReturn: Boolean = false

    private var captureHelper: CaptureHelper? = null

    private val binding by init { ActivityQrScanBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        BarUtils.addMarginTopEqualStatusBarHeight(this, binding.ctbTitle)
        BarUtils.setStatusBarColor(this, Color.TRANSPARENT, 0)
        BarUtils.setStatusBarLightMode(this, false)
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        PermissionX.init(this)
            .permissions(PERMISSION_CAMERA)
            .onExplain(this)
            .request { allGranted, _, _ ->
                if (!allGranted) {
                    toast(R.string.biz_permission_not_granted)
                }
            }
        initCamera()
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.ctbTitle.setOnRightClickListener {
            PermissionX.init(instance)
                .permissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .onExplain(instance)
                .request { allGranted, _, _ ->
                    if (allGranted) {
                        PictureSelector.create(instance)
                            .openGallery(PictureMimeType.ofImage())
                            .maxSelectNum(1)
                            .isCompress(false)
                            .isCamera(false)
                            .theme(R.style.chat_picture_style)
                            .imageEngine(GlideEngine.createGlideEngine())
                            .forResult(object : OnResultCallbackListener<LocalMedia> {
                                override fun onResult(result: MutableList<LocalMedia>?) {
                                    if (result.isNullOrEmpty()) {
                                        toast(R.string.chat_tips_img_not_exist)
                                        return
                                    }
                                    val media = result[0]
                                    if (media.path == null) {
                                        toast(R.string.chat_tips_img_not_exist)
                                        return
                                    }
                                    lifecycleScope.launch {
                                        loading(true)
                                        val uri = Uri.parse(media.path)
                                        val text = withContext(Dispatchers.IO) {
                                            QRCodeUtils.parseCode(instance, media.path)
                                        }
                                        if (text == null) {
                                            if (!cropImage(uri)) {
                                                toast(R.string.chat_tips_not_contain_qr_code)
                                            }
                                        } else {
                                            onResult(text)
                                        }
                                        dismiss()
                                    }
                                }

                                override fun onCancel() {

                                }
                            })
                    } else {
                        instance.toast(com.fzm.chat.biz.R.string.biz_permission_not_granted)
                    }
                }
        }
    }

    override fun onResume() {
        super.onResume()
        captureHelper?.onResume()
    }

    override fun onPause() {
        super.onPause()
        captureHelper?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureHelper?.onDestroy()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        captureHelper?.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun initCamera() {
        captureHelper =
            CaptureHelper(this, binding.surfaceView, binding.viewfinderView, binding.ivTorch)
        captureHelper?.setOnCaptureCallback(this)
        captureHelper?.onCreate()
    }

    override fun onResultCallback(result: String?): Boolean {
        if (forceReturn) return false
        if (QRCodeHelper.process(this, result)) {
            finish()
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == UCrop.REQUEST_CROP) {
                lifecycleScope.launch {
                    loading(true)
                    val uri = data?.getParcelableExtra<Uri>(UCrop.EXTRA_OUTPUT_URI)
                    val result = withContext(Dispatchers.IO) {
                        QRCodeUtils.parseCode(instance, uri)
                    }
                    if (result == null) {
                        toast(R.string.chat_tips_not_contain_qr_code)
                    } else {
                        onResult(result)
                    }
                    dismiss()
                }
            } else if (requestCode == UCrop.RESULT_ERROR) {
                val throwable = data?.getSerializableExtra(UCrop.EXTRA_ERROR) as? Throwable
                toast(throwable?.message ?: "裁剪失败，未知错误")
            }
        }
    }

    private fun onResult(text: String?) {
        if (onResultCallback(text)) {
            return
        }
        val intent = Intent()
        intent.putExtra(Intents.Scan.RESULT, text)
        setResult(RESULT_OK, intent)
        finish()
    }
}