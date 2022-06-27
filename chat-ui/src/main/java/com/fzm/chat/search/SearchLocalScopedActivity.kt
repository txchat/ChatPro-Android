package com.fzm.chat.search

import android.view.View
import androidx.fragment.app.commit
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.ChatSearchView
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.bean.SearchResult
import com.fzm.chat.core.data.bean.SearchScope
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.databinding.ActivitySearchLocalScopedBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/04/15
 * Description:本地搜索分类页面（群聊、好友、聊天消息）
 */
@Route(path = MainModule.SEARCH_LOCAL_SCOPED)
class SearchLocalScopedActivity : BizActivity() {

    @JvmField
    @Autowired
    var scope: Int = SearchScope.FRIEND

    @JvmField
    @Autowired
    var keywords: String = ""

    @JvmField
    @Autowired
    var result: ArrayList<SearchResult>? = null

    @JvmField
    @Autowired
    var chatLogs: ArrayList<ChatMessage>? = null

    @JvmField
    @Autowired
    var chatTarget: ChatTarget? = null

    @JvmField
    @Autowired
    var popKeyboard: Boolean = false

    private val viewModel by viewModel<SearchLocalViewModel>()

    private val searchResultScopeFragment by lazy {
        SearchResultScopedFragment.create(scope, result)
    }
    private val searchLogDetailFragment by lazy {
        SearchChatLogFragment.create(chatTarget, chatLogs)
    }

    private val binding by init { ActivitySearchLocalScopedBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        when (scope) {
            SearchScope.FRIEND -> binding.svSearch.setHint(getString(R.string.chat_tips_search_hint1))
            SearchScope.GROUP -> binding.svSearch.setHint(getString(R.string.chat_tips_search_hint2))
            SearchScope.CHAT_LOG -> binding.svSearch.setHint(getString(R.string.chat_tips_search_hint3))
        }
        viewModel.initSearchKey(keywords)
        binding.svSearch.setText(keywords)
        if (popKeyboard) {
            binding.svSearch.postDelayed({ KeyboardUtils.showKeyboard(binding.svSearch.getFocusView()) }, 150)
        }
    }

    override fun initData() {
        if (chatTarget != null) {
            supportFragmentManager.commit {
                add(R.id.fl_container, searchLogDetailFragment)
            }
        } else {
            supportFragmentManager.commit {
                add(R.id.fl_container, searchResultScopeFragment)
            }
        }
    }

    override fun setEvent() {
        binding.ivBack.setOnClickListener { onBackPressed() }
        binding.svSearch.setOnSearchCancelListener(object : ChatSearchView.OnSearchCancelListener {
            override fun onSearchCancel() {
                onBackPressed()
            }
        })
        binding.svSearch.setOnTextChangeListener(object : ChatSearchView.OnTextChangeListener {
            override fun onTextChange(s: String) {
                if (chatTarget != null) {
                    viewModel.searchChatLogs(s, chatTarget!!)
                } else {
                    viewModel.searchKeywordsScoped(s, scope)
                }
            }
        })
    }
}