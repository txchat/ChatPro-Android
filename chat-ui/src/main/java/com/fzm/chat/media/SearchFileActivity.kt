package com.fzm.chat.media

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.bean.SimpleFileBean
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.ChatSearchView
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.ActivitySearchFileBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.widget.divider.RecyclerViewDivider
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.visible
import com.zjy.architecture.util.FileUtils
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/04/19
 * Description:
 */
@Route(path = MainModule.SEARCH_FILE)
class SearchFileActivity : BizActivity() {

    @JvmField
    @Autowired
    var chatTarget: ChatTarget? = null

    private lateinit var mAdapter: BaseQuickAdapter<ChatMessage, BaseViewHolder>

    private val viewModel by viewModel<SearchFileViewModel>()

    private val binding by init { ActivitySearchFileBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        binding.svSearch.getTransitionView().transitionName = "searchView"
        viewModel.searchKey.observe(this) {
            if (it.isEmpty()) {
                binding.llEmpty.root.gone()
                binding.swipeLayout.visible()
                mAdapter.setList(null)
            }
        }
        viewModel.searchResult.observe(this) {
            if (it.isEmpty()) {
                binding.llEmpty.root.visible()
                binding.swipeLayout.gone()
            } else {
                binding.llEmpty.root.gone()
                binding.swipeLayout.visible()
            }
            mAdapter.setList(it)
        }
    }

    override fun initData() {
        binding.rvSearchFile.layoutManager = LinearLayoutManager(this)
        binding.rvSearchFile.addItemDecoration(RecyclerViewDivider(this,
            ContextCompat.getColor(this, R.color.biz_color_divider), 0.5f, LinearLayoutManager.VERTICAL))
        mAdapter = object : BaseQuickAdapter<ChatMessage, BaseViewHolder>(R.layout.item_chat_file) {
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
                holder.setGone(R.id.cb_select, true)
                holder.setText(R.id.tv_file_date, Utils.formatDay(item.datetime))
                holder.setText(R.id.tv_file_size, Utils.byteToSize(item.msg.size))
                holder.setText(R.id.tv_uploader, item.sender?.getDisplayName())
            }
        }
        mAdapter.setOnItemClickListener { _, _, position ->
            val message = mAdapter.data[position]
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
        binding.rvSearchFile.adapter = mAdapter
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
                chatTarget?.apply {
                    viewModel.searchKeywords(s, this)
                }
            }
        })
    }
}