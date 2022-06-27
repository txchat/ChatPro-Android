package com.fzm.chat.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.fzm.chat.R
import com.fzm.chat.core.backup.LocalAccountManager
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.enum.CodeType
import com.fzm.chat.core.exception.AppException
import com.fzm.chat.core.repo.BackupRepository
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.utils.StringUtils
import com.zjy.architecture.data.IGNORE_ERROR
import com.zjy.architecture.data.Result
import com.zjy.architecture.exception.ApiException
import com.zjy.architecture.ext.handleException
import com.zjy.architecture.mvvm.GlobalErrorHandler
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author zhengjy
 * @since 2019/10/24
 * Description:
 */
class EncryptPasswordViewModel(
    private val repository: BackupRepository,
    private val manager: LocalAccountManager,
    private val loginDelegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by loginDelegate {

    /*-----------------------------------设置密聊密码-----------------------------------*/
    private val _firstError by lazy { MutableLiveData<Int>() }
    val firstError: LiveData<Int>
        get() = _firstError

    private val _secondError by lazy { MutableLiveData<Int>() }
    val secondError: LiveData<Int>
        get() = _secondError

    private val _mnemonicResult by lazy { MutableLiveData<Any>() }
    val mnemonicResult: LiveData<Any>
        get() = _mnemonicResult

    /*-----------------------------------更新密聊密码-----------------------------------*/
    private val _oldError by lazy { MutableLiveData<Int>() }
    val oldError: LiveData<Int>
        get() = _oldError

    private val _newError by lazy { MutableLiveData<Int>() }
    val newError: LiveData<Int>
        get() = _newError

    private val _newSecondError by lazy { MutableLiveData<Int>() }
    val newSecondError: LiveData<Int>
        get() = _newSecondError

    private val _changeResult by lazy { MutableLiveData<Any>() }
    val changeResult: LiveData<Any>
        get() = _changeResult

    private val _bindResult by lazy { MutableLiveData<String>() }
    val bindResult: LiveData<String>
        get() = _bindResult

    suspend fun sendCodeSuspend(account: String, loginType: Int, codeType: CodeType): Boolean {
        val result = try {
            if (loginType == ChatConst.PHONE) {
                repository.sendPhoneCodeV2(account, codeType)
            } else {
                repository.sendEmailCodeV2(account, codeType)
            }
        } catch (e: Exception) {
            Result.Error(handleException(e))
        }
        return if (result.isSucceed()) {
            true
        } else {
            GlobalErrorHandler.handler?.invoke(result.error())
            false
        }
    }

    fun bindAccount(account: String, code: String, bindType: Int) {
        request<Any> {
            onRequest {
                val enc = preference.MNEMONIC_WORDS
                if (bindType == ChatConst.PHONE) {
                    repository.bindPhoneV2(account, code, enc)
                } else {
                    repository.bindEmailV2(account, code, enc)
                }
            }
            onSuccess {
                loginDelegate.updateInfo {
                    if (bindType == ChatConst.PHONE) {
                        phone = account
                    } else {
                        email = account
                    }
                }
                _bindResult.value = account
            }
        }
    }

    fun findLocalAccount(account: String, accountType: Int) = liveData {
        emit(manager.findLocalAccount(account, accountType)?.address)
    }

    /**
     * 加密后的助记词，公钥上传至服务端
     *
     * @param first             第一次输入的密码
     * @param second            第二次确认密码
     */
    fun changeDefaultPassword(first: String, second: String) {
        if (!setCheck(first, second)) {
            return
        }
        request<String> {
            onRequest {
                withContext(Dispatchers.IO) {
                    val words = preference.getMnemonicStringWithDefaultPwd()
                    if (words.isNullOrEmpty()) {
                        Result.Error(AppException(ChatConst.Error.DECRYPT_ERROR))
                    } else {
                        val mnem = preference.saveMnemonicString(words, first)
                        manager.backupMnemonic(getAddress() ?: "", preference.PRI_KEY, words)
                        if (mnem.isNullOrEmpty()) {
                            Result.Error(AppException(ChatConst.Error.SAVE_WORDS_ERROR))
                        } else {
                            repository.updateMnemonic(mnem)
                            Result.Success(mnem)
                        }
                    }
                }
            }
            onSuccess {
                _mnemonicResult.value = it
            }
            onFail {
                _mnemonicResult.value = null
            }
        }
    }

    /**
     * 检查第一次输入密码是否符合要求
     */
    fun checkFirst(password: String): Boolean {
        return if (password.length !in 8..16) {
            _firstError.value = R.string.chat_tips_update_encrypt_pwd3
            false
        } else if (!StringUtils.isEncryptPassword(password)) {
            _firstError.value = R.string.chat_tips_update_encrypt_pwd4
            false
        } else {
            _firstError.value = 0
            true
        }
    }

    private fun setCheck(first: String, second: String): Boolean {
        val checkFirst = checkFirst(first)
        val checkSecond = if (first != second) {
            _secondError.value = R.string.chat_tips_update_encrypt_pwd6
            false
        } else {
            _secondError.value = 0
            true
        }
        return checkFirst && checkSecond
    }

    /**
     * 外部调用，检查输入的旧密码是否正确
     */
    fun checkOldPassword(password: String) {
        launch {
            checkOld(password)
        }
    }

    /**
     * 修改密聊密码，重新上传加密后的助记词和公钥
     */
    fun changePassword(oldPassword: String, password: String, passwordAgain: String) {
        request<Any> {
            onRequest {
                withContext(Dispatchers.IO) {
                    if (updateCheck(oldPassword, password, passwordAgain)) {
                        val words = preference.getMnemonicString(oldPassword)
                        if (words.isNullOrEmpty()) {
                            Result.Error(AppException(ChatConst.Error.DECRYPT_ERROR))
                        } else {
                            val mnem = preference.saveMnemonicString(words, password)
                            manager.backupMnemonic(getAddress() ?: "", preference.PRI_KEY, words)
                            if (mnem.isNullOrEmpty()) {
                                Result.Error(AppException(ChatConst.Error.SAVE_WORDS_ERROR))
                            } else {
                                repository.updateMnemonic(mnem)
                            }
                        }
                    } else {
                        Result.Error(ApiException(IGNORE_ERROR))
                    }
                }
            }
            onSuccess {
                _changeResult.value = it
            }
        }
    }

    /**
     * 检查输入的旧密码是否正确
     */
    private suspend fun checkOld(password: String): Boolean {
        val check = withContext(Dispatchers.IO) {
            preference.checkPassword(password)
        }
        if (!check) {
            _oldError.postValue(R.string.chat_tips_update_encrypt_pwd2)
        } else {
            _oldError.postValue(0)
        }
        return check
    }

    /**
     * 检查输入的新密码是否正确
     */
    fun checkNew(password: String): Boolean {
        return if (password.length !in 8..16) {
            _newError.postValue(R.string.chat_tips_update_encrypt_pwd3)
            false
        } else if (!StringUtils.isEncryptPassword(password)) {
            _newError.postValue(R.string.chat_tips_update_encrypt_pwd4)
            false
        } else {
            _newError.postValue(0)
            true
        }
    }

    private suspend fun updateCheck(
        oldPassword: String,
        password: String,
        passwordAgain: String
    ): Boolean {
        val check = checkOld(oldPassword) && checkNew(password)
        if (password != passwordAgain) {
            _newSecondError.postValue(R.string.chat_tips_update_encrypt_pwd6)
        } else {
            _newSecondError.postValue(0)
        }
        return check && password == passwordAgain
    }

    fun resetFirstError() {
        _firstError.value = 0
        _newError.value = 0
    }

    fun resetSecondError() {
        _secondError.value = 0
        _newSecondError.value = 0
    }
}