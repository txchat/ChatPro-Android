package com.fzm.chat.search

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.widget.HighlightTextView
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.databinding.FragmentSearchChatLogBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.utils.StringUtils
import com.fzm.widget.divider.RecyclerViewDivider
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.visible
import com.zjy.architecture.util.KeyboardUtils
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.getViewModel
import kotlin.properties.Delegates

/**
 * @author zhengjy
 * @since 2021/04/15
 * Description:
 */
class SearchChatLogFragment : BizFragment() {

    companion object {
        fun create(target: ChatTarget?, chatLogs: ArrayList<ChatMessage>?): SearchChatLogFragment {
            return SearchChatLogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("target", target)
                    putSerializable("chatLogs", chatLogs)
                }
            }
        }
    }

    var channelType by Delegates.notNull<Int>()
    var targetId: String = ""
    var initResult: List<ChatMessage>? = null

    private val viewModel by lazy { requireActivity().getViewModel<SearchLocalViewModel>() }

    private lateinit var mAdapter: BaseQuickAdapter<ChatMessage, BaseViewHolder>

    private val binding by init<FragmentSearchChatLogBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        initResult = arguments?.getSerializable("chatLogs") as ArrayList<ChatMessage>?
        val target = arguments?.getSerializable("target") as ChatTarget
        channelType = target.channelType
        targetId = target.targetId
        viewModel.searchChatLogs.observe(this) {
            if (it == null) {
                updateSearchResult(null)
                return@observe
            }
            if (it.keywords == viewModel.searchKey.value) {
                updateSearchResult(it.chatLogs)
            }
        }

        if (channelType == ChatConst.GROUP_CHANNEL) {
            lifecycleScope.launch {
                val info = viewModel.getGroupInfo(targetId.toLong())
                binding.tvScope.text = getString(R.string.chat_tips_search_log_target, info?.getDisplayName())
            }
        } else {
            lifecycleScope.launch {
                val info = viewModel.manager.getUserInfo(targetId, true)
                binding.tvScope.text = getString(R.string.chat_tips_search_log_target, info.getDisplayName())
            }
        }
    }

    override fun initData() {
        binding.rvResultLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResultLogs.addItemDecoration(RecyclerViewDivider(requireContext(),
            ContextCompat.getColor(requireContext(), R.color.biz_color_divider), 0.5f, LinearLayoutManager.VERTICAL))
        mAdapter = object : BaseQuickAdapter<ChatMessage, BaseViewHolder>(R.layout.item_local_search_result_scoped) {
            override fun convert(holder: BaseViewHolder, item: ChatMessage) {
                holder.setText(R.id.desc, item.matchSnippet)
                holder.setVisible(R.id.time, true)
                holder.setText(R.id.time, StringUtils.timeFormat(requireContext(), item.datetime))

                val tagView = holder.getView<View>(R.id.title)
                tagView.tag = item.msgId
                if (channelType == ChatConst.GROUP_CHANNEL) {
                    lifecycleScope.launch {
                        val user = viewModel.manager.getGroupUserInfoFast(targetId, item.from)
                        if (tagView.tag == item.msgId) {
                            Glide.with(requireContext()).load(user.getDisplayImage())
                                .apply(RequestOptions().placeholder(R.mipmap.default_avatar_round))
                                .into(holder.getView(R.id.avatar))
                            holder.getView<HighlightTextView>(R.id.title).highlightSearchText(
                                user.getDisplayName(), viewModel.searchKey.value)
                        }
                    }
                } else {
                    lifecycleScope.launch {
                        val info = viewModel.manager.getUserInfo(item.from)
                        if (tagView.tag == item.msgId) {
                            Glide.with(requireContext()).load(info.getDisplayImage())
                                .apply(RequestOptions().placeholder(R.mipmap.default_avatar_round))
                                .into(holder.getView(R.id.avatar))
                            holder.getView<HighlightTextView>(R.id.title).highlightSearchText(
                                info.getDisplayName(), viewModel.searchKey.value)
                        }
                    }
                }
            }
        }
        mAdapter.setOnItemClickListener { _, view, position ->
            KeyboardUtils.hideKeyboard(view)
            ARouter.getInstance().build(MainModule.CHAT)
                .withInt("channelType", channelType)
                .withString("address", targetId)
                .withString("fromMsgId", mAdapter.data[position].msgId)
                .navigation()
        }
        binding.rvResultLogs.adapter = mAdapter
        updateSearchResult(initResult)
    }

    override fun setEvent() {

    }

    private fun updateSearchResult(list: List<ChatMessage>?) {
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