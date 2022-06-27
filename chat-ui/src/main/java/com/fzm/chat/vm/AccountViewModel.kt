package com.fzm.chat.vm

import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.Field
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.oss.MediaType
import com.fzm.chat.router.oss.OssModule
import com.fzm.chat.router.oss.OssService
import com.fzm.chat.router.route
import com.zjy.architecture.data.Result
import com.zjy.architecture.exception.ApiException
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request
import com.zjy.architecture.util.isContent

/**
 * @author zhengjy
 * @since 2021/03/19
 * Description:
 */
class AccountViewModel(
    private val delegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by delegate {

    private val ossService by route<OssService>(OssModule.APP_OSS)

    private val _avatarResult by lazy { MutableLiveData<String>() }
    val avatarResult: LiveData<String>
        get() = _avatarResult

    fun setAvatar(path: String) {
        request<String> {
            onRequest {
                val url = uploadMedia(path, MediaType.PICTURE)
                if (url.isEmpty()) {
                    Result.Error(ApiException("upload fail"))
                } else {
                    setUserInfo(listOf(Field(ChatConst.UserField.AVATAR, url, 1)))
                }
            }
            onSuccess {
                _avatarResult.value = path
            }
        }
    }

    private suspend fun uploadMedia(path: String?, @MediaType type: Int): String {
        return try {
            val endPoint = delegate.servers.value?.firstOrNull()?.address
            if (path.isContent()) {
                ossService?.uploadMedia(endPoint, path?.toUri(), type) ?: ""
            } else {
                ossService?.uploadMedia(endPoint, path, type) ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}