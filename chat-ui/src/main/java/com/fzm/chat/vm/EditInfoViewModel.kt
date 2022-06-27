package com.fzm.chat.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.Field
import com.fzm.chat.core.session.LoginDelegate
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request

/**
 * @author zhengjy
 * @since 2020/12/16
 * Description:
 */
class EditInfoViewModel(
    delegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by delegate {

    private val _nicknameResult by lazy { MutableLiveData<String>() }
    val nicknameResult: LiveData<String>
        get() = _nicknameResult

    fun setNickname(nickname: String) {
        request<String> {
            onRequest {
                setUserInfo(listOf(Field(ChatConst.UserField.NICKNAME, nickname, 1)))
            }
            onSuccess {
                _nicknameResult.value = it
            }
        }
    }
}