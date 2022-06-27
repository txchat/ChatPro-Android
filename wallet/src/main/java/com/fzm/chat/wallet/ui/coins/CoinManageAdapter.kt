package com.fzm.chat.wallet.ui.coins

import android.view.View
import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.DraggableModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.wallet.R
import com.fzm.wallet.sdk.db.entity.Coin
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.singleClick

/**
 * @author zhengjy
 * @since 2022/03/01
 * Description:
 */
abstract class CoinManageAdapter(
    val list: MutableList<Coin>? = null
) : BaseQuickAdapter<Coin, BaseViewHolder>(R.layout.item_coin_management, list), DraggableModule {

    init {
        setDiffCallback(CoinDiffCallback())
    }

    override fun convert(holder: BaseViewHolder, item: Coin) {
        holder.getView<ImageView>(R.id.ic_coin).load(item.icon)
        holder.setText(R.id.tv_coin, item.name)
        holder.setText(R.id.tv_name, item.nickname)
        holder.setImageResource(
            R.id.iv_operation,
            if (item.status == Coin.STATUS_ENABLE) R.drawable.ic_coins_del else R.drawable.ic_coins_add
        )
        holder.getView<View>(R.id.iv_operation).singleClick {
            if (item.status == Coin.STATUS_ENABLE) {
                updateCoin(item, data.indexOf(item), false)
            } else {
                updateCoin(item, data.indexOf(item), true)
            }
        }
    }

    abstract fun updateCoin(coin: Coin, index: Int, add: Boolean)
}