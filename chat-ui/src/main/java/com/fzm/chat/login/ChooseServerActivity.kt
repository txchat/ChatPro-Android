package com.fzm.chat.login

import android.content.Intent
import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.FadePageTransformer
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.databinding.ActivityChooseServerBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.ChooseServerViewModel
import com.fzm.chat.widget.AddServerPopup
import com.fzm.widget.ScrollPagerAdapter
import com.zjy.architecture.ext.toast
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/01/19
 * Description:
 */
@Route(path = MainModule.CHOOSE_SERVER)
class ChooseServerActivity : BizActivity(), View.OnClickListener {

    companion object {
        const val REQUEST_EDIT = 100
    }

    private val binding by init { ActivityChooseServerBinding.inflate(layoutInflater) }
    private val viewModel by viewModel<ChooseServerViewModel>()

    private lateinit var chat: ServerListFragment
    private lateinit var contract: ServerListFragment

    override val root: View
        get() = binding.root

    override val darkStatusColor: Boolean = true

    override fun initView() {
        binding.vpServer.apply {
            chat = ServerListFragment.chatList(showTips = true)
            contract = ServerListFragment.contractList(showTips = true)
            adapter = ScrollPagerAdapter(
                supportFragmentManager,
                listOf(
                    getString(R.string.chat_title_chat_server),
                    getString(R.string.chat_title_contract_server)
                ),
                listOf(chat, contract)
            )
            setPageTransformer(false, FadePageTransformer())
            offscreenPageLimit = 2
        }
        binding.tabLayout.setViewPager(binding.vpServer)
        binding.tabLayout.onPageSelected(binding.vpServer.currentItem)
    }

    override fun initData() {
        viewModel.serverFetchFail.observe(this) { toast(it) }
        viewModel.fetchServerList()
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.ctbTitle.setOnRightClickListener {
            AddServerPopup.create(this, this).show(binding.ctbTitle.getRightView())
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.add_chat_server -> {
                ARouter.getInstance().build(MainModule.EDIT_SERVER)
                    .withInt("type", ChatConst.CHAT_SERVER)
                    .navigation(this, REQUEST_EDIT)
                binding.vpServer.currentItem = 0
            }
            R.id.add_contract_server -> {
                ARouter.getInstance().build(MainModule.EDIT_SERVER)
                    .withInt("type", ChatConst.CONTRACT_SERVER)
                    .navigation(this, REQUEST_EDIT)
                binding.vpServer.currentItem = 1
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_EDIT) {
                viewModel.fetchServerList()
            }
        }
    }
}