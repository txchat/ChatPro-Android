package com.fzm.chat.wallet.ui.coins

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.ui.PasswordVerifyFragment
import com.fzm.chat.router.biz.PasswordVerifier
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.route
import com.fzm.chat.wallet.databinding.LayoutCoinManagementBinding
import com.fzm.wallet.sdk.db.entity.Coin
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.androidx.viewmodel.ext.android.getViewModel
import kotlin.coroutines.resume

/**
 * @author zhengjy
 * @since 2022/03/01
 * Description:
 */
abstract class AbstractCoinFragment : BizFragment() {

    protected lateinit var adapter: CoinManageAdapter

    protected val viewModel by lazy { requireActivity().getViewModel<CoinViewModel>() }

    protected val binding by init<LayoutCoinManagementBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        binding.rvCoins.layoutManager = LinearLayoutManager(requireContext())
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
        setupCoinDrag()
    }

    override fun initData() {
        loadCoins()
    }

    protected fun onCoinsUpdate(coins: List<Coin>) {
        adapter.setDiffNewData(coins.toMutableList())
    }

    override fun setEvent() {
        
    }

    abstract fun loadCoins()

    open fun setupCoinDrag() {

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
            dialog?.show(childFragmentManager, "VERIFY_ENC_PWD")
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
            dialog?.show(childFragmentManager, "SET_ENC_PWD")
        }
    }
}