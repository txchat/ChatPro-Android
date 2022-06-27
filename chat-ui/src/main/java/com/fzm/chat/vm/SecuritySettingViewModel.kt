package com.fzm.chat.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.core.backup.LocalAccountManager
import com.fzm.chat.core.session.LoginDelegate
import com.zjy.architecture.data.Event
import com.zjy.architecture.data.IGNORE_ERROR
import com.zjy.architecture.data.Result
import com.zjy.architecture.exception.ApiException
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author zhengjy
 * @since 2020/12/16
 * Description:
 */
class SecuritySettingViewModel(
    private val manager: LocalAccountManager,
    delegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by delegate {

    private val _verified by lazy { MutableLiveData<Event<Boolean>>() }
    val verified: LiveData<Event<Boolean>>
        get() = _verified

    private val _mnemonic by lazy { MutableLiveData<Event<String>>() }
    val mnemonic: LiveData<Event<String>>
        get() = _mnemonic

    private val _privateKey by lazy { MutableLiveData<Event<String>>() }
    val privateKey: LiveData<Event<String>>
        get() = _privateKey

    private val _errorPwd by lazy { MutableLiveData<Event<Unit>>() }
    val errorPwd: LiveData<Event<Unit>>
        get() = _errorPwd

    private val _setEncPwd by lazy { MutableLiveData<Event<String>>() }
    val setEncPwd: LiveData<Event<String>>
        get() = _setEncPwd

    private val _refresh by lazy { MutableLiveData<Unit>() }
    val refresh: LiveData<Unit>
        get() = _refresh

    fun getMnemonic(password: String) {
        request<Event<String>> {
            onRequest {
                withContext(Dispatchers.IO) {
                    val success = preference.checkPassword(password)
                    if (success) {
                        Result.Success(Event(preference.getMnemonicString(password) ?: ""))
                    } else {
                        Result.Error(ApiException(IGNORE_ERROR))
                    }
                }
            }
            onSuccess {
                _mnemonic.value = it
            }
            onFail {
                _errorPwd.value = Event(Unit)
            }
        }
    }

    @Deprecated("不需要导出私钥")
    fun getPrivateKey(password: String) {
        request<Event<String>> {
            onRequest {
                withContext(Dispatchers.IO) {
                    val success = preference.checkPassword(password)
                    if (success) {
                        Result.Success(Event(preference.PRI_KEY))
                    } else {
                        Result.Error(ApiException(IGNORE_ERROR))
                    }
                }
            }
            onSuccess {
                _privateKey.value = it
            }
            onFail {
                _errorPwd.value = Event(Unit)
            }
        }
    }

    fun checkPassword(password: String) {
        request<Event<Boolean>> {
            onRequest {
                withContext(Dispatchers.IO) {
                    val success = preference.checkPassword(password)
                    if (success) {
                        Result.Success(Event(true))
                    } else {
                        Result.Error(ApiException(IGNORE_ERROR))
                    }
                }
            }
            onSuccess {
                _verified.value = it
            }
            onFail {
                _errorPwd.value = Event(Unit)
            }
        }
    }

    fun changeDefaultPassword(password: String) {
        request<Event<String>> {
            onRequest {
                withContext(Dispatchers.IO) {
                    val words = preference.getMnemonicStringWithDefaultPwd()
                    if (words.isNullOrEmpty()) {
                        Result.Success(Event(""))
                    } else {
                        preference.saveMnemonicString(words, password)
                        manager.backupMnemonic(getAddress() ?: "", preference.PRI_KEY, words)
                        Result.Success(Event(words))
                    }
                }
            }
            onSuccess {
                _setEncPwd.value = it
            }
        }
    }

    fun refreshState() {
        _refresh.value = Unit
    }
}