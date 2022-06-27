package com.fzm.chat.vm

import androidx.core.util.PatternsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.repo.BackupRepository
import com.fzm.chat.core.repo.ContractRepository
import com.fzm.chat.core.session.LoginDelegate
import com.zjy.architecture.data.Result
import com.zjy.architecture.mvvm.LoadingViewModel
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2021/01/05
 * Description:
 */
class SearchUserViewModel(
    private val repository: ContractRepository,
    private val backup: BackupRepository,
    loginDelegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by loginDelegate {

    private val _searchResult by lazy { MutableLiveData<FriendUser>() }
    val searchResult: LiveData<FriendUser>
        get() = _searchResult

    fun searchUser(keywords: String) {
        launch {
            val result = when {
                keywords.matches(ChatConst.PHONE_PATTERN.toRegex()) || keywords.matches(PatternsCompat.EMAIL_ADDRESS.toRegex()) -> {
                    val result = backup.getAddress(keywords)
                    if (result.isSucceed()) {
                        repository.getFriendUser(result.data().address ?: "")
                    } else {
                        Result.Error(result.error())
                    }
                }
                else -> repository.getFriendUser(keywords)
            }
            _searchResult.value = result.dataOrNull()
        }
    }
}