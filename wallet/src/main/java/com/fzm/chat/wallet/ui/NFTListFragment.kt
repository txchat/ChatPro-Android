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
class NFTListFragment : AssetsListFragment() {

    companion object {
        fun create(
            listener: ResultReceiver.OnResultListener<Coin>?,
            layoutId: Int = R.layout.fragment_assets_list,
            itemLayout: Int = R.layout.item_wallet_assets
        ): NFTListFragment {
            return NFTListFragment().apply {
                arguments = bundleOf(
                    "layoutId" to layoutId,
                    "itemLayout" to itemLayout
                )
                setOnResultReceiver(listener)
            }
        }
    }

    override fun observeAssets() = viewModel.nftAssets

    override fun getAssetsList(showLoading: Boolean) = viewModel.getNFTAssetList(showLoading)
}