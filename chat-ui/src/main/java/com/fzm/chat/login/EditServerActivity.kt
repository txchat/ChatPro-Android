package com.fzm.chat.login

import android.view.View
import androidx.core.widget.addTextChangedListener
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.ServerNode
import com.fzm.chat.databinding.ActivityEditServerBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.ChooseServerViewModel
import com.fzm.widget.dialog.EasyDialog
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.toast
import okhttp3.HttpUrl
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/02/20
 * Description:
 */
@Route(path = MainModule.EDIT_SERVER)
class EditServerActivity : BizActivity() {

    @JvmField
    @Autowired
    var node: ServerNode.Node? = null

    /**
     * 添加服务器类型
     * 1：聊天服务器  2：区块链节点
     */
    @JvmField
    @Autowired
    var type: Int = 0

    private val viewModel by viewModel<ChooseServerViewModel>()
    private val binding by init { ActivityEditServerBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        when (type) {
            ChatConst.CHAT_SERVER -> {
                if (node != null) {
                    binding.ctbTitle.setTitle(getString(R.string.chat_login_edit_chat_server))
                } else {
                    binding.ctbTitle.setTitle(getString(R.string.chat_login_add_chat_server))
                }
            }
            ChatConst.CONTRACT_SERVER -> {
                if (node != null) {
                    binding.ctbTitle.setTitle(getString(R.string.chat_login_edit_contract_server))
                } else {
                    binding.ctbTitle.setTitle(getString(R.string.chat_login_add_contract_server))
                }
            }
            else -> {
                finish()
                return
            }
        }
        node?.apply {
            binding.tvNameCount.text = getString(R.string.chat_tips_num_15, name.length)
            binding.etName.setText(name)
            binding.etName.setSelection(name.length)
            binding.etAddress.setText(address)
            binding.etAddress.setSelection(address.length)
        }
    }

    override fun initData() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.addResult.observe(this) {
            if (node == null) {
                toast(R.string.chat_tip_add_server_success)
            } else {
                toast(R.string.chat_tip_edit_server_success)
            }
            setResult(RESULT_OK)
            finish()
        }
        viewModel.delResult.observe(this) {
            toast(R.string.chat_tip_del_server_success)
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.ctbTitle.setRightVisible(node != null)
        binding.ctbTitle.setOnRightClickListener {
            node?.apply {
                EasyDialog.Builder()
                    .setHeaderTitle(getString(R.string.biz_tips))
                    .setContent(getString(R.string.chat_tip_confirm_to_del_server))
                    .setBottomLeftText(getString(R.string.biz_cancel))
                    .setBottomRightText(getString(R.string.biz_confirm))
                    .setBottomRightClickListener { dialog ->
                        viewModel.deleteHistory(id)
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
                viewModel.addHistory(node?.id ?: -1, name, address, type)
            } catch (e: Exception) {
                toast(R.string.chat_tip_wrong_server_format)
            }
        }
    }
}