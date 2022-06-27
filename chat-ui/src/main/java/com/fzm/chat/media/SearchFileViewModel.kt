package com.fzm.chat.media

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.local.MessageRepository
import com.fzm.chat.core.data.model.ChatMessage
import com.zjy.architecture.mvvm.LoadingViewModel
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2021/04/19
 * Description:
 */
class SearchFileViewModel : LoadingViewModel() {

    /**
     * 当前搜索框内的关键词
     */
    private val _searchKey by lazy { MutableLiveData<String>() }
    val searchKey: LiveData<String>
        get() = _searchKey

    /**
     * 搜索结果
     */
    private val _searchResult by lazy { MutableLiveData<List<ChatMessage>>() }
    val searchResult: LiveData<List<ChatMessage>>
        get() = _searchResult

    /**
     * 搜索关键字
     *
     * @param keywords      关键字
     */
    fun searchKeywords(keywords: String, target: ChatTarget) = launch {
        _searchKey.value = keywords
        if (keywords.isEmpty()) {
            return@launch
        }
        _searchResult.value = MessageRepository.searchChatFiles(keywords, target)
    }
}