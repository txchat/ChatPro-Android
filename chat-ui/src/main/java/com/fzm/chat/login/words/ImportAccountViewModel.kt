package com.fzm.chat.login.words

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.CipherUtils
import com.fzm.chat.core.utils.formatMnemonic
import com.zjy.architecture.ext.bytes2Hex
import com.zjy.architecture.mvvm.LoadingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import walletapi.Walletapi

/**
 * @author zhengjy
 * @since 2020/12/11
 * Description:
 */
class ImportAccountViewModel(
    private val delegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by delegate {

    private val _importResult by lazy { MutableLiveData<Unit>() }
    val importResult: LiveData<Unit>
        get() = _importResult

    fun importMnemonic(mnemonic: String) = launch {
        loading(true)
        val mnemWithSpace = mnemonic.formatMnemonic()
        val hdWallet = withContext(Dispatchers.IO) {
            CipherUtils.getHDWallet(Walletapi.TypeBtyString, mnemWithSpace)
        }
        if (hdWallet == null) {
            dismiss()
            _importResult.value = null
            return@launch
        }
        loginSuspend(hdWallet.newKeyPub(0).bytes2Hex(), hdWallet.newKeyPriv(0).bytes2Hex(), mnemWithSpace)
        dismiss()
        _importResult.value = Unit
    }
}