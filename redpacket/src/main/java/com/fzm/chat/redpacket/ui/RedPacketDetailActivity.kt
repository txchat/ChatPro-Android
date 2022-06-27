package com.fzm.chat.redpacket.ui

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.AppConst
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.core.data.platform
import com.fzm.chat.redpacket.data.bean.ReceiveInfo
import com.fzm.chat.redpacket.databinding.ActivityRedPacketDetailBinding
import com.fzm.chat.redpacket.R
import com.fzm.chat.redpacket.RedPacketConfig
import com.fzm.chat.redpacket.data.bean.RedPacketInfo
import com.fzm.chat.redpacket.data.bean.isExpired
import com.fzm.chat.router.biz.BizModule
import com.fzm.chat.router.redpacket.RedPacketModule
import com.fzm.chat.router.wallet.WalletModule
import com.zjy.architecture.ext.*
import com.zjy.architecture.util.FinanceUtils
import com.zjy.architecture.util.other.BarUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/08/27
 * Description:
 */
@Route(path = RedPacketModule.RED_PACKET_DETAIL)
class RedPacketDetailActivity : BizActivity() {

    /**
     * 红包id
     */
    @JvmField
    @Autowired
    var packetId: String = ""

    /**
     * 自己发的私人红包自己不能抢，只能查看信息
     */
    @JvmField
    @Autowired
    var viewOnly: Boolean = false

    private lateinit var mAdapter: BaseQuickAdapter<ReceiveInfo, BaseViewHolder>

    private val viewModel by viewModel<RedPacketViewModel>()

    private var packetInfo: RedPacketInfo? = null
    private var receiveInfo: ReceiveInfo? = null

    private var chainBrowser: String? = null

    private val binding by init<ActivityRedPacketDetailBinding>()

    override fun setSystemBar() {
        notchSupport(window)
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, R.color.biz_red_tips), 0)
        BarUtils.setStatusBarLightMode(this, false)
        window?.navigationBarColor = ContextCompat.getColor(this, R.color.biz_color_primary)
    }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)

        binding.rvReceive.layoutManager = LinearLayoutManager(this)
        mAdapter = object : BaseQuickAdapter<ReceiveInfo, BaseViewHolder>(R.layout.item_packet_receive_record, null), LoadMoreModule {
            override fun convert(holder: BaseViewHolder, item: ReceiveInfo) {
                holder.getView<ChatAvatarView>(R.id.image)
                    .load(item.receiver?.getDisplayImage(), R.mipmap.default_avatar_round)
                holder.setText(R.id.id_user, item.receiver?.getDisplayName() ?: item.addr)
                val ssb = SpannableStringBuilder("${FinanceUtils.getPlainNum(item.amount.toDouble() / AppConfig.AMOUNT_SCALE, item.decimalPlaces)}${item.assetSymbol}")
                ssb.setSpan(
                    AbsoluteSizeSpan(14, true),
                    ssb.length - item.assetSymbol.length,
                    ssb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                holder.setText(R.id.amount, ssb)
                holder.setText(R.id.time, (item.createTime * 1000).format("yyyy/MM/dd HH:mm:ss"))
            }
        }
        mAdapter.setOnItemClickListener { _, _, position ->
            if (chainBrowser.isNullOrEmpty()) {
                toast("区块链浏览器地址为空")
                return@setOnItemClickListener
            }
            val hash = mAdapter.data[position].receiveHash ?: ""
            ARouter.getInstance().build(BizModule.WEB_ACTIVITY)
                .withString("url", "$chainBrowser$hash")
                .withBoolean("showTitle", true)
                .withString("title", "区块链浏览器")
                .navigation()
        }
        mAdapter.loadMoreModule.setOnLoadMoreListener {
            mAdapter.data.lastOrNull()?.receiveId?.also {
                viewModel.getReceiveList(packetId, it)
            }
        }
        binding.rvReceive.adapter = mAdapter
    }

    override fun initData() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.packetInfo.observe(this) {
            packetInfo = it
            it.sender?.apply {
                binding.ivAvatar.load(getDisplayImage(), R.mipmap.default_avatar_round)
                binding.tvTitle.text = "${getDisplayName()}的红包"
            }
            binding.tvRemark.text = it.remark
            val total = FinanceUtils.getPlainNum(it.amount.toDouble() / AppConfig.AMOUNT_SCALE, it.decimalPlaces)
            if (!viewOnly) {
                binding.tvReceiveCount.text = "已领取${it.size - it.remain}/${it.size}，剩${it.remain}个"
                handlePacketState(packetInfo, receiveInfo)
            } else {
                if (it.remain > 0) {
                    binding.tvReceiveCount.text = "一个红包共$total${it.assetSymbol}"
                    if (it.isExpired) {
                        binding.tvAmount.text = "红包已过期"
                        binding.tvAmount.visible()
                        binding.tvGoAsset.gone()
                    } else {
                        binding.tvAmount.text = "等待对方领取"
                        binding.tvAmount.visible()
                        binding.tvGoAsset.gone()
                    }
                } else {
                    binding.tvReceiveCount.text = "一个红包共$total${it.assetSymbol}"
                    binding.tvAmount.gone()
                    binding.tvGoAsset.gone()
                }
            }
        }
        viewModel.myReceiveRecord.observe(this) {
            receiveInfo = it
            handlePacketState(packetInfo, receiveInfo)
        }
        viewModel.receiveList.observe(this) {
            mAdapter.addData(it)
            if (it.size < AppConst.PAGE_SIZE) {
                mAdapter.loadMoreModule.isEnableLoadMore = false
            } else {
                mAdapter.loadMoreModule.isEnableLoadMore = true
                mAdapter.loadMoreModule.loadMoreComplete()
            }
        }
        viewModel.browserUrl.observe(this) {
            if (it.isEmpty()) {
                toast("区块链浏览器地址获取失败")
            } else {
                chainBrowser = it
            }
        }
        viewModel.getBrowserUrl(RedPacketConfig.FULL_CHAIN.platform)
        viewModel.getRedPacketInfo(packetId)
        viewModel.getReceiveList(packetId, "")
        if (!viewOnly) {
            viewModel.getMyReceiveRecord(packetId)
        }
    }

    private fun handlePacketState(packetInfo: RedPacketInfo?, receiveInfo: ReceiveInfo?) {
        if (packetInfo != null && receiveInfo != null) {
            if (!receiveInfo.isEmpty) {
                val ssb = SpannableStringBuilder("${FinanceUtils.getPlainNum(receiveInfo.amount.toDouble() / AppConfig.AMOUNT_SCALE, packetInfo.decimalPlaces)}${receiveInfo.assetSymbol}")
                ssb.setSpan(
                    AbsoluteSizeSpan(20, true),
                    ssb.length - receiveInfo.assetSymbol.length,
                    ssb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                binding.tvAmount.text = ssb
                binding.tvAmount.visible()
                binding.tvGoAsset.visible()
            } else {
                if (packetInfo.remain == 0) {
                    binding.tvAmount.text = "红包已领完"
                    binding.tvAmount.visible()
                    binding.tvGoAsset.gone()
                } else if (packetInfo.isExpired) {
                    binding.tvAmount.text = "红包已过期"
                    binding.tvAmount.visible()
                    binding.tvGoAsset.gone()
                }
            }
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressed() }
        binding.ctbTitle.setOnRightClickListener {
            ARouter.getInstance().build(RedPacketModule.RED_PACKET_RECORD).navigation()
        }
        binding.tvGoAsset.setOnClickListener {
            ARouter.getInstance().build(WalletModule.WALLET_INDEX).navigation()
        }
    }
}