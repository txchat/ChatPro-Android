package com.fzm.chat.login.words

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.databinding.FragmentImportMnemBinding
import com.fzm.chat.router.app.AppModule
import com.zjy.architecture.ext.toast
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2020/12/11
 * Description:
 */
class ImportMnemFragment : BizFragment() {

    private val viewModel by viewModel<ImportAccountViewModel>()

    private val binding by init<FragmentImportMnemBinding>()

    private var block = false

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
//        binding.etMnem.filters = arrayOf(MnemInputFilter())
        viewModel.loading.observe(viewLifecycleOwner) { setupLoading(it) }
        viewModel.importResult.observe(viewLifecycleOwner) {
            if (it != null) {
                context?.applicationContext?.toast(R.string.chat_login_mnem_import_success)
                ARouter.getInstance().build(AppModule.MAIN).navigation()
                activity?.finish()
            } else {
                toast(R.string.chat_login_mnem_import_fail)
            }
        }
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.etMnem.doOnTextChanged { text, start, count, after ->
            binding.btnImport.isEnabled = !text.isNullOrEmpty()
            if (block) return@doOnTextChanged
            if (!text.isNullOrEmpty()) {
                val noSpace = text.replace(" ".toRegex(), "")
                val first = text.substring(0, 1)
                if (first.matches(ChatConst.REGEX_CHINESE.toRegex())) {
                    // 如果是中文助记词，则需要添加空格
                    val sb = StringBuilder()
                    for (i in noSpace.indices) {
                        sb.append(noSpace[i])
                        if ((i + 1) % 3 == 0 && i != noSpace.length - 1) {
                            sb.append(" ")
                        }
                    }
                    block = true
                    binding.etMnem.setText(sb)
                    binding.etMnem.setSelection(sb.length)
                    block = false
                }
            }
        }
        binding.btnImport.setOnClickListener {
            viewModel.importMnemonic(binding.etMnem.text.toString())
        }
    }
}