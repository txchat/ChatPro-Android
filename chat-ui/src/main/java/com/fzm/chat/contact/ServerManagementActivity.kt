package com.fzm.chat.contact

import android.content.Intent
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.FadePageTransformer
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.databinding.ActivityServerManagementBinding
import com.fzm.chat.login.ServerListFragment
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.ChooseServerViewModel
import com.fzm.widget.ScrollPagerAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/02/22
 * Description:用户服务器管理页面
 */
@Route(path = MainModule.SERVER_MANAGE)
class ServerManagementActivity : BizActivity() {

    companion object {
        const val REQUEST_EDIT = 100
    }

    private val chooseServerViewModel by viewModel<ChooseServerViewModel>()

    private val binding by init { ActivityServerManagementBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override val darkStatusColor: Boolean = true

    override fun initView() {
        binding.vpServer.apply {
            // 聊天服务器分组
            val groups = ChatServerListFragment.create(false)
            // 合约服务器选择
            val contract = ServerListFragment.contractList()
            adapter = ScrollPagerAdapter(
                supportFragmentManager,
                listOf(
                    getString(R.string.chat_title_chat_server_group),
                    getString(R.string.chat_title_contract_server)
                ),
                listOf(groups, contract)
            )
            setPageTransformer(false, FadePageTransformer())
            offscreenPageLimit = 2
        }
        binding.tabLayout.setViewPager(binding.vpServer)
        binding.tabLayout.onPageSelected(binding.vpServer.currentItem)
    }

    override fun initData() {
        chooseServerViewModel.fetchServerList()
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.ctbTitle.setOnRightClickListener {
            if (binding.vpServer.currentItem == 0) {
                ARouter.getInstance().build(MainModule.EDIT_SERVER_GROUP)
                    .navigation(this, REQUEST_EDIT)
            } else {
                ARouter.getInstance().build(MainModule.EDIT_SERVER)
                    .withInt("type", ChatConst.CONTRACT_SERVER)
                    .navigation(this, REQUEST_EDIT)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_EDIT) {
                chooseServerViewModel.fetchServerList()
            }
        }
    }
}