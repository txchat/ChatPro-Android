package com.fzm.chat.ui

import android.view.View
import androidx.fragment.app.commit
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.core.data.bean.PreSendParams
import com.fzm.chat.biz.base.AppConst
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.ChatSearchView
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.databinding.ActivityContactSelectBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.ContactSelectViewModel
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.visible
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf

/**
 * @author zhengjy
 * @since 2021/06/18
 * Description:
 */
@Route(path = MainModule.CONTACT_SELECT, extras = AppConst.NEED_LOGIN)
class ContactSelectActivity : BizActivity(), View.OnClickListener {

    @JvmField
    @Autowired
    var preSend: PreSendParams? = null

    @JvmField
    @Autowired
    var messages: ArrayList<ChatMessage>? = null

    /**
     * 需要转账的币种类型
     */
    @JvmField
    @Autowired
    var symbol: String? = null
    @JvmField
    @Autowired
    var chain: String? = null
    @JvmField
    @Autowired
    var platform: String? = null

    /**
     * 选择联系人操作类型
     */
    @JvmField
    @Autowired
    var action: String? = null

    @JvmField
    @Autowired
    var channelFilter: Int = 0

    private lateinit var viewModel: ContactSelectViewModel

    private val binding by init { ActivityContactSelectBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        if (preSend != null || messages != null) {
            binding.chatSearch.setHint(getString(R.string.chat_tips_search_contact))
            binding.tvSearchTips.hint = getString(R.string.chat_tips_search_contact)
            viewModel = getViewModel<ForwardSelectFragment.ForwardSelectViewModel>()
            supportFragmentManager.commit {
                add(R.id.fcv_container, ForwardSelectFragment.create(preSend, messages))
            }
        } else if (action == "transfer") {
            binding.chatSearch.setHint(getString(R.string.chat_tips_search_friends))
            binding.tvSearchTips.hint = getString(R.string.chat_tips_search_friends)
            viewModel = getViewModel<TransferSelectFragment.TransferViewModel>()
            supportFragmentManager.commit {
                add(R.id.fcv_container, TransferSelectFragment.create(symbol, chain, platform))
            }
        } else if (action == "contactCard") {
            if (channelFilter and ContactSelectViewModel.PRIVATE != 0 && channelFilter and ContactSelectViewModel.GROUP != 0) {
                binding.chatSearch.setHint(getString(R.string.chat_tips_search_contact))
                binding.tvSearchTips.hint = getString(R.string.chat_tips_search_contact)
            } else if (channelFilter and ContactSelectViewModel.PRIVATE != 0) {
                binding.chatSearch.setHint(getString(R.string.chat_tips_search_friends))
                binding.tvSearchTips.hint = getString(R.string.chat_tips_search_friends)
            } else {
                binding.chatSearch.setHint(getString(R.string.chat_tips_search_groups))
                binding.tvSearchTips.hint = getString(R.string.chat_tips_search_groups)
            }
            viewModel = getViewModel<ContactCardSelectFragment.ContactCardViewModel> { parametersOf(channelFilter) }
            supportFragmentManager.commit {
                add(R.id.fcv_container, ContactCardSelectFragment.create(channelFilter))
            }
        }
    }

    override fun initData() {
    }

    override fun setEvent() {
        binding.tvBack.setOnClickListener(this)
        binding.chatSearch.setOnSearchCancelListener(object :
            ChatSearchView.OnSearchCancelListener {
            override fun onSearchCancel() {
                binding.llContactSearch.visible()
                binding.chatSearch.gone()
                KeyboardUtils.hideKeyboard(binding.chatSearch.getFocusView())
                binding.chatSearch.setText(null)
            }
        })
        binding.llContactSearch.setOnClickListener(this)
        binding.chatSearch.setOnTextChangeListener(object : ChatSearchView.OnTextChangeListener {
            override fun onTextChange(s: String) {
                viewModel.searchByKeywords(s)
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_back -> onBackPressed()
            R.id.ll_contact_search -> {
                binding.llContactSearch.gone()
                binding.chatSearch.visible()
                binding.chatSearch.postDelayed({
                    KeyboardUtils.showKeyboard(binding.chatSearch.getFocusView())
                }, 100)
            }
        }
    }
}