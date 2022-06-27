package com.fzm.chat.ui

import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.databinding.ActivityEditUserRemarkBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.ext.toast
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2021/02/01
 * Description:
 */
@Route(path = MainModule.CONTACT_REMARK)
class EditUserRemarkActivity : BizActivity(), View.OnClickListener {

    @JvmField
    @Autowired
    var address: String? = null

    @JvmField
    @Autowired
    var remark: String? = null

    var newRemark: String = ""

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val binding by init { ActivityEditUserRemarkBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        remark?.also {
            if (it.isNotEmpty()) {
                binding.etRemark.setText(it)
                binding.etRemark.setSelection(it.length)
            }
        }
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.tvSubmit.setOnClickListener(this)
        binding.etRemark.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.tvRemarkCount.text = getString(R.string.chat_tips_num_20, 0)
            } else {
                binding.tvRemarkCount.text = getString(R.string.chat_tips_num_20, text.length)
            }
            newRemark = text.toString()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_submit -> {
                address?.also {
                    lifecycleScope.launch {
                        database.friendUserDao().updateRemark(it, newRemark)
                        toast(R.string.chat_tip_remark_edit_success)
                        finish()
                    }
                }
            }
        }
    }
}