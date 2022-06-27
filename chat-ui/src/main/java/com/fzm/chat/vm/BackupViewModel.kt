package com.fzm.chat.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.fzm.chat.R
import com.fzm.chat.core.backup.LocalAccountManager
import com.fzm.chat.core.backup.LocalAccountType
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.BackupMnemonic
import com.fzm.chat.core.data.enum.CodeType
import com.fzm.chat.core.repo.BackupRepository
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.CipherUtils
import com.fzm.chat.core.utils.formatMnemonic
import com.zjy.architecture.data.Event
import com.zjy.architecture.data.Result
import com.zjy.architecture.ext.bytes2Hex
import com.zjy.architecture.ext.handleException
import com.zjy.architecture.mvvm.GlobalErrorHandler
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import walletapi.Walletapi

/**
 * @author zhengjy
 * @since 2021/01/12
 * Description:
 */
class BackupViewModel(
    private val repository: BackupRepository,
    private val manager: LocalAccountManager,
    delegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by delegate {

    private val _code by lazy { MutableLiveData<Int>() }
    val code: LiveData<Int>
        get() = _code

    private val _sendResult by lazy { MutableLiveData<Any>() }
    val sendResult: LiveData<Any>
        get() = _sendResult

    private val _fetchResult by lazy { MutableLiveData<BackupMnemonic>() }
    val fetchResult: LiveData<BackupMnemonic>
        get() = _fetchResult

    private val _checkError by lazy { MutableLiveData<Event<Int>>() }
    val checkError: LiveData<Event<Int>>
        get() = _checkError

    private val _bindResult by lazy { MutableLiveData<String>() }
    val bindResult: LiveData<String>
        get() = _bindResult

    private val _queryResult by lazy { MutableLiveData<Boolean>() }
    val queryResult: LiveData<Boolean>
        get() = _queryResult

    private val _error by lazy { MutableLiveData<String>() }
    val error: LiveData<String>
        get() = _error

    private val _needCreate by lazy { MutableLiveData<Triple<String, String, Int>>() }
    val needCreate: LiveData<Triple<String, String, Int>>
        get() = _needCreate

    private val _importResult by lazy { MutableLiveData<String>() }
    val importResult: LiveData<String>
        get() = _importResult

    private val _recoverResult by lazy { MutableLiveData<Event<Unit>>() }
    val recoverResult: LiveData<Event<Unit>>
        get() = _recoverResult

    private var countDownJob: Job? = null

    fun verifyCompanyUser(account: String, code: String, loginType: Int) {
        launch {
            flow {
                loading(true)
                emit(if (loginType == ChatConst.PHONE) {
                    repository.phoneQuery(account)
                } else {
                    repository.emailQuery(account)
                })
            }.map {
                if (it.isSucceed()) {
                    it.data()["exists"] as Boolean
                } else {
                    throw it.error()
                }
            }.catch {
                dismiss()
                _error.value = it.message
            }.collect {
                if (it) {
                    login(account, code, loginType)
                } else {
                    if (loginType == ChatConst.EMAIL) {
                        _error.value = "请用手机号注册，并绑定邮箱后尝试登录"
                    } else {
                        createMnemonic(account, code, loginType)
                    }
                }
                dismiss()
            }
        }
    }

    private fun createMnemonic(account: String, code: String, loginType: Int) {
        launch {
            try {
                loading(true)
                val mnemWithSpace = CipherUtils.createMnemonicString(1, 160).formatMnemonic()
                val hdWallet = withContext(Dispatchers.IO) {
                    CipherUtils.getHDWallet(Walletapi.TypeBtyString, mnemWithSpace)
                }
                if (hdWallet == null) {
                    dismiss()
                    _error.value = "私钥创建失败"
                    return@launch
                }
                loginSuspend(
                    hdWallet.newKeyPub(0).bytes2Hex(),
                    hdWallet.newKeyPriv(0).bytes2Hex(),
                    mnemWithSpace
                )
                _needCreate.value = Triple(account, code, loginType)
            } finally {
                dismiss()
            }
        }
    }

    fun sendCode(account: String, loginType: Int, codeType: CodeType) {
        request<Any> {
            onRequest {
                if (loginType == ChatConst.PHONE) {
                    repository.sendPhoneCodeV2(account, codeType)
                } else {
                    repository.sendEmailCodeV2(account, codeType)
                }
            }
            onSuccess {
                _sendResult.value = it
            }
        }
    }

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

    private fun login(account: String, code: String, loginType: Int) {
        request<BackupMnemonic> {
            onRequest {
                if (loginType == ChatConst.PHONE) {
                    repository.fetchBackupByPhone(account, code)
                } else {
                    repository.fetchBackupByEmail(account, code)
                }
            }
            onSuccess {
                _fetchResult.value = it
            }
        }
    }

    /**
     * 尝试用指定密码直接导入
     */
    suspend fun autoImportMnemonic(enc: String, password: String): Boolean {
        loading(true)
        val mnemonic = withContext(Dispatchers.IO) {
            CipherUtils.decryptMnemonicString(enc, password)
        }
        return if (mnemonic == null) {
            dismiss()
            false
        } else {
            if (reImportMnemonic(mnemonic, password)) {
                dismiss()
                _checkError.value = Event(0)
                true
            } else {
                dismiss()
                false
            }
        }
    }

    /**
     * 使用用户输入密码导入
     */
    fun decryptAndReImportMnemonic(enc: String, password: String) {
        launch {
            loading(true)
            val mnemonic = withContext(Dispatchers.IO) {
                CipherUtils.decryptMnemonicString(enc, password)
            }
            if (mnemonic == null) {
                _checkError.value = Event(R.string.chat_tips_chat_pwd_error)
                dismiss()
            } else {
                if (reImportMnemonic(mnemonic, password)) {
                    dismiss()
                    _checkError.value = Event(0)
                } else {
                    dismiss()
                }
            }
        }
    }

    /**
     * 在忘记密码时，先尝试自动恢复助记词
     */
    suspend fun tryRecoverMnemonic(account: String, @LocalAccountType type: Int): Boolean {
        loading(true)
        val mnemonic = manager.tryRecoverMnemonic(account, type)
        if (!mnemonic.isNullOrEmpty()) {
            return if (reImportMnemonic(mnemonic, "", true)) {
                dismiss()
                _recoverResult.value = Event(Unit)
                true
            } else {
                dismiss()
                false
            }
        }
        dismiss()
        return false
    }

    private suspend fun reImportMnemonic(mnemonic: String, password: String, ignoreError: Boolean = false): Boolean {
        val mnemWithSpace = mnemonic.formatMnemonic()
        val hdWallet = withContext(Dispatchers.IO) {
            CipherUtils.getHDWallet(Walletapi.TypeBtyString, mnemWithSpace)
        }
        if (hdWallet == null) {
            if (!ignoreError) {
                _checkError.value = Event(R.string.chat_login_mnem_import_fail)
            }
            return false
        }
        loginSuspend(hdWallet.newKeyPub(0).bytes2Hex(), hdWallet.newKeyPriv(0).bytes2Hex(), mnemWithSpace, password)
        return true
    }

    /**
     * 快速创建并导入助记词登录，用于在忘记密码的情况下
     */
    fun autoCreateAndImportMnemonic(account: String) = launch {
        loading(true)
        val mnemonic = CipherUtils.createMnemonicString(1, 160).formatMnemonic()
        val hdWallet = withContext(Dispatchers.IO) {
            CipherUtils.getHDWallet(Walletapi.TypeBtyString, mnemonic)
        }
        if (hdWallet == null) {
            dismiss()
            _importResult.value = null
            return@launch
        }
        loginSuspend(
            hdWallet.newKeyPub(0).bytes2Hex(),
            hdWallet.newKeyPriv(0).bytes2Hex(),
            mnemonic
        )
        dismiss()
        _importResult.value = account
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
                updateInfo {
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

    fun bindAutoCreateAccount(account: String, code: String, bindType: Int) {
        request<Any> {
            onRequest {
                val enc = preference.MNEMONIC_WORDS
                if (bindType == ChatConst.PHONE) {
                    repository.bindPhone(account, code, enc)
                } else {
                    repository.bindEmail(account, code, enc)
                }
            }
            onSuccess {
                updateInfo {
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

    fun checkBind(account: String, bindType: Int) {
        request<Map<String, *>> {
            onRequest {
                if (bindType == ChatConst.PHONE) {
                    repository.phoneQuery(account)
                } else {
                    repository.emailQuery(account)
                }
            }
            onSuccess {
                _queryResult.value = it["exists"] as Boolean
            }
        }
    }

    fun findLocalAccount(account: String, accountType: Int) = liveData {
        emit(manager.findLocalAccount(account, accountType)?.address)
    }

    fun startCount() {
        countDownJob?.cancel()
        countDownJob = launch {
            repeat(60) {
                _code.value = 60 - (it + 1)
                delay(1000)
            }
        }
    }

    fun cancelCount() {
        countDownJob?.cancel()
        _code.value = 0
    }

}