package com.fzm.chat.wallet.ui.coins

import androidx.recyclerview.widget.DiffUtil
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.uid

/**
 * @author zhengjy
 * @since 2022/03/01
 * Description:
 */
class CoinDiffCallback : DiffUtil.ItemCallback<Coin>() {

    override fun areItemsTheSame(oldItem: Coin, newItem: Coin): Boolean {
        return oldItem.uid == newItem.uid
    }

    override fun areContentsTheSame(oldItem: Coin, newItem: Coin): Boolean {
        if (oldItem.balance != newItem.balance) return false
        if (oldItem.rmb != newItem.rmb) return false
        if (oldItem.status != newItem.status) return false
        if (oldItem.icon != newItem.icon) return false
        if (oldItem.chain != newItem.chain) return false
        if (oldItem.platform != newItem.platform) return false
        if (oldItem.name != newItem.name) return false
        if (oldItem.nickname != newItem.nickname) return false
        return true
    }
}