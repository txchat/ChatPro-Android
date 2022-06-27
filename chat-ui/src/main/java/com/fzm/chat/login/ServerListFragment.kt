package com.fzm.chat.login

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.data.bean.ServerNode
import com.fzm.chat.core.logic.ServerManager
import com.fzm.chat.databinding.FragmentServerListBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.ChooseServerViewModel
import com.fzm.chat.widget.SelectBox
import com.zjy.architecture.ext.setVisible
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * @author zhengjy
 * @since 2021/01/20
 * Description:
 */
class ServerListFragment : BizFragment() {

    companion object {

        const val REQUEST_EDIT = 100

        /**
         * 单选聊天服务器
         */
        private const val CHAT_SERVER = 1

        private const val CONTRACT_SERVER = 2

        fun chatList(selectable: Boolean = false, showTips: Boolean = false): ServerListFragment {
            return ServerListFragment().apply {
                arguments = bundleOf(
                    "type" to CHAT_SERVER,
                    "selectable" to selectable,
                    "showTips" to showTips,
                )
            }
        }

        fun contractList(selectable: Boolean = false, showTips: Boolean = false): ServerListFragment {
            return ServerListFragment().apply {
                arguments = bundleOf(
                    "type" to CONTRACT_SERVER,
                    "selectable" to selectable,
                    "showTips" to showTips,
                )
            }
        }
    }

    private var type: Int = CHAT_SERVER

    /**
     * 是否只作为选择列表（点击条目，不会修改数据）
     */
    private var selectable: Boolean = false
    private var showTips: Boolean = false
    private val viewModel by lazy { requireActivity().getViewModel<ChooseServerViewModel>() }
    private lateinit var mAdapter: BaseQuickAdapter<ServerNode.Node, BaseViewHolder>

    private val binding by init<FragmentServerListBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        type = arguments?.getInt("type") ?: CHAT_SERVER
        selectable = arguments?.getBoolean("selectable") ?: false
        showTips = arguments?.getBoolean("showTips") ?: false
        binding.rlServerTips.setVisible(showTips)
        viewModel.loading.observe(this) { binding.refresh.isRefreshing = it.loading }
        binding.refresh.setColorSchemeColors(resources.getColor(R.color.biz_color_accent))
        binding.refresh.setOnRefreshListener {
            viewModel.fetchServerList()
        }
        binding.rvNode.layoutManager = LinearLayoutManager(requireContext())
        mAdapter = object : BaseQuickAdapter<ServerNode.Node, BaseViewHolder>(
            R.layout.item_server_node,
            mutableListOf()
        ) {
            override fun convert(holder: BaseViewHolder, item: ServerNode.Node) {
                val checkBox = holder.getView<SelectBox>(R.id.cb_select)
                checkBox.isChecked = item.selected
                checkBox.setOnClickListener {
                    mAdapter.data.forEach { it.selected = false }
                    item.selected = true
                    mAdapter.notifyDataSetChanged()
                    if (!selectable) {
                        if (type == CHAT_SERVER) {
                            ServerManager.changeLocalChatServer(item.address)
                        } else {
                            ServerManager.changeContractServer(item.address)
                        }
                    }
                }

                val name = holder.getView<TextView>(R.id.tv_name)
                val id = if (item.active) {
                    R.drawable.ic_server_connect
                } else {
                    R.drawable.ic_server_disconnect
                }
                val status = ResourcesCompat.getDrawable(resources, id, null)?.apply {
                    setBounds(0, 0, minimumWidth, minimumHeight)
                }
                name.setCompoundDrawables(null, null, status, null)
                name.text = item.name

                holder.setText(R.id.tv_address, item.address)
                holder.setVisible(R.id.iv_operation, item.custom)
                holder.getView<View>(R.id.iv_operation).setOnClickListener {
                    ARouter.getInstance().build(MainModule.EDIT_SERVER)
                        .withSerializable("node", item)
                        .withInt("type", type)
                        .navigation(requireActivity(), REQUEST_EDIT)
                }
                holder.itemView.setOnClickListener {
                    checkBox.performClick()
                }
            }
        }
        binding.rvNode.adapter = mAdapter
        mAdapter.setEmptyView(R.layout.layout_empty_common)
    }

    override fun initData() {
        if (type == CHAT_SERVER) {
            binding.tvServerTips.setText(R.string.chat_login_chat_server_tips)
            binding.tvTips.setText(R.string.chat_login_tips_choose_chat_server)
            viewModel.chatList.observe(this) {
                for (item in it) {
                    if (item.address == AppPreference.CHAT_URL) {
                        item.selected = true
                        break
                    }
                }
                mAdapter.setList(it)
            }
            viewModel.chatState.observe(this) {
                mAdapter.notifyItemChanged(it)
            }
        } else {
            binding.tvServerTips.setText(R.string.chat_login_contract_server_tips)
            binding.tvTips.setText(R.string.chat_login_tips_choose_contract_server)
            viewModel.contractList.observe(this) {
                for (item in it) {
                    if (item.address == AppPreference.CONTRACT_URL) {
                        item.selected = true
                        break
                    }
                }
                mAdapter.setList(it)
            }
            viewModel.contractState.observe(this) {
                mAdapter.notifyItemChanged(it)
            }
        }
    }

    /**
     * 获取选中的服务器
     */
    fun getSelectedServer(): ServerNode.Node? {
        return mAdapter.data.firstOrNull { it.selected }
    }


    override fun setEvent() {

    }
}