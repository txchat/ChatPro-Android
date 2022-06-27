package com.fzm.widget.webview

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.AttributeSet
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.fzm.widget.R
import com.fzm.widget.WidgetProvider
import com.permissionx.guolindev.PermissionX
import com.qmuiteam.qmui.widget.dialog.QMUIBottomSheet
import java.io.File

/**
 * @author zhengjy
 * @since 2020/07/24
 * Description:
 */
abstract class WebViewGroupWrapper : FrameLayout {
    companion object {
        // 选择文件（相册/视频）
        private const val REQUEST_CODE_SELECT_FILE = 101

        // 拍照/视频
        private const val REQUEST_CODE_TAKE_IMAGE = 105
        private const val REQUEST_CODE_TAKE_VIDEO = 106

        private val PERMISSION_CAMERA = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        private val PERMISSION_VIDEO = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

        private val PERMISSION_CHOOSE_FILE = listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private var jsParams: JSParams? = null
    abstract val activity: FragmentActivity
    lateinit var mWebView: FixedWebView

    private var imageUri: Uri? = null
    private var videoUri: Uri? = null

    private var onProgressChangeListener: ((Int) -> Unit)? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    protected fun initWebView(webView: FixedWebView) {
        this.mWebView = webView
        mWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                onProgressChangeListener?.invoke(newProgress)
            }

            // 选择文件
            // For Android < 5.0
            fun openFileChooser(
                uploadMsg: ValueCallback<Uri?>?,
                acceptType: String?,
                capture: String?
            ) {
                openFileChooserImpl(acceptType, uploadMsg)
            }

            // 选择文件
            // For Android >= 5.0
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onShowFileChooser(
                webView: WebView,
                uploadMsg: ValueCallback<Array<Uri?>?>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                openFileChooseImplForAndroid5(fileChooserParams, uploadMsg)
                return true
            }
        }
    }

    protected fun setOnProgressChangeListener(listener: (Int) -> Unit) {
        onProgressChangeListener = listener
    }

    /**
     * android 5.0 以下开启文件选择
     */
    private fun openFileChooserImpl(acceptType: String?, uploadMsg: ValueCallback<Uri?>?) {
        jsParams = JSParams(arrayOf(acceptType ?: ""), uploadMsg)
        chooseFile(acceptType)
    }

    /**
     * android 5.0(含) 以上开启文件选择
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun openFileChooseImplForAndroid5(
        fileChooserParams: WebChromeClient.FileChooserParams?,
        filePathCallback: ValueCallback<Array<Uri?>?>?
    ) {
        jsParams = JSParams(fileChooserParams?.acceptTypes ?: arrayOf(), filePathCallback)
        chooseFile(fileChooserParams?.acceptTypes?.firstOrNull())
    }

    /**
     * 子类可以覆盖此方法，来实现拍照，录像等功能
     */
    protected open fun chooseFile(acceptType: String?) {
        when {
            acceptType?.startsWith("image/") == true -> {
                val dialog = QMUIBottomSheet.BottomListSheetBuilder(activity)
                    .addItem("拍照")
                    .addItem("从相册中选择")
                    .setOnSheetItemClickListener { dialog, _, position, _ ->
                        if (position == 0) {
                            takePhoto()
                        } else {
                            selectFile()
                        }
                        dialog.dismiss()
                    }
                    .build()
                dialog.show()
            }
            acceptType?.startsWith("video/") == true -> {
                captureVideo()
            }
            else -> {
                selectFile()
            }
        }
    }

    /**
     * 拍照
     */
    open fun takePhoto() {
        PermissionX.init(activity)
            .permissions(PERMISSION_CAMERA)
            .onForwardToSettings { scope, _ ->
                scope.showForwardToSettingsDialog(
                    PERMISSION_CAMERA,
                    activity.getString(R.string.open_setting_to_grant_permission),
                    activity.getString(R.string.widget_confirm),
                    activity.getString(R.string.widget_cancel)
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    val photoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    imageUri = createCacheMediaUri("image/jpeg")
                    photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                        .resolveActivity(activity.packageManager)?.let {
                            activity.startActivityForResult(photoIntent, REQUEST_CODE_TAKE_IMAGE)
                        }
                } else {
                    Toast.makeText(activity, R.string.permission_not_granted, Toast.LENGTH_SHORT)
                        .show()
                    jsParams?.onReceiveValue(null)
                }
            }
    }

    /**
     * 拍视频
     */
    open fun captureVideo() {
        PermissionX.init(activity)
            .permissions(PERMISSION_VIDEO)
            .onForwardToSettings { scope, _ ->
                scope.showForwardToSettingsDialog(
                    PERMISSION_VIDEO,
                    activity.getString(R.string.open_setting_to_grant_permission),
                    activity.getString(R.string.widget_confirm),
                    activity.getString(R.string.widget_cancel)
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    videoUri = createCacheMediaUri("video/mp4")
                    videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
                        .resolveActivity(activity.packageManager)?.let {
                            activity.startActivityForResult(videoIntent, REQUEST_CODE_TAKE_VIDEO)
                        }
                } else {
                    Toast.makeText(activity, R.string.permission_not_granted, Toast.LENGTH_SHORT)
                        .show()
                    jsParams?.onReceiveValue(null)
                }
            }
    }

    /**
     * 选择图片/视频/文件
     *
     * @param mimeType  要选择文件的类型
     */
    open fun selectFile(mimeType: String? = null) {
        PermissionX.init(activity)
            .permissions(PERMISSION_CHOOSE_FILE)
            .onForwardToSettings { scope, _ ->
                scope.showForwardToSettingsDialog(
                    PERMISSION_CHOOSE_FILE,
                    activity.getString(R.string.open_setting_to_grant_permission),
                    activity.getString(R.string.widget_confirm),
                    activity.getString(R.string.widget_cancel)
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    Intent(Intent.ACTION_GET_CONTENT).let { intent ->
                        intent.addCategory(Intent.CATEGORY_OPENABLE)
                        intent.type = "*/*"
                        intent.putExtra(
                            Intent.EXTRA_MIME_TYPES,
                            when {
                                mimeType != null -> mimeType
                                jsParams?.acceptType != null -> jsParams?.acceptType
                                else -> arrayOf("*/*")
                            }
                        )
                        intent.resolveActivity(activity.packageManager)?.let {
                            activity.startActivityForResult(
                                Intent.createChooser(intent, "File Chooser"),
                                REQUEST_CODE_SELECT_FILE
                            )
                        }
                    }
                } else {
                    Toast.makeText(activity, R.string.permission_not_granted, Toast.LENGTH_SHORT)
                        .show()
                    jsParams?.onReceiveValue(null)
                }
            }
    }

    private fun createCacheMediaUri(mimeType: String): Uri? {
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val timeStamp = System.currentTimeMillis()
        val prefix = when {
            mimeType.startsWith("image") -> "IMG"
            mimeType.startsWith("video") -> "VID"
            else -> "DOC"
        }
        val file = File("${context.externalCacheDir}", "${prefix}_${timeStamp}.$ext")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, WidgetProvider.authority(context), file)
        } else {
            Uri.fromFile(file)
        }
    }

    /**
     * 在Activity中的onActivityResult里调用
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_SELECT_FILE -> {
                    val result = data?.data
                    jsParams?.onReceiveValue(result)
                }
                REQUEST_CODE_TAKE_IMAGE -> {
                    jsParams?.onReceiveValue(imageUri)
                    imageUri = null
                }
                REQUEST_CODE_TAKE_VIDEO -> {
                    jsParams?.onReceiveValue(videoUri)
                    videoUri = null
                }
            }
        } else {
            jsParams?.onReceiveValue(Uri.EMPTY)
        }
    }
}