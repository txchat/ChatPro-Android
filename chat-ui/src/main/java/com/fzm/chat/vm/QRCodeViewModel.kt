package com.fzm.chat.vm

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.utils.ImageUtils
import com.fzm.chat.core.session.LoginDelegate
import com.zjy.architecture.mvvm.LoadingViewModel

/**
 * @author zhengjy
 * @since 2021/01/06
 * Description:
 */
class QRCodeViewModel(
    delegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by delegate {

    private val _saveResult by lazy { MutableLiveData<Uri>() }
    val saveResult: LiveData<Uri>
        get() = _saveResult

    fun saveQRCode(context: Context, bitmap: Bitmap) {
        try {
            loading(true)
            val uri = ImageUtils.saveBitmapToGallery(context, bitmap, AppConfig.APP_NAME_EN)
            dismiss()
            _saveResult.value = uri
        } finally {
            bitmap.recycle()
            dismiss()
        }
    }
}