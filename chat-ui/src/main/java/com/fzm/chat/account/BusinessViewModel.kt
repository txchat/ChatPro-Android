package com.fzm.chat.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.biz.bean.ModuleState
import com.fzm.chat.biz.data.repo.BusinessRepository
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request

/**
 * @author zhengjy
 * @since 2021/08/17
 * Description:
 */
class BusinessViewModel(
    private val repository: BusinessRepository
) : LoadingViewModel() {

    private val _moduleState by lazy { MutableLiveData<List<ModuleState>>() }
    val moduleState: LiveData<List<ModuleState>>
        get() = _moduleState

    fun fetchModuleState() {
        request<List<ModuleState>> {
            onRequest {
                repository.fetchModuleState()
            }
            onSuccess {
                _moduleState.value = it
            }
        }
    }
}