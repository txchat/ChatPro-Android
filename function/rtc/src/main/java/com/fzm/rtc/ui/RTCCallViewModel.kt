package com.fzm.rtc.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.fzm.chat.core.data.local.ContactManager
import com.zjy.architecture.mvvm.LoadingViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2021/07/09
 * Description:
 */
class RTCCallViewModel(
    private val manager: ContactManager
) : LoadingViewModel() {

    private val _closePage by lazy { MutableLiveData<Unit>() }
    val closePage: LiveData<Unit>
        get() = _closePage

    private var closeJob: Job? = null
    private var statusJob: Job? = null

    fun closeActivity(delay: Long = 0) {
        closeJob = launch {
            delay(delay)
            _closePage.value = Unit
        }
    }

    fun cancelClose() {
        closeJob?.cancel()
    }

    fun startHideStatus(delay: Long) = liveData<Unit> {
        statusJob?.cancel()
        statusJob = launch(coroutineContext) {
            delay(delay)
            emit(Unit)
        }
        statusJob?.join()
    }

    fun getContactInfo(targetId: String) = liveData {
        emit(manager.getUserInfo(targetId))
    }
}