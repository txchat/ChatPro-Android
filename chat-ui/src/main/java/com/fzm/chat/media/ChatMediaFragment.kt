package com.fzm.chat.media

import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.po.getDisplayUrl
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.FragmentChatMediaBinding
import com.fzm.chat.ui.MediaGalleryActivity
import com.fzm.chat.vm.ChatFileViewModel
import com.fzm.chat.vm.ChatMediaEntity
import com.fzm.chat.widget.ChatRelativeLayout
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.windowManager
import dtalk.biz.Biz
import org.koin.androidx.viewmodel.ext.android.getViewModel
import java.util.ArrayList

/**
 * @author zhengjy
 * @since 2021/03/26
 * Description:
 */
class ChatMediaFragment : BizFragment() {

    companion object {
        fun create(target: String, channelType: Int): ChatMediaFragment {
            return ChatMediaFragment().apply {
                arguments = bundleOf(
                    "target" to target,
                    "channelType" to channelType
                )
            }
        }
    }
    private var target: String = ""
    private var channelType: Int = 0

    private lateinit var mAdapter: BaseSectionQuickAdapter<ChatMediaEntity, BaseViewHolder>
    private val viewModel by lazy { requireActivity().getViewModel<ChatFileViewModel>() }

    private val binding by init<FragmentChatMediaBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        target = arguments?.getString("target") ?: ""
        channelType = arguments?.getInt("channelType", 0) ?: 0
        val size = Point(360.dp, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireContext().windowManager?.currentWindowMetrics?.bounds?.apply {
                size.x = width()
                size.y = height()
            }
        } else {
            requireContext().windowManager?.defaultDisplay?.getRealSize(size)
        }
        val itemSize = (size.x - 10.dp) / 4
        val manager = GridLayoutManager(activity, 4)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (mAdapter.data[position].isHeader) 4 else 1
            }
        }
        binding.rvChatMedia.layoutManager = manager
        mAdapter = object : BaseSectionQuickAdapter<ChatMediaEntity, BaseViewHolder>(
            R.layout.item_chat_media_header,
            R.layout.item_chat_media,
            mutableListOf()
        ) {
            override fun convert(holder: BaseViewHolder, item: ChatMediaEntity) {
                val message = item.content as ChatMessage
                holder.setGone(R.id.cb_select, !viewModel.selectable)
                val image = holder.getView<ImageView>(R.id.iv_media)
                image.layoutParams = image.layoutParams.apply {
                    height = itemSize
                    width = itemSize
                }
                image.load(message.msg.getDisplayUrl(requireContext()), R.drawable.biz_image_placeholder_r0)
                val cbSelect = holder.getView<CheckBox>(R.id.cb_select)
                cbSelect.tag = message
                cbSelect.isChecked = message.isSelected
                cbSelect.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.tag == message) {
                        message.isSelected = isChecked
                        if (isChecked) {
                            viewModel.select(item.content)
                        } else {
                            viewModel.unSelect(item.content)
                        }
                    }
                }
                if (viewModel.selectable) {
                    holder.getView<ChatRelativeLayout>(R.id.cl_container).apply {
                        setOnClickListener {
                            cbSelect.performClick()
                        }
                        setSelectable(true)
                    }
                } else {
                    holder.getView<ChatRelativeLayout>(R.id.cl_container).apply {
                        setOnClickListener {
                            val list = mAdapter.data.filter { !it.isHeader }.map { it.content as ChatMessage }
                            startActivity(Intent(requireContext(), MediaGalleryActivity::class.java).apply {
                                putExtra("messages", ArrayList(list))
                                putExtra("index", list.indexOf(message))
                                putExtra("showGallery", false)
                            })
                        }
                        setSelectable(false)
                    }
                }
                if (message.msgType == Biz.MsgType.Video_VALUE) {
                    holder.setText(R.id.tv_duration, Utils.formatVideoDuration(message.msg.duration))
                    holder.setGone(R.id.iv_video, false)
                    holder.setGone(R.id.tv_duration, false)
                } else {
                    holder.setGone(R.id.iv_video, true)
                    holder.setGone(R.id.tv_duration, true)
                }
            }

            override fun convertHeader(helper: BaseViewHolder, item: ChatMediaEntity) {
                helper.setText(R.id.tv_date, item.content as String)
            }
        }
        binding.rvChatMedia.adapter = mAdapter
        mAdapter.setEmptyView(R.layout.layout_chat_media_empty)
    }

    override fun initData() {
        viewModel.refreshChatMedia(target, channelType)
        viewModel.chatMedia.observe(viewLifecycleOwner) {
            if (it.refresh) {
                mAdapter.setList(it.list)
                binding.swipeLayout.finishRefresh(true)
            } else {
                val entity = mAdapter.data.last { entity -> entity.isHeader }
                if (it.list.isNotEmpty()) {
                    val newDate = it.list[0].content as String
                    val lastDate = entity.content as String
                    if (lastDate == newDate) {
                        mAdapter.addData(it.list.subList(1, it.list.size))
                    } else {
                        mAdapter.addData(it.list)
                    }
                }
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
                if (!it.isHeader) {
                    (it.content as ChatMessage).isSelected = false
                }
            }
            mAdapter.notifyDataSetChanged()
        }
        viewModel.deleteResult.observe(viewLifecycleOwner) { messages ->
            messages.filter {
                it.msgType == Biz.MsgType.Image_VALUE ||
                    it.msgType == Biz.MsgType.Video_VALUE
            }.forEach { deleted ->
                val list = mAdapter.data
                val itr = list.iterator()
                while (itr.hasNext()) {
                    val msg = itr.next()
                    if (!msg.isHeader && (msg.content as ChatMessage).msgId == deleted.msgId) {
                        itr.remove()
                    }
                }
                mAdapter.setList(list)
            }
        }
    }

    override fun setEvent() {
        binding.swipeLayout.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                viewModel.refreshChatMedia(target, channelType)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                viewModel.loadMoreChatMedia(target, channelType)
            }
        })
    }
}