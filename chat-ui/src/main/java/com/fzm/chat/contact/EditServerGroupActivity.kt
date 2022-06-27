package com.fzm.chat.contact

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.widget.addTextChangedListener
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.data.bean.ServerGroupInfo
import com.fzm.chat.core.data.bean.ServerNode
import com.fzm.chat.databinding.ActivityEditServerGroupBinding
import com.fzm.chat.databinding.ItemServerHistoryBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.ServerManageViewModel
import com.fzm.chat.widget.ListPopup
import com.fzm.widget.dialog.EasyDialog
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.toast
import okhttp3.HttpUrl
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/02/22
 * Description:服务器分组创建编辑界面
 */
@Route(path = MainModule.EDIT_SERVER_GROUP)
class EditServerGroupActivity : BizActivity() {

    /**
     * 分组信息，如果是创建则为空
     */
    @JvmField
    @Autowired
    var info: ServerGroupInfo? = null

    /**
     * 是否是默认分组
     */
    @JvmField
    @Autowired
    var defaultGroup: Boolean = false

    private val viewModel by viewModel<ServerManageViewModel>()
    private val binding by init { ActivityEditServerGroupBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        if (info != null) {
            binding.ctbTitle.setTitle(getString(R.string.chat_login_edit_chat_server_group))
            binding.tvNameCount.text = getString(R.string.chat_tips_num_15, info!!.name.length)
            binding.etName.setText(info!!.name)
            binding.etName.setSelection(info!!.name.length)
            binding.etAddress.setText(info!!.value)
            binding.etAddress.setSelection(info!!.value.length)
            binding.ctbTitle.setRightVisible(true)
        } else {
            binding.ctbTitle.setTitle(getString(R.string.chat_login_add_chat_server_group))
            binding.ctbTitle.setRightVisible(false)
        }
    }

    override fun initData() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.addResult.observe(this) {
            toast(R.string.chat_tip_add_server_group_success)
            dismiss()
            setResult(RESULT_OK)
            finish()
        }
        viewModel.delResult.observe(this) {
            toast(R.string.chat_tip_del_server_group_success)
            dismiss()
            setResult(RESULT_OK)
            finish()
        }
        viewModel.editResult.observe(this) {
            toast(R.string.chat_tip_edit_server_group_success)
            dismiss()
            setResult(RESULT_OK)
            finish()
        }
        viewModel.chatList.observe(this) {
            adapter.setData(it)
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.ctbTitle.setOnRightClickListener {
            info?.apply {
                if (defaultGroup) {
                    toast(R.string.chat_tip_cant_del_default_server)
                    return@apply
                }
                EasyDialog.Builder()
                    .setHeaderTitle(getString(R.string.biz_tips))
                    .setContent(getString(R.string.chat_tip_confirm_to_del_server_group))
                    .setBottomLeftText(getString(R.string.biz_cancel))
                    .setBottomRightText(getString(R.string.biz_confirm))
                    .setBottomRightClickListener { dialog ->
                        viewModel.deleteServerGroup(id)
                        dialog.dismiss()
                    }
                    .create(instance)
                    .show()
            }
        }
        binding.etName.addTextChangedListener {
            if (it.isNullOrEmpty()) {
                binding.tvNameCount.text = getString(R.string.chat_tips_num_15, 0)
            } else {
                binding.tvNameCount.text = getString(R.string.chat_tips_num_15, it.length)
            }
        }
        binding.ivHistory.setOnClickListener {
            viewModel.fetchServerList()
            val popup = ListPopup(this, adapter)
            popup.create(binding.slAddress.measuredWidth, 200.dp) { _, _, position, _ ->
                val node = adapter.getItem(position) as ServerNode.Node
                binding.etAddress.setText(node.address)
                binding.etAddress.setSelection(node.address.length)
                popup.dismiss()
            }
            popup.setPositionOffsetYWhenBottom((-10).dp)
            popup.show(binding.slAddress)
        }
        binding.tvSubmit.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            try {
                val uri = HttpUrl.get(address)
                if (uri.scheme() != "https" && uri.scheme() != "http") {
                    toast(R.string.chat_tip_wrong_server_protocol)
                    return@setOnClickListener
                }
                if (uri.port() > 65535 || uri.port() < 1) {
                    toast(R.string.chat_tip_wrong_server_port)
                    return@setOnClickListener
                }
                if (info != null) {
                    viewModel.editServerGroup(info!!.id, name, address)
                } else {
                    viewModel.addServerGroup(name, address)
                }
            } catch (e: Exception) {
                toast(R.string.chat_tip_wrong_server_format)
            }
        }
    }

    private val adapter by lazy { HistoryAdapter(this) }

    private class HistoryAdapter(private val context: Context) : BaseAdapter() {

        fun setData(list: List<ServerNode.Node>) {
            data.clear()
            data.addAll(list)
            notifyDataSetChanged()
        }

        private val data = mutableListOf<ServerNode.Node>()

        override fun getCount(): Int {
            return data.size
        }

        override fun getItem(position: Int): Any {
            return data[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val root: View
            val bind: ItemServerHistoryBinding
            if (convertView == null) {
                bind = ItemServerHistoryBinding.inflate(LayoutInflater.from(context))
                root = bind.root
                root.tag = bind
            } else {
                root = convertView
                bind = root.tag as ItemServerHistoryBinding
            }
            bind.tvName.text = data[position].name
            bind.tvAddress.text = data[position].address

            return root
        }
    }
}