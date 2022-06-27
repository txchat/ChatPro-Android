package com.fzm.chat.app

import androidx.lifecycle.SavedStateHandle
import com.zjy.architecture.mvvm.LoadingViewModel

/**
 * @author zhengjy
 * @since 2021/12/10
 * Description:
 */
class MainViewModel(private val savedState: SavedStateHandle) : LoadingViewModel() {

    companion object {
        const val SHOW_SHOP = "shop"
        const val SHOW_WALLET = "wallet"
        const val SHOW_BIND = "bind"
    }

    val shopModule = savedState.getLiveData(SHOW_SHOP, false)

    val walletModule = savedState.getLiveData(SHOW_WALLET, false)

    val showBind = savedState.getLiveData(SHOW_BIND, true)
}