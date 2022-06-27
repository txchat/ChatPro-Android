package com.fzm.chat.biz.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.fzm.chat.biz.R
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.config.PictureSelectionConfig
import com.luck.picture.lib.manager.UCropManager
import com.luck.picture.lib.tools.DateUtils
import com.luck.picture.lib.tools.DoubleUtils
import com.luck.picture.lib.tools.PictureFileUtils
import com.luck.picture.lib.tools.ToastUtils
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.request.PermissionBuilder
import com.yalantis.ucrop.UCrop
import com.zjy.architecture.ext.toast
import java.io.File

/**
 * @author zhengjy
 * @since 2020/08/13
 * Description:
 */

val PERMISSION_CAMERA = listOf(
    Manifest.permission.CAMERA,
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)

val PERMISSION_CHOOSE_FILE = listOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)

fun FragmentActivity.takePhoto(imageUri: Uri?, requestCode: Int) {
    PermissionX.init(this)
        .permissions(PERMISSION_CAMERA)
        .onExplain(this)
        .request { allGranted, _, _ ->
            if (allGranted) {
                val photoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                    .resolveActivity(this.packageManager)?.let {
                        this.startActivityForResult(photoIntent, requestCode)
                    }
            } else {
                this.toast(R.string.biz_permission_not_granted)
            }
        }
}

fun Fragment.takePhoto(imageUri: Uri?, requestCode: Int) {
    PermissionX.init(this)
        .permissions(PERMISSION_CAMERA)
        .onExplain(this)
        .request { allGranted, _, _ ->
            if (allGranted) {
                val photoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                    .resolveActivity(this.requireContext().packageManager)?.let {
                        this.startActivityForResult(photoIntent, requestCode)
                    }
            } else {
                this.requireContext().toast(R.string.biz_permission_not_granted)
            }
        }
}

fun FragmentActivity.selectPhoto(requestCode: Int) {
    PermissionX.init(this)
        .permissions(PERMISSION_CHOOSE_FILE)
        .onExplain(this)
        .request { allGranted, _, _ ->
            if (allGranted) {
                Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ).let { intent ->
                    intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                    intent.resolveActivity(this.packageManager)?.let {
                        this.startActivityForResult(
                            Intent.createChooser(intent, "Image Chooser"),
                            requestCode
                        )
                    }
                }
            } else {
                this.toast(R.string.biz_permission_not_granted)
            }
        }
}

fun Fragment.selectPhoto(requestCode: Int) {
    PermissionX.init(this)
        .permissions(PERMISSION_CHOOSE_FILE)
        .onExplain(this)
        .request { allGranted, _, _ ->
            if (allGranted) {
                Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ).let { intent ->
                    intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                    intent.resolveActivity(this.requireContext().packageManager)?.let {
                        this.startActivityForResult(
                            Intent.createChooser(intent, "Image Chooser"),
                            requestCode
                        )
                    }
                }
            } else {
                this.requireContext().toast(R.string.biz_permission_not_granted)
            }
        }
}

fun PermissionBuilder.onExplain(activity: Activity): PermissionBuilder {
    return onExplainRequestReason { scope, deniedList, _ ->
        scope.showRequestReasonDialog(
            deniedList,
            activity.getString(R.string.biz_need_permission_to_use),
            activity.getString(R.string.biz_confirm),
            activity.getString(R.string.biz_cancel)
        )
    }.onForwardToSettings { scope, deniedList ->
        scope.showForwardToSettingsDialog(
            deniedList,
            activity.getString(R.string.open_setting_to_grant_permission),
            activity.getString(R.string.widget_confirm),
            activity.getString(R.string.widget_cancel)
        )
    }
}

fun PermissionBuilder.onExplain(fragment: Fragment): PermissionBuilder {
    return onExplainRequestReason { scope, deniedList, _ ->
        scope.showRequestReasonDialog(
            deniedList,
            fragment.getString(R.string.biz_need_permission_to_use),
            fragment.getString(R.string.biz_confirm),
            fragment.getString(R.string.biz_cancel)
        )
    }.onForwardToSettings { scope, deniedList ->
        scope.showForwardToSettingsDialog(
            deniedList,
            fragment.getString(R.string.open_setting_to_grant_permission),
            fragment.getString(R.string.widget_confirm),
            fragment.getString(R.string.widget_cancel)
        )
    }
}

fun Activity.cropImage(uri: Uri?): Boolean {
    if (uri == null) {
        toast("图片不存在")
        return false
    }
    ofCrop(this, uri.toString(), contentResolver.getType(uri) ?: "image/jpg")
    return true
}

fun Fragment.cropImage(uri: Uri?): Boolean {
    if (uri == null) {
        toast("图片不存在")
        return false
    }
    ofCrop(requireActivity(), uri.toString(), requireContext().contentResolver.getType(uri) ?: "image/jpg")
    return true
}

fun ofCrop(activity: Activity, originalPath: String?, mimeType: String) {
    if (DoubleUtils.isFastDoubleClick()) {
        return
    }
    if (TextUtils.isEmpty(originalPath)) {
        ToastUtils.s(
            activity.applicationContext,
            activity.getString(R.string.picture_not_crop_data)
        )
        return
    }
    val config = PictureSelectionConfig.getInstance()
    val isHttp = PictureMimeType.isHasHttp(originalPath)
    val suffix = mimeType.replace("image/", ".")
    val file = File(
        PictureFileUtils.getDiskCacheDir(activity.applicationContext),
        if (TextUtils.isEmpty(config.renameCropFileName)) DateUtils.getCreateFileName("IMG_CROP_") + suffix else config.renameCropFileName
    )
    val uri =
        if (isHttp || PictureMimeType.isContent(originalPath)) Uri.parse(originalPath) else Uri.fromFile(
            File(originalPath)
        )
    val options = UCropManager.basicOptions(activity).apply {
        isOpenWhiteStatusBar(true)
    }
    UCrop.of(uri, Uri.fromFile(file))
        .withOptions(options)
        .withAspectRatio(1f, 1f)
        .startAnimationActivity(
            activity,
            PictureSelectionConfig.windowAnimationStyle.activityCropEnterAnimation
        )
}

/**
 * 压缩图片
 */
//suspend fun compress(context: Context, uri: Uri?, size: Int = 100, quality: Int = 60): Uri? {
//    if (uri == null) return null
//    val files = withContext(Dispatchers.IO) {
//        tryWith { Luban.with(context).load(uri).ignoreBy(size).setQuality(quality).get() }
//    }
//    return files?.firstOrNull()?.toUri(context, "${context.packageName}.bizfileprovider")
//}