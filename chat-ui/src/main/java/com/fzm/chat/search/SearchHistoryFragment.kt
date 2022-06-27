package com.fzm.chat.search

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.core.data.model.SearchHistory
import com.fzm.chat.databinding.FragmentSearchHistoryBinding
import com.fzm.widget.divider.RecyclerViewDivider
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * @author zhengjy
 * @since 2021/04/14
 * Description:本地搜索记录页面
 */
class SearchHistoryFragment : BizFragment() {

    private val viewModel by lazy { requireActivity().getViewModel<SearchLocalViewModel>() }

    private lateinit var mAdapter: BaseQuickAdapter<SearchHistory, BaseViewHolder>

    private val binding by init<FragmentSearchHistoryBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.addItemDecoration(RecyclerViewDivider(requireContext(),
            ContextCompat.getColor(requireContext(), R.color.biz_color_divider), 0.5f, LinearLayoutManager.VERTICAL))
        mAdapter = object : BaseQuickAdapter<SearchHistory, BaseViewHolder>(R.layout.item_local_search_history, mutableListOf()) {
            override fun convert(holder: BaseViewHolder, item: SearchHistory) {
                holder.setText(R.id.tv_words, item.keywords)
                holder.getView<View>(R.id.iv_delete).setOnClickListener {
                    viewModel.deleteSearchHistory(item.keywords)
                }
            }
        }
        mAdapter.setOnItemClickListener { _, v, position ->
            viewModel.searchKeywords(mAdapter.data[position].keywords)
            KeyboardUtils.hideKeyboard(v)
        }

        binding.rvHistory.adapter = mAdapter
    }

    override fun initData() {
        viewModel.searchHistory().observe(this) {
            mAdapter.setList(it)
            if (mAdapter.data.size == 0) {
                binding.tvTitle.visibility = View.GONE
                binding.tvClearHistory.visibility = View.GONE
            } else {
                binding.tvTitle.visibility = View.VISIBLE
                binding.tvClearHistory.visibility = View.VISIBLE
            }
        }
    }

    override fun setEvent() {
        binding.tvClearHistory.setOnClickListener { viewModel.clearSearchHistory() }
    }
}