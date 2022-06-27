package com.fzm.chat.contact

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.core.data.bean.ServerGroupInfo
import com.fzm.arch.connection.utils.urlKey
import com.fzm.chat.databinding.FragmentChatServerListBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.ServerManageViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * @author zhengjy
 * @since 2021/02/23
 * Description:
 */
class ChatServerListFragment : BizFragment() {

    companion object {

        fun create(selectable: Boolean): ChatServerListFragment {
            return ChatServerListFragment().apply {
                arguments = bundleOf("selectable" to selectable)
            }
        }
    }

    /**
     * 是否是选择模式
     */
    @JvmField
    @Autowired
    var selectable = false

    private val viewModel by lazy { requireActivity().getViewModel<ServerManageViewModel>() }
    private var onSelectListener: ((ServerGroupInfo) -> Unit)? = null

    private lateinit var mAdapter: BaseQuickAdapter<ServerGroupInfo, BaseViewHolder>

    private val binding by init<FragmentChatServerListBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        selectable = arguments?.getBoolean("selectable")?:false
        binding.refresh.setColorSchemeColors(resources.getColor(R.color.biz_color_accent))
        binding.refresh.isEnabled = !selectable
        binding.refresh.setOnRefreshListener {
            viewModel.getServerGroupList()
        }
        binding.rvServerGroup.layoutManager = LinearLayoutManager(requireContext())
        mAdapter = object : BaseQuickAdapter<ServerGroupInfo, BaseViewHolder>(R.layout.item_server_group_with_status, mutableListOf()) {
            override fun convert(holder: BaseViewHolder, item: ServerGroupInfo) {
                val index = mAdapter.data.indexOf(item)
                holder.itemView.setOnClickListener {
                    if (selectable) {
                        onSelectListener?.invoke(item)
                    } else {
                        goEditPage(item, index == 0)
                    }
                }
                val name = holder.getView<TextView>(R.id.tv_name)

                val socket = viewModel.connection.getChatSocket(item.value)
                val id = if (socket?.isAlive == true) {
                    item.state = socket.state.value ?: 0
                    R.drawable.ic_server_connect
                } else {
                    item.state = socket?.state?.value ?: 0
                    R.drawable.ic_server_disconnect
                }

                val status = ResourcesCompat.getDrawable(resources, id, null)?.apply {
                    setBounds(0, 0, minimumWidth, minimumHeight)
                }
                name.setCompoundDrawables(null, null, status, null)
                name.text = item.name
                holder.setText(R.id.tv_address, item.value)
            }
        }
        binding.rvServerGroup.adapter = mAdapter
        mAdapter.setEmptyView(R.layout.layout_empty_common)
    }

    override fun initData() {
        viewModel.loading.observe(this) { binding.refresh.isRefreshing = it.loading }
        if (selectable) {
            // 选择模式下需要服务器分组的id，因此需要从接口直接获取
            viewModel.serverGroups.observe(this) { servers ->
                val name = viewModel.companyUser.value?.company?.name
                val server = viewModel.companyUser.value?.company?.imServer
                if (!server.isNullOrEmpty()) {
                    mAdapter.setList(
                        servers.toMutableList().also {
                            it.add(0, ServerGroupInfo("company", 0, name!!, server))
                        }
                    )
                } else {
                    mAdapter.setList(servers)
                }
            }
        } else {
            viewModel.current.observe(this) { info ->
                val list = info.servers.map { ServerGroupInfo(it.id, 0, it.name, it.address) }
                mAdapter.setList(list)
            }
        }
        viewModel.connection.observeSocketState(this) { (key, state) ->
            mAdapter.data.forEachIndexed { index, info ->
                if (info.value.urlKey() == key) {
                    if (info.state != state) {
                        mAdapter.notifyItemChanged(index)
                    }
                    return@forEachIndexed
                }
            }
        }
        viewModel.getServerGroupList()
    }

    fun refresh() {
        viewModel.getServerGroupList()
    }

    fun setOnSelectListener(listener: (ServerGroupInfo) -> Unit) {
        this.onSelectListener = listener
    }

    private fun goEditPage(info: ServerGroupInfo? = null, defaultGroup: Boolean = false) {
        ARouter.getInstance().build(MainModule.EDIT_SERVER_GROUP)
            .withParcelable("info", info)
            .withBoolean("defaultGroup", defaultGroup)
            .navigation(requireActivity(), ServerManagementActivity.REQUEST_EDIT)
    }

    override fun setEvent() {

    }
}