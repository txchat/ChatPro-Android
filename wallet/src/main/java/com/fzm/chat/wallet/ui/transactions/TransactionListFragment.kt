package com.fzm.chat.wallet.ui.transactions

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.wallet.R
import com.fzm.chat.wallet.data.bean.TransactionInfo
import com.fzm.chat.wallet.data.bean.isSend
import com.fzm.wallet.sdk.utils.tokenSymbol
import com.fzm.chat.wallet.databinding.FragmentTransactionListBinding
import com.fzm.chat.wallet.ui.WalletViewModel
import com.fzm.wallet.sdk.db.entity.Coin
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import com.zjy.architecture.ext.format
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * @author zhengjy
 * @since 2021/08/09
 * Description:
 */
class TransactionListFragment : BizFragment() {

    companion object {
        fun create(coin: Coin, type: Int): TransactionListFragment {
            return TransactionListFragment().apply {
                arguments = bundleOf(
                    "coin" to coin,
                    "type" to type,
                )
            }
        }
    }

    @JvmField
    @Autowired
    var coin: Coin? = null

    /**
     * 0-查询全部 1-查询转账 2-查询收款
     */
    @JvmField
    @Autowired
    var type: Int = 0

    private var index = 0L

    private lateinit var mAdapter: BaseQuickAdapter<TransactionInfo, BaseViewHolder>

    private val viewModel by lazy { requireActivity().getViewModel<WalletViewModel>() }

    private val binding by init<FragmentTransactionListBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        ARouter.getInstance().inject(this)
        binding.rvTx.layoutManager = LinearLayoutManager(requireContext())
        mAdapter = object : BaseQuickAdapter<TransactionInfo, BaseViewHolder>(R.layout.item_transaction, null) {
            override fun convert(holder: BaseViewHolder, item: TransactionInfo) {
                val s = if (item.isSend) "-" else "+"
                holder.setText(R.id.tv_amount, "$s${item.value}")
                holder.setText(R.id.tv_address, item.getAddress())
                holder.setText(R.id.tv_time, (item.blockTime * 1000).format("yyyy/MM/dd HH:mm"))
                when (item.status) {
                    TransactionInfo.FAIL -> {
                        holder.setText(R.id.tv_status, "失败")
                        holder.setTextColorRes(R.id.tv_status, R.color.biz_red_tips)
                    }
                    TransactionInfo.PENDING -> {
                        holder.setText(R.id.tv_status, "确认中")
                        holder.setTextColorRes(R.id.tv_status, R.color.biz_wallet_accent)
                    }
                    TransactionInfo.SUCCESS -> {
                        holder.setText(R.id.tv_status, "完成")
                        holder.setTextColorRes(R.id.tv_status, R.color.biz_text_grey_light)
                    }
                }
                if (item.contact != null) {
                    holder.setText(R.id.tv_remark, item.contact?.getDisplayName())
                    holder.setGone(R.id.tv_remark, false)
                } else {
                    holder.setGone(R.id.tv_remark, true)
                }
            }
        }
        mAdapter.setOnItemClickListener { _, _, position ->
            ARouter.getInstance().build(WalletModule.TRANSACTION_DETAIL)
                .withString("txId", mAdapter.data[position].txId)
                .withString("chain", coin!!.chain)
                .withString("platform", coin!!.platform)
                .withString("symbol", coin!!.tokenSymbol)
                .navigation()
        }
        binding.rvTx.adapter = mAdapter
    }

    override fun initData() {
        viewModel.observeTxList(type, viewLifecycleOwner) {
            val size = it.data.size
            index += size
            if (it.refresh) {
                binding.refresh.finishRefresh(true)
                if (size < ChatConfig.PAGE_SIZE) {
                    binding.refresh.finishLoadMoreWithNoMoreData()
                }
                mAdapter.setList(it.data)
            } else {
                mAdapter.addData(it.data)
                if (size < ChatConfig.PAGE_SIZE) {
                    binding.refresh.finishLoadMoreWithNoMoreData()
                } else {
                    binding.refresh.finishLoadMore(true)
                }
            }
        }
        viewModel.getTransactionList(coin!!, type, index, false)
    }

    override fun setEvent() {
        binding.refresh.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                index = 0
                viewModel.getTransactionList(coin!!, type, index)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                viewModel.getTransactionList(coin!!, type, index, false)
            }
        })
    }
}