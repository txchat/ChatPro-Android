package com.fzm.chat.wallet.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.router.biz.ResultReceiver
import com.fzm.chat.wallet.R
import com.fzm.chat.wallet.data.copy
import com.fzm.chat.wallet.ui.coins.CoinDiffCallback
import com.fzm.wallet.sdk.db.entity.Coin
import com.zjy.architecture.ext.load
import com.zjy.architecture.util.ArithUtils
import com.zjy.architecture.util.FinanceUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/10/19
 * Description:
 */
abstract class AssetsListFragment : BizFragment(), ResultReceiver<Coin> {

    private var _layoutId: Int = 0
    private var itemLayout: Int = 0

    protected val viewModel by viewModel<WalletViewModel>()

    protected lateinit var mAdapter: BaseQuickAdapter<Coin, BaseViewHolder>

    private val _root by lazy { LayoutInflater.from(requireContext()).inflate(_layoutId, null) }

    override val root: View
        get() = _root

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _layoutId = arguments?.getInt("layoutId", R.layout.fragment_assets_list) ?: R.layout.fragment_assets_list
        itemLayout = arguments?.getInt("itemLayout", R.layout.item_wallet_assets) ?: R.layout.item_wallet_assets
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        val rvAssets = root.findViewById<RecyclerView>(R.id.rv_assets)
        val refresh = root.findViewById<SwipeRefreshLayout>(R.id.refresh)
        rvAssets.layoutManager = LinearLayoutManager(requireContext())
        mAdapter = object : BaseQuickAdapter<Coin, BaseViewHolder>(itemLayout, null) {
            override fun convert(holder: BaseViewHolder, item: Coin) {
                holder.getView<ImageView>(R.id.ic_coin).load(item.icon)
                holder.setText(R.id.tv_coin, item.name)
                if (item.name.isNullOrEmpty()) {
                    holder.setGone(R.id.tv_name, true)
                } else {
                    holder.setGone(R.id.tv_name, false)
                    holder.setText(R.id.tv_name, item.nickname)
                }
                holder.setText(R.id.tv_balance, item.balance)
                if (holder.getView<TextView>(R.id.tv_price).isVisible) {
                    val value = FinanceUtils.getPlainNum(ArithUtils.mul(item.rmb.toDouble(), item.balance.toDouble()), 2)
                    holder.setText(R.id.tv_price, "≈¥${value}")
                }
            }
        }
        mAdapter.setOnItemClickListener { _, _, position ->
            listener?.onResult(mAdapter.data[position])
        }
        rvAssets.adapter = mAdapter
        mAdapter.setEmptyView(R.layout.layout_empty_assets)
        mAdapter.isUseEmpty = false
        mAdapter.setDiffCallback(CoinDiffCallback())
        refresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.biz_wallet_accent))
        refresh.setOnRefreshListener {
            if (autoRefresh) {
                lifecycleScope.launch {
                    delay(1000)
                    refresh.isRefreshing = false
                }
            } else {
                getAssetsList(true)
            }
        }

        coinFilter?.also { viewModel.setCoinFilter(it) }
        observeAssets().observe(viewLifecycleOwner) {
            mAdapter.isUseEmpty = true
            mAdapter.setDiffNewData(it.asSequence().map { c -> c.copy() }.sorted().toMutableList())
        }
        viewModel.loading.observe(viewLifecycleOwner) {
            if (!autoRefresh) {
                refresh.isRefreshing = it.loading
            }
        }
    }

    open val autoRefresh: Boolean = true

    abstract fun observeAssets(): LiveData<List<Coin>>

    abstract fun getAssetsList(showLoading: Boolean)

    override fun onResume() {
        super.onResume()
        getAssetsList(false)
    }

    protected var listener: ResultReceiver.OnResultListener<Coin>? = null

    override fun setOnResultReceiver(listener: ResultReceiver.OnResultListener<Coin>?) {
        this.listener = listener
    }

    private var coinFilter: ((Coin) -> Boolean)? = null

    fun setCoinFilter(coinFilter: (Coin) -> Boolean) {
        this.coinFilter = coinFilter
    }

    override fun initData() {

    }

    override fun setEvent() {
        
    }
}