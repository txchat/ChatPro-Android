package com.fzm.chat.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.core.backup.LocalAccountManager
import com.fzm.chat.core.backup.LocalAccountType
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.enum.CodeType
import com.fzm.chat.core.data.po.LocalAccount
import com.fzm.chat.core.repo.BackupRepository
import com.fzm.chat.core.session.LoginDelegate
import com.zjy.architecture.data.Result
import com.zjy.architecture.ext.handleException
import com.zjy.architecture.mvvm.GlobalErrorHandler
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request

/**
 * @author zhengjy
 * @since 2021/12/01
 * Description:
 */
class ImportLocalAccountViewModel(
    private val manager: LocalAccountManager,
    private val repository: BackupRepository,
    private val delegate: LoginDelegate
) : LoadingViewModel() {

    private val _accountList by lazy(LazyThreadSafetyMode.NONE) { MutableLiveData<List<LocalAccount>>() }
    val accountList: LiveData<List<LocalAccount>>
        get() = _accountList

    private val _importResult by lazy(LazyThreadSafetyMode.NONE) { MutableLiveData<Unit>() }
    val importResult: LiveData<Unit>
        get() = _importResult

    private val _verifyResult by lazy(LazyThreadSafetyMode.NONE) { MutableLiveData<Triple<String, Int, String>>() }
    val verifyResult: LiveData<Triple<String, Int, String>>
        get() = _verifyResult

    fun getAccountList() {
        request<List<LocalAccount>> {
            onRequest {
                Result.Success(
                    manager.getLocalAccountList { it.address != delegate.getAddress() }
                )
            }
            onSuccess {
                _accountList.value = it
            }
        }
    }

    suspend fun sendCodeSuspend(account: String, accountType: Int, codeType: CodeType): Boolean {
        val result = try {
            if (accountType == ChatConst.PHONE) {
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

    fun verifyPhoneExport(phone: String, code: String, address: String, hash: String) {
        request<Triple<String, Int, String>> {
            onRequest {
                val result = repository.phoneExport(phone, code, address)
                if (result.isSucceed()) {
                    Result.Success(Triple(phone, LocalAccountType.BACKUP_PHONE, hash))
                } else {
                    Result.Error(result.error())
                }
            }
            onSuccess {
                _verifyResult.value = it
            }
        }
    }

    fun verifyEmailExport(email: String, code: String, address: String, hash: String) {
        request<Triple<String, Int, String>> {
            onRequest {
                val result = repository.emailExport(email, code, address)
                if (result.isSucceed()) {
                    Result.Success(Triple(email, LocalAccountType.BACKUP_EMAIL, hash))
                } else {
                    Result.Error(result.error())
                }
            }
            onSuccess {
                _verifyResult.value = it
            }
        }
    }

    fun importAccount(account: String, @LocalAccountType type: Int, hash: String) {
        request<Unit> {
            onRequest {
                manager.importAccount(account, type, hash)
            }
            onSuccess {
                _importResult.value = it
            }
        }
    }
}