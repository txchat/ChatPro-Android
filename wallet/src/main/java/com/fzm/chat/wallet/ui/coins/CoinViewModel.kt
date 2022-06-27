package com.fzm.chat.wallet.ui.coins

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.fzm.chat.biz.base.AppConst
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.db.entity.AddCoinTabBean
import com.fzm.wallet.sdk.db.entity.Coin
import com.zjy.architecture.data.Result
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request
import kotlinx.coroutines.flow.onEach

/**
 * @author zhengjy
 * @since 2022/02/28
 * Description:
 */
class CoinViewModel(private val delegate: LoginDelegate) : LoadingViewModel() {

    private val _chainAssets by lazy { MutableLiveData<List<AddCoinTabBean>>() }
    val chainAssets: LiveData<List<AddCoinTabBean>>
        get() = _chainAssets

    private val _searchResult by lazy { MutableLiveData<List<Coin>>() }
    val searchResult: LiveData<List<Coin>>
        get() = _searchResult

    val homeCoins by lazy {
        BWallet.get().getCoinsFlow().onEach { coins ->
            netCoinsMap.putAll(
                coins.filter { it.netId != null }
                    .associateBy { it.netId }
            )
        }.asLiveData(coroutineContext)
    }

    val netCoinsMap = HashMap<String, Coin>()

    fun hasPassword() = delegate.preference.hasChatPassword()

    fun searchCoins(page: Int, keywords: String, chain: String, platform: String) {
        request<List<Coin>> {
            onRequest {
                try {
                    val coins = BWallet.get()
                        .searchCoins(page, AppConst.PAGE_SIZE, keywords, chain, platform)
                    Result.Success(coins)
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }
            onSuccess {
                _searchResult.value = it
            }
        }
    }

    fun addCoin(coin: Coin, index: Int, password: suspend () -> String) {
        request<Unit>(loading = false) {
            onRequest {
                Result.Success(BWallet.get().addCoins(listOf(coin), password))
            }
        }
    }

    fun removeCoin(coin: Coin, index: Int) {
        request<Unit>(loading = false) {
            onRequest {
                Result.Success(BWallet.get().deleteCoins(listOf(coin)))
            }
        }
    }

    fun getChainAssets() {
        request<List<AddCoinTabBean>> {
            onRequest {
                Result.Success(BWallet.get().getChainAssets())
            }
            onSuccess {
                _chainAssets.value = it
            }
        }
    }
}