package com.fzm.chat.ui

import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.databinding.ActivitySearchUserBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.biz.widget.ChatSearchView
import com.fzm.chat.databinding.ItemSearchListBinding
import com.fzm.chat.utils.StringUtils
import com.fzm.chat.vm.SearchUserViewModel
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.visible
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/01/05
 * Description:
 */
@Route(path = MainModule.SEARCH_USER)
class SearchUserActivity : BizActivity() {

    private val viewModel by viewModel<SearchUserViewModel>()
    private var searchKey = ""
    private val searchLayout by init { ItemSearchListBinding.inflate(layoutInflater) }

    private val binding by init<ActivitySearchUserBinding>()

    override val root: View
        get() = binding.root

    override fun initView() {
        binding.svSearch.getTransitionView().transitionName = "searchView"
        binding.tvMyAccount.text = getString(R.string.chat_tips_search_my_account, StringUtils.getDisplayAddress(viewModel.getAddress()))
        binding.flSearch.addView(searchLayout.root)
        viewModel.searchResult.observe(this) { user ->
            if (user != null) {
                searchLayout.tvTag.setText(R.string.chat_tips_search_type_user)
                searchLayout.tvName.text = user.getDisplayName()
                searchLayout.ivAvatar.load(user.getDisplayImage(), R.mipmap.default_avatar_round)
                searchLayout.llContainer.setOnClickListener {
                    ARouter.getInstance().build(MainModule.CONTACT_INFO)
                        .withString("address", user.address)
                        .navigation()
                }
                binding.statusLayout.showContent()
            } else {
                searchLayout.llContainer.setOnClickListener(null)
                binding.statusLayout.showEmpty()
            }
        }
    }

    override fun initData() {
        binding.svSearch.postDelayed({
            KeyboardUtils.showKeyboard(binding.svSearch.getFocusView())
        }, 300)
    }

    override fun setEvent() {
        binding.svSearch.setOnSearchCancelListener(object : ChatSearchView.OnSearchCancelListener {
            override fun onSearchCancel() {
                KeyboardUtils.hideKeyboard(binding.svSearch.getFocusView())
                onBackPressedSupport()
            }
        })
        binding.svSearch.setOnTextChangeListener(object : ChatSearchView.OnTextChangeListener {
            override fun onTextChange(s: String) {
                if (s.isEmpty()) {
                    binding.rlList.gone()
                } else {
                    binding.rlList.visible()
                    binding.statusLayout.showLoading()
                    searchKey = s
                    viewModel.searchUser(searchKey)
                }
            }
        })
        binding.llMyAccount.setOnClickListener {
            ARouter.getInstance().build(MainModule.QR_CODE).navigation()
        }
    }
}