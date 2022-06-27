package com.fzm.chat.wallet.ui.coins

import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.biz.base.AppConst
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.ui.PasswordVerifyFragment
import com.fzm.chat.biz.widget.ChatSearchView
import com.fzm.chat.router.biz.PasswordVerifier
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.route
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.wallet.databinding.ActivitySearchCoinBinding
import com.fzm.wallet.sdk.db.entity.Coin
import com.zjy.architecture.ext.*
import com.zjy.architecture.util.KeyboardUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.coroutines.resume

/**
 * @author zhengjy
 * @since 2022/02/25
 * Description:
 */
@Route(path = WalletModule.SEARCH_COINS)
class SearchCoinActivity : BizActivity() {

    private var page = 1
    private var searchKey = ""
    private val viewModel by viewModel<CoinViewModel>()

    private lateinit var adapter: BaseQuickAdapter<Coin, BaseViewHolder>

    private val binding by init<ActivitySearchCoinBinding>()

    override val root: View
        get() = binding.root

    override fun initView() {
        binding.svSearch.getTransitionView().transitionName = "searchView"
        binding.rvCoins.layoutManager = LinearLayoutManager(this)
        adapter = object : CoinManageAdapter(null) {
            override fun updateCoin(coin: Coin, index: Int, add: Boolean) {
                if (add) {
                    viewModel.addCoin(coin, index, ::requestPassword)
                } else {
                    viewModel.removeCoin(coin, index)
                }
            }
        }
        binding.rvCoins.adapter = adapter
        binding.refresh.setEnableRefresh(false)
        binding.refresh.setEnableLoadMore(true)
        binding.refresh.setOnLoadMoreListener {
            viewModel.searchCoins(page, searchKey, "", "")
        }
        binding.svSearch.postDelayed({
            KeyboardUtils.showKeyboard(binding.svSearch.getFocusView())
        }, 200)
    }

    override fun initData() {
        viewModel.homeCoins.observe(this) {
            adapter.data.forEachIndexed { index, coin ->
                val temp = viewModel.netCoinsMap[coin.netId]
                if (temp != null) {
                    if (coin.status != temp.status) {
                        coin.status = temp.status
                        coin.id = temp.id
                        adapter.notifyItemChanged(index)
                    }
                }
            }
        }
        viewModel.searchResult.observe(this) {
            it.forEach { coin ->
                val temp = viewModel.netCoinsMap[coin.netId]
                if (temp != null) {
                    if (coin.status != temp.status) {
                        coin.status = temp.status
                        coin.id = temp.id
                    }
                }
            }
            if (page == 1) {
                binding.refresh.finishRefresh()
                if (it.size < AppConst.PAGE_SIZE) {
                    binding.refresh.finishLoadMoreWithNoMoreData()
                }
                adapter.setList(it)
            } else {
                if (it.size < AppConst.PAGE_SIZE) {
                    binding.refresh.finishLoadMoreWithNoMoreData()
                } else {
                    binding.refresh.finishLoadMore()
                }
                adapter.addData(it)
            }
            page++
        }
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
                    adapter.setList(null)
                    binding.rvCoins.gone()
                } else {
                    binding.rvCoins.visible()
                    page = 1
                    searchKey = s
                    viewModel.searchCoins(page, searchKey, "", "")
                }
            }
        })
    }

    private suspend fun requestPassword() = suspendCancellableCoroutine<String> { cont ->
        if (viewModel.hasPassword()) {
            val dialog by route<PasswordVerifyFragment>(
                MainModule.VERIFY_ENC_PWD,
                bundleOf("type" to 0)
            )
            dialog?.setOnPasswordVerifyListener(object :
                PasswordVerifier.OnPasswordVerifyListener {
                override fun onSuccess(password: String) {
                    cont.resume(password)
                }

                override fun onCancel() {
                    cont.cancel()
                }
            })
            dialog?.show(supportFragmentManager, "VERIFY_ENC_PWD")
        } else {
            val dialog by route<PasswordVerifyFragment>(
                MainModule.SET_ENC_PWD,
                bundleOf("showWords" to 0)
            )
            dialog?.setOnPasswordVerifyListener(object :
                PasswordVerifier.OnPasswordVerifyListener {
                override fun onSuccess(password: String) {
                    cont.resume(password)
                }

                override fun onCancel() {
                    cont.cancel()
                }
            })
            dialog?.show(supportFragmentManager, "SET_ENC_PWD")
        }
    }
}