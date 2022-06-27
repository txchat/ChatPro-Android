package com.fzm.chat.ui

import android.view.View
import androidx.core.widget.doOnTextChanged
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.databinding.ActivityEditInfoBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.EditInfoViewModel
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2020/12/16
 * Description:
 */
@Route(path = MainModule.EDIT_INFO)
class EditInfoActivity : BizActivity() {

    private val viewModel by viewModel<EditInfoViewModel>()
    private var name: String = ""

    private val binding by init { ActivityEditInfoBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.current.observe(this) {
            name = it.nickname
            binding.etName.setText(name)
            binding.etName.selectAll()
            binding.tvNameCount.text = getString(R.string.chat_tips_num_20, name.length)
        }
        viewModel.nicknameResult.observe(this) {
            toast(R.string.chat_tip_nickname_edit_success)
            finish()
        }
        binding.etName.postDelayed({
            KeyboardUtils.showKeyboard(binding.etName)
        }, 300)
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.etName.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                binding.tvNameCount.text = getString(R.string.chat_tips_num_20, 0)
            } else {
                binding.tvNameCount.text = getString(R.string.chat_tips_num_20, text.length)
            }
        }
        binding.tvSubmit.setOnClickListener {
            val newName = binding.etName.text?.toString()?.trim()
            if (newName.isNullOrEmpty()) {
                toast(R.string.chat_tip_nickname_empty)
                return@setOnClickListener
            }
            if (name != newName) {
                viewModel.setNickname(newName)
            } else {
                finish()
            }
        }
    }
}