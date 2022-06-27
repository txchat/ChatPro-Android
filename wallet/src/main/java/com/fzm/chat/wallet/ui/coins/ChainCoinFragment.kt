package com.fzm.chat.wallet.ui.coins

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.fzm.wallet.sdk.db.entity.Coin

/**
 * @author zhengjy
 * @since 2022/03/01
 * Description:
 */
class ChainCoinFragment : AbstractCoinFragment() {

    companion object {
        fun create(coins: ArrayList<Coin>): ChainCoinFragment {
            return ChainCoinFragment().apply {
                arguments = bundleOf("coins" to coins)
            }
        }
    }

    private val coins = mutableListOf<Coin>()

    override fun initView(view: View, savedInstanceState: Bundle?) {
        coins.clear()
        coins.addAll(arguments?.getSerializable("coins") as ArrayList<Coin>)
        super.initView(view, savedInstanceState)
    }

    override fun initData() {
        super.initData()
        viewModel.homeCoins.observe(viewLifecycleOwner) {
            coins.forEachIndexed { index, coin ->
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
    }

    override fun loadCoins() {
        onCoinsUpdate(coins)
    }
}