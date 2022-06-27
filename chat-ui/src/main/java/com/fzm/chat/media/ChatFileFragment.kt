package com.fzm.chat.media

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.bean.SimpleFileBean
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.FragmentChatFileBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.ChatFileViewModel
import com.fzm.chat.widget.ChatRelativeLayout
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.zjy.architecture.util.FileUtils
import dtalk.biz.Biz
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * @author zhengjy
 * @since 2021/03/26
 * Description:
 */
class ChatFileFragment : BizFragment() {

    companion object {
        fun create(target: String?, channelType: Int): ChatFileFragment {
            return ChatFileFragment().apply {
                arguments = bundleOf(
                    "target" to target,
                    "channelType" to channelType
                )
            }
        }
    }

    private var target: String = ""
    private var channelType: Int = 0

    private lateinit var mAdapter: BaseQuickAdapter<ChatMessage, BaseViewHolder>
    private val viewModel by lazy { requireActivity().getViewModel<ChatFileViewModel>() }

    private val binding by init<FragmentChatFileBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        target = arguments?.getString("target") ?: ""
        channelType = arguments?.getInt("channelType", 0) ?: 0
        binding.rvChatFile.layoutManager = LinearLayoutManager(requireContext())
        mAdapter = object : BaseQuickAdapter<ChatMessage, BaseViewHolder>(R.layout.item_chat_file, mutableListOf()) {
            override fun convert(holder: BaseViewHolder, item: ChatMessage) {
                when (FileUtils.getExtension(item.msg.fileName)) {
                    "doc", "docx" -> holder.setImageResource(R.id.iv_file_type, R.mipmap.icon_file_doc)
                    "pdf" -> holder.setImageResource(R.id.iv_file_type, R.mipmap.icon_file_pdf)
                    "xls", "xlsx" -> holder.setImageResource(R.id.iv_file_type, R.mipmap.icon_file_xls)
                    "mp3", "wma", "wav", "ogg" -> holder.setImageResource(R.id.iv_file_type, R.mipmap.icon_file_music)
                    "mp4", "avi", "rmvb", "flv",
                    "f4v", "mpg", "mkv", "mov" -> holder.setImageResource(R.id.iv_file_type, R.mipmap.icon_file_video)
                    else -> holder.setImageResource(R.id.iv_file_type, R.mipmap.icon_file_other)
                }
                holder.setText(R.id.tv_file_name, item.msg.fileName)
                holder.setGone(R.id.cb_select, !viewModel.selectable)
                holder.setText(R.id.tv_file_date, Utils.formatDay(item.datetime))
                holder.setText(R.id.tv_file_size, Utils.byteToSize(item.msg.size))
                val cbSelect = holder.getView<CheckBox>(R.id.cb_select)
                cbSelect.tag = item
                cbSelect.isChecked = item.isSelected
                cbSelect.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.tag == item) {
                        item.isSelected = isChecked
                        if (isChecked) {
                            viewModel.select(item)
                        } else {
                            viewModel.unSelect(item)
                        }
                    }
                }
                holder.setText(R.id.tv_uploader, item.sender?.getDisplayName())
                if (viewModel.selectable) {
                    holder.getView<ChatRelativeLayout>(R.id.cl_container).apply {
                        setOnClickListener {
                            cbSelect.performClick()
                        }
                        setSelectable(true)
                    }
                } else {
                    holder.getView<ChatRelativeLayout>(R.id.cl_container).apply {
                        setOnClickListener { download(item) }
                        setSelectable(false)
                    }
                }
            }
        }
        binding.rvChatFile.adapter = mAdapter
        mAdapter.setEmptyView(R.layout.layout_chat_file_empty)
        
    }

    override fun initData() {
        viewModel.refreshChatFile(target, channelType)
        viewModel.chatFiles.observe(viewLifecycleOwner) {
            if (it.refresh) {
                mAdapter.setList(it.list)
                binding.swipeLayout.finishRefresh(true)
            } else {
                mAdapter.addData(it.list)
                binding.swipeLayout.finishRefresh(true)
            }
            if (it.noMore) {
                binding.swipeLayout.finishLoadMoreWithNoMoreData()
            } else {
                binding.swipeLayout.finishLoadMore(true)
            }
        }
        viewModel.chooseMode.observe(viewLifecycleOwner) {
            mAdapter.data.forEach {
                it.isSelected = false
            }
            mAdapter.notifyDataSetChanged()
        }
        viewModel.deleteResult.observe(viewLifecycleOwner) { messages ->
            messages.filter { it.msgType == Biz.MsgType.File_VALUE }.forEach { mAdapter.remove(it) }
        }
    }

    override fun setEvent() {
        binding.llSearch.setOnClickListener {
            ARouter.getInstance().build(MainModule.SEARCH_FILE)
                .withSerializable("chatTarget", ChatTarget(channelType, target))
                .withOptionsCompat(
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        requireActivity(),
                        it,
                        "searchView")
                )
                .navigation(requireActivity())
        }
        binding.swipeLayout.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                viewModel.refreshChatFile(target, channelType)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                viewModel.loadMoreChatFile(target, channelType)
            }
        })
    }

    private fun download(message: ChatMessage) {
        ARouter.getInstance().build(MainModule.FILE_DETAIL)
            .withSerializable(
                "file", SimpleFileBean(
                    message.msg.fileName,
                    message.msg.size,
                    message.msg.md5,
                    message.msg.localUrl)
            )
            .withSerializable("message", message)
            .navigation()
    }
}