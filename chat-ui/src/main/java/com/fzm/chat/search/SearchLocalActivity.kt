package com.fzm.chat.search

import android.view.View
import androidx.fragment.app.commit
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.ChatSearchView
import com.fzm.chat.databinding.ActivitySearchLocalBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/04/14
 * Description:本地搜索页面
 */
@Route(path = MainModule.SEARCH_LOCAL)
class SearchLocalActivity : BizActivity() {

    private val viewModel by viewModel<SearchLocalViewModel>()
    private val historyFragment by lazy { SearchHistoryFragment() }
    private val resultFragment by lazy { SearchResultFragment() }

    internal val binding by init { ActivitySearchLocalBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        supportFragmentManager.commit {
            add(R.id.fl_container, historyFragment)
            add(R.id.fl_container, resultFragment)
            show(historyFragment)
            hide(resultFragment)
        }
    }

    override fun initData() {
        viewModel.searchKey.observe(this) {
            if (binding.svSearch.getText() != it) {
                binding.svSearch.setTextWithoutWatcher(it)
                if (it.isNotEmpty()) {
                    showResult()
                }
            }
        }
        viewModel.searchResult.observe(this) {
            if (it.list == null) {
                showHistory()
            }
        }
    }

    override fun setEvent() {
        binding.svSearch.postDelayed({ KeyboardUtils.showKeyboard(binding.svSearch.getFocusView()) }, 100)
        binding.svSearch.setOnSearchCancelListener(object : ChatSearchView.OnSearchCancelListener {
            override fun onSearchCancel() {
                onBackPressed()
            }
        })
        binding.svSearch.setOnTextChangeListener(object : ChatSearchView.OnTextChangeListener {
            override fun onTextChange(s: String) {
                viewModel.searchKeywords(s)
                if (s.isNotEmpty()) {
                    showResult()
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        binding.svSearch.getFocusView().clearFocus()
    }

    private fun showResult() {
        supportFragmentManager.commit {
            show(resultFragment)
            hide(historyFragment)
        }
    }

    private fun showHistory() {
        supportFragmentManager.commit {
            show(historyFragment)
            hide(resultFragment)
        }
    }
}