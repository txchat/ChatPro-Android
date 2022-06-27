package com.fzm.chat.wallet.ui

import androidx.core.os.bundleOf
import com.fzm.chat.router.biz.ResultReceiver
import com.fzm.chat.wallet.R
import com.fzm.wallet.sdk.db.entity.Coin

/**
 * @author zhengjy
 * @since 2021/10/19
 * Description:
 */
class TokenListFragment : AssetsListFragment() {

    companion object {
        fun create(
            listener: ResultReceiver.OnResultListener<Coin>?,
            layoutId: Int = R.layout.fragment_assets_list,
            itemLayout: Int = R.layout.item_wallet_assets,
            coinFilter: ((Coin) -> Boolean)? = null
        ): TokenListFragment {
            return TokenListFragment().apply {
                arguments = bundleOf(
                    "layoutId" to layoutId,
                    "itemLayout" to itemLayout
                )
                setOnResultReceiver(listener)
                coinFilter?.also { setCoinFilter(it) }
            }
        }
    }

    override fun observeAssets() = viewModel.moduleAssets

    override fun getAssetsList(showLoading: Boolean) {

    }
}