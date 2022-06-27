package com.fzm.chat.wallet.ui

import android.content.ClipData
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.base.*
import com.fzm.chat.biz.widget.FadePageTransformer
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.wallet.R
import com.fzm.chat.wallet.data.qrCode
import com.fzm.chat.wallet.databinding.ActivityAssetDetailBinding
import com.fzm.chat.wallet.ui.transactions.TransactionListFragment
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.widget.ScrollPagerAdapter
import com.king.zxing.util.CodeUtils
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.*
import com.zjy.architecture.util.other.BarUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/08/09
 * Description:
 */
@Route(path = WalletModule.ASSET_DETAIL)
class AssetDetailActivity : BizActivity(), View.OnClickListener {

    @JvmField
    @Autowired
    var coin: Coin? = null

    private val currentCoin: Coin
        get() = coin ?: throw Exception("coin is empty")

    private val viewModel by viewModel<WalletViewModel>()

    private val binding by init<ActivityAssetDetailBinding>()

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        notchSupport(window)
        BarUtils.setStatusBarColor(
            this,
            ContextCompat.getColor(this, android.R.color.transparent),
            0
        )
        binding.ctbTitle.layoutParams = binding.ctbTitle.layoutParams.apply {
            height += BarUtils.getStatusBarHeight(instance)
        }
        BarUtils.addPaddingTopEqualStatusBarHeight(this, binding.ctbTitle)
        BarUtils.setStatusBarLightMode(this, true)
        window?.navigationBarColor = ContextCompat.getColor(this, R.color.biz_color_primary)
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        binding.vpTransactions.apply {
            adapter = ScrollPagerAdapter(
                supportFragmentManager,
                listOf("全部", "转账", "收款"),
                listOf(
                    TransactionListFragment.create(currentCoin, 0),
                    TransactionListFragment.create(currentCoin, 1),
                    TransactionListFragment.create(currentCoin, 2)
                )
            )
            setPageTransformer(false, FadePageTransformer())
            offscreenPageLimit = 2
        }
        binding.tabLayout.setViewPager(binding.vpTransactions)
        binding.ctbTitle.setTitle(currentCoin.name)
        binding.tvRecordTips.mediumBold()
        if (currentCoin.nickname.isNullOrEmpty()) {
            binding.tvCoin.text = currentCoin.name
        } else {
            val ssb = SpannableStringBuilder("${currentCoin.name}(${currentCoin.nickname})")
            ssb.setSpan(
                AbsoluteSizeSpan(14, true),
                ssb.length - currentCoin.nickname!!.length - 2,
                ssb.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ssb.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.biz_text_grey_light)),
                ssb.length - currentCoin.nickname!!.length - 2,
                ssb.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            binding.tvCoin.text = ssb
        }
        binding.tvCoin.mediumBold()
        binding.ivCode.setImageBitmap(CodeUtils.createQRCode(currentCoin.qrCode, 70.dp))
        binding.tvAddress.text = currentCoin.address
        binding.tvShopTransfer.setVisible(FunctionModules.enableShop)
    }

    override fun initData() {
        viewModel.asset.observe(this) {
            currentCoin.balance = it.balance
            binding.tvBalance.text = /*FinanceUtils.checkString(currentCoin.balance, currentCoin.decimalPlaces)*/currentCoin.balance
        }
        viewModel.getCoinBalance(currentCoin)
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.ctbTitle.setOnRightClickListener {
            ARouter.getInstance().build(MainModule.QR_SCAN).navigation()
        }
        binding.tvAddress.setOnClickListener(this)
        binding.tvTransfer.setOnClickListener(this)
        binding.tvReceipt.setOnClickListener(this)
        binding.tvShopTransfer.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_address -> {
                val data = ClipData.newPlainText("address", currentCoin.address)
                clipboardManager?.setPrimaryClip(data)
                toast("地址已复制到剪贴板")
            }
            R.id.tv_transfer -> {
                ARouter.getInstance().build(MainModule.CONTACT_SELECT)
                    .withString("symbol", currentCoin.name)
                    .withString("chain", currentCoin.chain)
                    .withString("platform", currentCoin.platform)
                    .withString("action", "transfer")
                    .navigation()
            }
            R.id.tv_receipt -> {
                ARouter.getInstance().build(WalletModule.WALLET_CODE)
                    .withSerializable("coin", coin)
                    .navigation()
            }
            R.id.tv_shop_transfer -> {
//                ARouter.getInstance().build(ShopModule.TRANSFER_SHOP)
//                    .withSerializable("token", currentCoin)
//                    .navigation()
                toast("暂未开放")
            }
        }
    }
}