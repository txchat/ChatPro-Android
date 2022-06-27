package com.fzm.chat.search

import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.bus.BusEvent
import com.fzm.chat.biz.bus.LiveDataBus
import com.fzm.chat.biz.bus.event.ChangeTabEvent
import com.fzm.chat.biz.utils.defaultAvatar
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.bean.SearchResult
import com.fzm.chat.core.data.bean.SearchScope
import com.fzm.chat.databinding.FragmentSearchResultBinding
import com.fzm.chat.databinding.ItemLocalSearchResultBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.utils.StringUtils
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.setVisible
import com.zjy.architecture.ext.visible
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.getViewModel
import kotlin.math.min

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/04/14
 * Description:搜索结果列表页面
 */
class SearchResultFragment : BizFragment() {

    companion object {
        /**
         * 每种搜索类型最多显示条数
         */
        const val MAX_RESULT_NUM = 3
    }

    private val viewModel by lazy { requireActivity().getViewModel<SearchLocalViewModel>() }

    private val friends = mutableListOf<SearchResult>()
    private val groups = mutableListOf<SearchResult>()
    private val chatLogs = mutableListOf<SearchResult>()

    private val binding by init<FragmentSearchResultBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        handleSearchResult(viewModel.searchResult.value?.list)
    }

    override fun initData() {

    }

    override fun setEvent() {
        viewModel.searchResult.observe(this) {
            if (it.list == null) {
                handleSearchResult(null)
                return@observe
            }
            if (it.keywords == viewModel.searchKey.value) {
                handleSearchResult(it.list)
            }
        }
    }

    private fun handleSearchResult(list: List<SearchResult>?) {
        when {
            list == null -> {
                // 关键字为空，则不显示
                binding.llResult.gone()
                binding.llEmpty.root.gone()
                return
            }
            list.isEmpty() -> {
                // 搜索结果为空，则显示空页面
                binding.llResult.gone()
                binding.llEmpty.root.visible()
                return
            }
            else -> {
                binding.llResult.visible()
                binding.llEmpty.root.gone()
            }
        }
        val map = list.groupBy { it.searchScope }
        friends.clear()
        groups.clear()
        chatLogs.clear()
        map[SearchScope.FRIEND]?.take(MAX_RESULT_NUM + 1)?.let {
            friends.addAll(it)
        }
        map[SearchScope.GROUP]?.take(MAX_RESULT_NUM + 1)?.let {
            groups.addAll(it)
        }
        map[SearchScope.CHAT_LOG]?.take(MAX_RESULT_NUM + 1)?.let {
            chatLogs.addAll(it)
        }
        removeViews()
        showSearchResult()
    }

    private fun removeViews() {
        // 移除每一个分类中的搜索结果，保留第一个HeaderView
        for (i in 1 until binding.llResultFriends.childCount) {
            binding.llResultFriends.removeViewAt(1)
        }
        for (i in 1 until binding.llResultGroups.childCount) {
            binding.llResultGroups.removeViewAt(1)
        }
        for (i in 1 until binding.llResultLogs.childCount) {
            binding.llResultLogs.removeViewAt(1)
        }
    }

    private fun showSearchResult() {
        // 好友搜索结果
        if (friends.isEmpty()) {
            binding.llResultFriends.gone()
        } else {
            binding.llResultFriends.visible()
            binding.tvFriendMore.setVisible(friends.size > MAX_RESULT_NUM)
            binding.tvFriendMore.setOnClickListener {
                ARouter.getInstance().build(MainModule.SEARCH_LOCAL_SCOPED)
                    .withInt("scope", SearchScope.FRIEND)
                    .withString("keywords", viewModel.searchKey.value)
                    .withSerializable("result", viewModel.getSearchResultByScope(SearchScope.FRIEND) as Serializable)
                    .withOptionsCompat(
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                            requireActivity(),
                            (requireActivity() as SearchLocalActivity).binding.svSearch,
                            "searchView"
                        )
                    )
                    .navigation(requireActivity())
            }
            for (i in 0 until min(friends.size, MAX_RESULT_NUM)) {
                val bind = ItemLocalSearchResultBinding.inflate(layoutInflater)
                bindView(bind, friends[i])
                binding.llResultFriends.addView(bind.root)
            }
        }
        // 群聊搜索结果
        if (groups.isEmpty()) {
            binding.llResultGroups.gone()
        } else {
            binding.llResultGroups.visible()
            binding.tvGroupMore.setVisible(groups.size > MAX_RESULT_NUM)
            binding.tvGroupMore.setOnClickListener {
                ARouter.getInstance().build(MainModule.SEARCH_LOCAL_SCOPED)
                    .withInt("scope", SearchScope.GROUP)
                    .withString("keywords", viewModel.searchKey.value)
                    .withSerializable("result", viewModel.getSearchResultByScope(SearchScope.GROUP) as Serializable)
                    .withOptionsCompat(
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                            requireActivity(),
                            (requireActivity() as SearchLocalActivity).binding.svSearch,
                            "searchView"
                        )
                    )
                    .navigation(requireActivity())
            }
            for (i in 0 until min(groups.size, MAX_RESULT_NUM)) {
                val bind = ItemLocalSearchResultBinding.inflate(layoutInflater)
                bindView(bind, groups[i])
                binding.llResultGroups.addView(bind.root)
            }
        }
        // 聊天记录搜索结果
        if (chatLogs.isEmpty()) {
            binding.llResultLogs.gone()
        } else {
            binding.llResultLogs.visible()
            binding.tvLogMore.setVisible(chatLogs.size > MAX_RESULT_NUM)
            binding.tvLogMore.setOnClickListener {
                ARouter.getInstance().build(MainModule.SEARCH_LOCAL_SCOPED)
                    .withInt("scope", SearchScope.CHAT_LOG)
                    .withString("keywords", viewModel.searchKey.value)
                    .withSerializable("result", viewModel.getSearchResultByScope(SearchScope.CHAT_LOG) as Serializable)
                    .withOptionsCompat(
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                            requireActivity(),
                            (requireActivity() as SearchLocalActivity).binding.svSearch,
                            "searchView"
                        )
                    )
                    .navigation(requireActivity())
            }
            for (i in 0 until min(chatLogs.size, MAX_RESULT_NUM)) {
                val bind = ItemLocalSearchResultBinding.inflate(layoutInflater)
                bindView(bind, chatLogs[i])
                binding.llResultLogs.addView(bind.root)
            }
        }
    }

    private fun bindView(bind: ItemLocalSearchResultBinding, result: SearchResult) {
        when (result.searchScope) {
            SearchScope.FRIEND -> {
                Glide.with(this).load(result.avatar)
                    .apply(RequestOptions().placeholder(R.mipmap.default_avatar_round))
                    .into(bind.avatar)
                if (result.subTitle.isNullOrEmpty()) {
                    bind.desc.gone()
                } else {
                    bind.desc.visible()
                    bind.desc.highlightSearchText(getString(R.string.chat_tips_nickname_placeholder, result.subTitle), result.keywords)
                }
                bind.title.highlightSearchText(result.title, result.keywords)
                bind.root.setOnClickListener {
                    ARouter.getInstance().build(MainModule.CHAT)
                        .withInt("channelType", ChatConst.PRIVATE_CHANNEL)
                        .withString("address", result.targetId)
                        .navigation()
                    LiveDataBus.of(BusEvent::class.java).changeTab().setValue(ChangeTabEvent(0, 1))
                    activity?.finish()
                }
            }
            SearchScope.GROUP -> {
                Glide.with(this).load(result.avatar)
                    .apply(RequestOptions().placeholder(R.mipmap.default_avatar_room))
                    .into(bind.avatar)
                bind.desc.gone()
                bind.title.highlightSearchText(result.title, result.keywords)
                bind.root.setOnClickListener {
                    ARouter.getInstance().build(MainModule.CHAT)
                        .withInt("channelType", ChatConst.GROUP_CHANNEL)
                        .withString("name", result.title)
                        .withString("address", result.targetId)
                        .navigation()
                    LiveDataBus.of(BusEvent::class.java).changeTab().setValue(ChangeTabEvent(0, 0))
                    activity?.finish()
                }
            }
            SearchScope.CHAT_LOG -> {
                Glide.with(this).load(result.avatar)
                    .apply(RequestOptions().placeholder(defaultAvatar(result.chatLogs!![0].channelType)))
                    .into(bind.avatar)
                bind.title.highlightSearchText(result.title, result.keywords)
                bind.desc.visible()
                val message = result.chatLogs!![0]
                if (result.chatLogs!!.size == 1) {
                    bind.desc.text = message.matchSnippet
                    bind.time.visibility = View.VISIBLE
                    bind.time.text = StringUtils.timeFormat(requireContext(), result.chatLogs!![0].datetime)
                    bind.root.setOnClickListener {
                        KeyboardUtils.hideKeyboard(it)
                        ARouter.getInstance().build(MainModule.CHAT)
                            .withInt("channelType", message.channelType)
                            .withString("name", result.title)
                            .withString("address", result.targetId)
                            .withString("fromMsgId", message.msgId)
                            .navigation()
                    }
                } else {
                    bind.desc.text = getString(R.string.chat_tips_search_log_count, result.chatLogs!!.size)
                    bind.time.visibility = View.GONE
                    bind.root.setOnClickListener {
                        ARouter.getInstance().build(MainModule.SEARCH_LOCAL_SCOPED)
                            .withInt("scope", SearchScope.CHAT_LOG)
                            .withSerializable("chatTarget", ChatTarget(message.channelType, result.targetId!!))
                            .withString("keywords", viewModel.searchKey.value)
                            .withSerializable("chatLogs", result.chatLogs as Serializable)
                            .withOptionsCompat(
                                ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    requireActivity(),
                                    (requireActivity() as SearchLocalActivity).binding.svSearch,
                                    "searchView"
                                )
                            )
                            .navigation(requireActivity())
                    }
                }
            }
        }
    }
}