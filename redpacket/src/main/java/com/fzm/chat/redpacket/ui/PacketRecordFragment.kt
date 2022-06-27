package com.fzm.chat.redpacket.ui

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.redpacket.R
import com.fzm.chat.redpacket.data.bean.ReceiveInfo
import com.fzm.chat.redpacket.data.bean.RedPacketInfo
import com.fzm.chat.redpacket.data.bean.isExpired
import com.fzm.chat.redpacket.databinding.FragmentPacketReocrdBinding
import com.fzm.chat.router.redpacket.ModuleAsset
import com.fzm.chat.router.redpacket.RedPacketModule
import com.fzm.chat.router.redpacket.assetExec
import com.fzm.widget.divider.RecyclerViewDivider
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.singleClick
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.FinanceUtils
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat

/**
 * @author zhengjy
 * @since 2021/08/30
 * Description:
 */
class PacketRecordFragment<T> : BizFragment() {

    companion object {

        private const val SIZE_PAGE = 20
        private const val STATUS_SEND_PACKET = "status"

        fun <T> create(type: Int, year: String, month: String): PacketRecordFragment<T> {
            return PacketRecordFragment<T>().apply {
                arguments = bundleOf("type" to type, "year" to year, "month" to month)
            }
        }
    }

    /**
     * 0：收到红包
     * 1：发出红包
     */
    @JvmField
    @Autowired
    var type: Int = 0

    /**
     * 筛选年份
     */
    @JvmField
    @Autowired
    var year: String = ""

    /**
     * 筛选月份
     */
    @JvmField
    @Autowired
    var month: String = ""

    /**
     * 筛选币种名
     */
    private var asset: ModuleAsset? = null

    private var pageIndexId: String = ""

    private val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm")

    private val viewModel by viewModel<RedPacketViewModel>()
    private val delegate by inject<LoginDelegate>()

    private lateinit var mAdapter : BaseQuickAdapter<T, BaseViewHolder>

    private val binding by init<FragmentPacketReocrdBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        ARouter.getInstance().inject(this)
        mAdapter = object : BaseQuickAdapter<T, BaseViewHolder>(R.layout.item_my_packet_record, mutableListOf()) {
            override fun convert(holder: BaseViewHolder, item: T) {
               if (item is ReceiveInfo) {
                   // 收到红包
                   holder.setText(R.id.id_user, item.sender?.getDisplayName() ?: item.fromAddr)
                   holder.setText(R.id.time, sdf.format(item.createTime * 1000))
                   val ssb = SpannableStringBuilder("${FinanceUtils.getPlainNum(item.amount.toDouble() / AppConfig.AMOUNT_SCALE, item.decimalPlaces)}${item.assetSymbol}")
                   ssb.setSpan(
                       AbsoluteSizeSpan(14, true),
                       ssb.length - item.assetSymbol.length,
                       ssb.length,
                       Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                   )
                   holder.setText(R.id.amount, ssb)
                   holder.setGone(R.id.merge, item.type != ChatConst.RED_PACKET_LUCKY)
                   when(item.status) {
                       1 -> {
                           holder.setText(R.id.status, "成功")
                           holder.setTextColorRes(R.id.status, R.color.biz_text_grey_light)
                       }

                       2 -> {
                           holder.setText(R.id.status, "失败")
                           holder.setTextColorRes(R.id.status, R.color.biz_red_tips)
                       }
                   }
               }

               if (item is RedPacketInfo) {
                   // 发出红包
                   val packetType = when(item.type) {
                       ChatConst.RED_PACKET_LUCKY -> "拼手气红包"
                       else -> "普通红包"
                   }
                   holder.setText(R.id.id_user, packetType)
                   holder.setText(R.id.time, sdf.format(item.createTime * 1000))
                   val ssb = SpannableStringBuilder("${FinanceUtils.getPlainNum(item.amount.toDouble() / AppConfig.AMOUNT_SCALE, item.decimalPlaces)}${item.assetSymbol}")
                   ssb.setSpan(
                       AbsoluteSizeSpan(14, true),
                       ssb.length - item.assetSymbol.length,
                       ssb.length,
                       Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                   )
                   holder.setText(R.id.amount, ssb)
                   val status = when(item.status) {
                       1 -> {
                           if (item.isExpired) {
                               // 过期
                               "已过期 ${item.size - item.remain}/${item.size}"
                           } else {
                               "领取中 ${item.size - item.remain}/${item.size}"
                           }
                       }
                       2 -> "已领完 ${item.size}/${item.size}"
                       3 -> "已退回剩余红包"
                       else -> "未知"
                   }
                   holder.setGone(R.id.withdrawPacket, !(item.isExpired && item.status == 1))
                   holder.setText(R.id.status, status)
                   holder.getView<View>(R.id.withdrawPacket).singleClick {
                       viewModel.backRedPacket(item)
                   }
               }

               holder.itemView.singleClick {
                   val packetId = when (item) {
                       is ReceiveInfo -> {
                           item.packetId
                       }
                       is RedPacketInfo -> {
                           item.packetId
                       }
                       else -> {
                           ""
                       }
                   }
                   ARouter.getInstance().build(RedPacketModule.RED_PACKET_DETAIL)
                       .withString("packetId", packetId)
                       .navigation()
               }
            }

            override fun convert(holder: BaseViewHolder, item: T, payloads: List<Any>) {
                if (payloads.isEmpty()) {
                    convert(holder, item)
                    return
                }
                val bundle = payloads[0] as Bundle
                bundle.getString(STATUS_SEND_PACKET)?.also {
                    holder.getView<TextView>(R.id.status).text = "已退回剩余红包"
                    holder.getView<TextView>(R.id.status).setTextColor(resources.getColor(R.color.biz_text_grey_light))

                    holder.getView<TextView>(R.id.withdrawPacket).gone()
                }
            }
        }
        binding.rvRecords.adapter = mAdapter
        binding.rvRecords.addItemDecoration(
            RecyclerViewDivider(
                requireContext(),
                ContextCompat.getColor(requireContext(), R.color.biz_color_divider),
                0.5f,
                LinearLayoutManager.VERTICAL
            )
        )
        mAdapter.setEmptyView(R.layout.layout_empty_red_packet_record)
    }

    fun getStatisticInfo(year: String, month: String, asset: ModuleAsset?, indexId: String = "") {
        this.year = year
        this.month = month
        this.asset = asset
        this.pageIndexId = indexId
        val map = mutableMapOf<String, String>().apply {
            // 收发红包动作
            put("operation", type.toString())

            // 数量 -1：返回所有记录
            put("count", SIZE_PAGE.toString())

            // 币种
            asset?.let {
                put("assetExec", it.assetExec)
                put("assetSymbol", it.symbol)
            }

            // 地址
            put("addr", delegate.getAddress()?: "")

            // 年份
            put("year", year)

            // 月份
            if (month.isNotEmpty()) {
                put("month", month)
            }

            if (indexId.isNotEmpty()) {
                if (RedPacketRecordActivity.TYPE_RECEIVE_PACKET == type) {
                    put("receiveId", indexId)
                } else {
                    put("txIndex", indexId)
                }
            }
        }
        viewModel.getStatisticRecordList(map)
    }

    override fun initData() {
        viewModel.statisticInfo.observe(this) {
            if (it.operation == type) {
                val direction = if (RedPacketRecordActivity.TYPE_RECEIVE_PACKET == type) "收到" else "发出"
                if (pageIndexId.isEmpty()){
                    // 加载更多，返回的是当前分页的条目
                    if (it.sum != -1L) {
                        // 确定红包金额
                        binding.tvTypeTips.text = "共$direction${it.totalCount}个${asset?.symbol}红包，共计"
                        val ssb = SpannableStringBuilder("${FinanceUtils.getPlainNum(it.sum.toDouble() / AppConfig.AMOUNT_SCALE, asset?.decimalPlaces?: 0)}${asset?.symbol}")
                        ssb.setSpan(
                            AbsoluteSizeSpan(20, true),
                            ssb.length - (asset?.symbol?.length ?: 0),
                            ssb.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        binding.tvTotalNum.text = ssb
                    } else {
                        binding.tvTypeTips.text = "共${direction}红包"
                        binding.tvTotalNum.text = "${it.totalCount}个"
                    }
                }

                var list: List<T> =
                    if (type == RedPacketRecordActivity.TYPE_RECEIVE_PACKET) {
                        (it.receiveRecords ?: emptyList()) as List<T>
                    } else {
                        (it.redPackets ?: emptyList()) as List<T>
                    }
                binding.srlRefresh.finishRefresh()
                binding.srlRefresh.setEnableLoadMore(list.size == SIZE_PAGE)
                if (pageIndexId.isNotEmpty()) {
                    mAdapter.addData(list)
                    binding.srlRefresh.finishLoadMore()
                } else {
                    mAdapter.setList(list)
                }
            }
        }

        viewModel.loading.observe(this) { setupLoading(it) }

        viewModel.receiveError.observe(this) {
            toast(it.message ?: "未知错误")
        }

        viewModel.backPacketResult.observe(this) { packetId ->
           if (RedPacketRecordActivity.TYPE_SEND_PACKET == type) {
               mAdapter.data.forEachIndexed { index, item ->
                   if (packetId == (item as RedPacketInfo).packetId) {
                       mAdapter.notifyItemChanged(index, bundleOf(STATUS_SEND_PACKET to "3"))
                   }
               }
           }
        }

        getStatisticInfo(year, month, asset)
    }

    override fun setEvent() {
        binding.srlRefresh.setEnableLoadMore(false)
        binding.srlRefresh.setOnRefreshListener {
            getStatisticInfo(year, month, asset)
        }
        binding.srlRefresh.setOnLoadMoreListener {
            val size = mAdapter.data.size
            if (size > 0) {
                if (RedPacketRecordActivity.TYPE_RECEIVE_PACKET == type) {
                    val lastItem = mAdapter.data[size - 1] as ReceiveInfo
                    getStatisticInfo(year, month, asset, lastItem.receiveId)
                } else {
                    val lastItem = mAdapter.data[size - 1] as RedPacketInfo
                    getStatisticInfo(year, month, asset, lastItem.txIndex)
                }
            }
        }
    }
}