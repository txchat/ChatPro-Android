package com.fzm.chat.search

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.utils.defaultAvatar
import com.fzm.chat.biz.widget.HighlightTextView
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.bean.SearchResult
import com.fzm.chat.core.data.bean.SearchScope
import com.fzm.chat.databinding.FragmentSearchResultScopedBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.utils.StringUtils
import com.fzm.widget.divider.RecyclerViewDivider
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.visible
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.getViewModel
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/04/15
 * Description:分类搜索结果列表页面
 */
class SearchResultScopedFragment : BizFragment() {

    companion object {
        fun create(scope: Int, list: ArrayList<SearchResult>?): SearchResultScopedFragment {
            return SearchResultScopedFragment().apply {
                arguments = Bundle().apply {
                    putInt("scope", scope)
                    putSerializable("initResult", list)
                }
            }
        }
    }

    @JvmField
    @Autowired
    var scope: Int = 0
    @JvmField
    @Autowired
    var initResult: List<SearchResult>? = null
    private val viewModel by lazy { requireActivity().getViewModel<SearchLocalViewModel>() }

    private lateinit var mAdapter: BaseQuickAdapter<SearchResult, BaseViewHolder>

    private val binding by init<FragmentSearchResultScopedBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        ARouter.getInstance().inject(this)
        viewModel.searchScopeResult.observe(this) {
            if (it == null) {
                updateSearchResult(null)
                return@observe
            }
            if (it.keywords == viewModel.searchKey.value) {
                updateSearchResult(it.list)
            }
        }
        when (scope) {
            SearchScope.FRIEND -> binding.tvScope.setText(R.string.chat_tips_search_type1)
            SearchScope.GROUP -> binding.tvScope.setText(R.string.chat_tips_search_type2)
            SearchScope.CHAT_LOG -> binding.tvScope.setText(R.string.chat_tips_search_type3)
        }
    }

    override fun initData() {
        binding.rvResultScoped.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResultScoped.addItemDecoration(RecyclerViewDivider(requireContext(),
            ContextCompat.getColor(requireContext(), R.color.biz_color_divider), 0.5f, LinearLayoutManager.VERTICAL))
        mAdapter = object : BaseQuickAdapter<SearchResult, BaseViewHolder>(R.layout.item_local_search_result_scoped) {
            override fun convert(holder: BaseViewHolder, item: SearchResult) {
                when (scope) {
                    SearchScope.FRIEND -> {
                        Glide.with(requireContext()).load(item.avatar)
                            .apply(RequestOptions().placeholder(R.mipmap.default_avatar_round))
                            .into(holder.getView(R.id.avatar))
                        holder.setVisible(R.id.desc, !item.subTitle.isNullOrEmpty())
                        if (!item.subTitle.isNullOrEmpty()) {
                            holder.getView<HighlightTextView>(R.id.desc).highlightSearchText(
                                getString(R.string.chat_tips_nickname_placeholder, item.subTitle), item.keywords)
                        }
                        holder.getView<HighlightTextView>(R.id.title).highlightSearchText(
                            item.title, item.keywords)
                    }
                    SearchScope.GROUP -> {
                        Glide.with(requireContext()).load(item.avatar)
                            .apply(RequestOptions().placeholder(R.mipmap.default_avatar_room))
                            .into(holder.getView(R.id.avatar))
                        holder.setVisible(R.id.desc, false)
                        holder.getView<HighlightTextView>(R.id.title).highlightSearchText(
                            item.title, item.keywords)
                    }
                    SearchScope.CHAT_LOG -> {
                        Glide.with(requireContext()).load(item.avatar)
                            .apply(RequestOptions().placeholder(defaultAvatar(item.chatLogs!![0].channelType)))
                            .into(holder.getView(R.id.avatar))
                        holder.getView<HighlightTextView>(R.id.title).highlightSearchText(item.title, item.keywords)
                        holder.setVisible(R.id.desc, true)
                        val message = item.chatLogs!![0]
                        if (item.chatLogs!!.size == 1) {
                            holder.setText(R.id.desc, message.matchSnippet)
                            holder.setVisible(R.id.time, true)
                            holder.setText(R.id.time, StringUtils.timeFormat(requireContext(), message.datetime))
                        } else {
                            holder.setText(R.id.desc, getString(R.string.chat_tips_search_log_count, item.chatLogs!!.size))
                            holder.setVisible(R.id.time, false)
                        }
                    }
                }
            }
        }
        mAdapter.setOnItemClickListener { _, view, position ->
            val result = mAdapter.data
            when (scope) {
                SearchScope.FRIEND -> {
                    ARouter.getInstance().build(MainModule.CONTACT_INFO)
                        .withString("address", result[position].targetId)
                        .navigation()
                }
                SearchScope.GROUP -> {
                    ARouter.getInstance().build(MainModule.GROUP_INFO)
                        .withLong("groupId", result[position].targetId!!.toLong())
                        .navigation()
                }
                SearchScope.CHAT_LOG -> {
                    val message = result[position].chatLogs!![0]
                    if (result[position].chatLogs!!.size == 1) {
                        KeyboardUtils.hideKeyboard(view)
                        ARouter.getInstance().build(MainModule.CHAT)
                            .withInt("channelType", message.channelType)
                            .withString("name", result[position].title)
                            .withString("address", result[position].targetId)
                            .withString("fromMsgId", message.msgId)
                            .navigation()
                    } else {
                        ARouter.getInstance().build(MainModule.SEARCH_LOCAL_SCOPED)
                            .withInt("scope", SearchScope.CHAT_LOG)
                            .withSerializable("chatTarget", ChatTarget(message.channelType, result[position].targetId!!))
                            .withString("keywords", viewModel.searchKey.value)
                            .withSerializable("chatLogs", result[position].chatLogs as Serializable)
                            .navigation()
                    }
                }
            }
        }
        binding.rvResultScoped.adapter = mAdapter
        updateSearchResult(initResult)
    }

    override fun setEvent() {

    }

    private fun updateSearchResult(list: List<SearchResult>?) {
        when {
            list == null -> {
                // 表示未输入关键词
                binding.llResult.visible()
                binding.llEmpty.root.gone()
                mAdapter.setList(null)
            }
            list.isEmpty() -> {
                binding.llResult.gone()
                binding.llEmpty.root.visible()
                mAdapter.setList(null)
            }
            else -> {
                binding.llResult.visible()
                binding.llEmpty.root.gone()
                mAdapter.setList(list)
            }
        }
    }
}