package com.fzm.chat.contact

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.fragment.app.commit
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.databinding.ActivityChooseServerGroupBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.ServerManageViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/03/01
 * Description:
 */
@Route(path = MainModule.CHOOSE_SERVER_GROUP)
class ChooseServerGroupActivity : BizActivity() {

    private val viewModel by viewModel<ServerManageViewModel>()
    private val binding by init { ActivityChooseServerGroupBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override val darkStatusColor: Boolean = true

    override fun initView() {
        val fragment = ChatServerListFragment.create(true)
        fragment.setOnSelectListener {
            setResult(Activity.RESULT_OK, Intent().putExtra("serverInfo", it))
            finish()
        }
        supportFragmentManager.commit {
            add(R.id.fcv_container, fragment)
        }
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.ctbTitle.setOnRightClickListener {
            ARouter.getInstance().build(MainModule.SERVER_MANAGE).navigation(this, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.getServerGroupList()
    }
}