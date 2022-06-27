package com.fzm.chat.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.dao.SearchHistoryDao
import com.fzm.chat.core.data.model.SearchHistory
import com.fzm.chat.core.data.bean.SearchResult
import com.fzm.chat.core.data.bean.SearchScope
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.core.repo.GroupRepository
import com.fzm.chat.core.repo.SearchRepository
import com.zjy.architecture.mvvm.LoadingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author zhengjy
 * @since 2021/04/14
 * Description:
 */
class SearchLocalViewModel(
    private val repository: SearchRepository,
    private val groupRepo: GroupRepository,
    val manager: ContactManager
) : LoadingViewModel() {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val historyDao: SearchHistoryDao
        get() = database.searchHistoryDao()

    /**
     * 当前搜索框内的关键词
     */
    private val _searchKey by lazy { MutableLiveData<String>() }
    val searchKey: LiveData<String>
        get() = _searchKey

    /**
     * 聚合搜索结果
     */
    private val _searchResult by lazy { MutableLiveData<SearchResult.Wrapper>() }
    val searchResult: LiveData<SearchResult.Wrapper>
        get() = _searchResult

    /**
     * 分类搜索结果
     */
    private val _searchScopeResult by lazy { MutableLiveData<SearchResult.Wrapper>() }
    val searchScopeResult: LiveData<SearchResult.Wrapper>
        get() = _searchScopeResult

    /**
     * 聊天记录搜索结果
     */
    private val _searchChatLogs by lazy { MutableLiveData<SearchResult>() }
    val searchChatLogs: LiveData<SearchResult>
        get() = _searchChatLogs

    private val historyEvent = Channel<String>(10)

    init {
        subscribeHistoryEvent()
    }

    @FlowPreview
    private fun subscribeHistoryEvent() = launch {
        historyEvent.receiveAsFlow()
            .debounce(1500L)
            .filter { it == searchKey.value }
            .collect {
                // 1.5秒后如果搜索关键词没有变，则保存搜索记录
                historyDao.insert(SearchHistory(it, System.currentTimeMillis()))
            }
    }

    /**
     * 提供外部手动修改搜索关键字，建议尽量不使用
     */
    internal fun initSearchKey(keywords: String?) {
        _searchKey.value = keywords
    }

    /**
     * 全局搜索关键字
     *
     * @param keywords      关键字
     */
    fun searchKeywords(keywords: String) = launch {
        _searchKey.value = keywords
        if (keywords.isEmpty()) {
            _searchResult.value = SearchResult.Wrapper(keywords, null)
            return@launch
        }
        historyEvent.send(keywords)
        _searchResult.value = repository.searchDataScoped(keywords, SearchScope.ALL)
    }

    /**
     * 从全局搜索结果中，过滤出指定范围的内容
     *
     * @param   scope 搜索范围
     * @see     [SearchScope]
     */
    fun getSearchResultByScope(scope: Int): List<SearchResult>? {
        return _searchResult.value?.list?.filter {
            scope == it.searchScope || scope == SearchScope.ALL
        }
    }

    /**
     * 在指定的范围内搜索关键字
     *
     * @param   keywords  关键字
     * @param   scope     搜索范围
     * @see     [SearchScope]
     */
    fun searchKeywordsScoped(keywords: String, scope: Int) = launch {
        _searchKey.value = keywords
        if (keywords.isEmpty()) {
            _searchScopeResult.value = SearchResult.Wrapper(keywords, null)
            return@launch
        }
        _searchScopeResult.value = withContext(Dispatchers.IO) {
            repository.searchDataScoped(keywords, scope)
        }
    }

    /**
     * 搜索指定对象的聊天记录
     *
     * @param keywords  关键字
     * @param target    需要查询聊天记录的对象
     */
    fun searchChatLogs(keywords: String, target: ChatTarget) = launch {
        _searchKey.value = keywords
        if (keywords.isEmpty()) {
            _searchChatLogs.value = null
            return@launch
        }
        _searchChatLogs.value = withContext(Dispatchers.IO) {
            val chatLogs = repository.searchChatLogs(keywords, target)
            SearchResult(SearchScope.CHAT_LOG, keywords, target.targetId, null, null, null, chatLogs)
        }
    }

    /**
     * 获取搜索历史记录列表
     *
     * @return [LiveData]
     */
    fun searchHistory() = historyDao.getSearchHistory()

    /**
     * 清除搜索历史记录
     */
    fun clearSearchHistory() = launch {
        historyDao.deleteAllHistory()
    }

    /**
     * 清除指定搜索历史记录
     *
     * @param key    搜索记录的关键词
     */
    fun deleteSearchHistory(key: String?) = launch {
        if (!key.isNullOrEmpty()) {
            historyDao.deleteHistory(key)
        }
    }

    suspend fun getGroupInfo(gid: Long): GroupInfo? {
        val groupInfo = database.groupDao().getGroupInfo(gid)
        return if (groupInfo != null) {
            groupRepo.getGroupInfo(groupInfo.server.address, gid, 0).dataOrNull()
        } else {
            null
        }
    }
}