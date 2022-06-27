package com.fzm.chat.wallet.ui.transactions

import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.ui.DeepLinkHelper
import com.fzm.chat.biz.ui.PasswordVerifyFragment
import com.fzm.chat.biz.utils.MoneyInputFilter
import com.fzm.chat.biz.utils.highlightAddress
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.bean.chains
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.biz.PasswordVerifier
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.route
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.wallet.R
import com.fzm.wallet.sdk.utils.decimalPlaces
import com.fzm.chat.wallet.databinding.ActivityTransferBinding
import com.fzm.chat.wallet.ui.WalletViewModel
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.widget.dialog.EasyDialog
import com.zjy.architecture.ext.*
import com.zjy.architecture.util.FinanceUtils
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.min

/**
 * @author zhengjy
 * @since 2021/08/03
 * Description:转账收款页面
 */
@Route(path = WalletModule.TRANSFER)
class TransferActivity : BizActivity() {

    /**
     * 目标
     */
    @JvmField
    @Autowired(name = "target")
    var target: String? = null

    /**
     * 币种地址
     */
    @JvmField
    @Autowired
    var coinAddress: String? = null

    /**
     * 主链
     */
    @JvmField
    @Autowired
    var chain: String? = null

    /**
     * 平台码（平行链）
     */
    @JvmField
    @Autowired
    var platform: String? = null

    /**
     * 转账收款类型
     */
    @Deprecated("现在只有转账类型，没有收款")
    @JvmField
    @Autowired
    var transferType: Int = ChatConst.TRANSFER

    /**
     * 转账收款币种
     */
    @JvmField
    @Autowired
    var symbol: String? = null

    /**
     * 是否需要发送转账消息
     */
    @JvmField
    @Autowired
    var sendMsg: Boolean = true

    /**
     * 从聊天页面进入
     */
    @JvmField
    @Autowired
    var fromChat: Boolean = false

    /**
     * 当前选择的币种
     */
    var currentCoin: Coin? = null

    /**
     * 如果是主代币，则最大可用额度需要扣除手续费
     * 如果是Token，则最大可用额度不需要扣除手续费
     */
    private val available: Double
        get() = if (currentCoin != null && currentCoin?.chain == currentCoin?.name) {
            (currentCoin?.balance?.toDouble() ?: 0.0) - fee
        } else {
            (currentCoin?.balance?.toDouble() ?: 0.0)
        }

    private var contact: Contact? = null

    // 界面上填写的信息
    private var amount: Double = 0.0
    private var fee: Double = 0.0
    private var note: String = ""
    private var localNote: String = ""

    private var getFeeFail = false
    private var editAmount = false
    private var isSelf = false
    private var allowChange = true

    private val viewModel by viewModel<WalletViewModel>()
    private val delegate by inject<LoginDelegate>()

    private lateinit var mAdapter: BaseQuickAdapter<Coin, BaseViewHolder>
    private val binding by init { ActivityTransferBinding.inflate(layoutInflater) }

    private val showTitleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 80
        addUpdateListener {
            binding.llTarget.alpha = it.animatedFraction * 1f
            binding.llTarget.translationY = (1 - it.animatedFraction) * 15.dp
        }
    }

    private val hideTitleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 80
        addUpdateListener {
            binding.llTarget.alpha = (1 - it.animatedFraction) * 1f
            binding.llTarget.translationY = it.animatedFraction * 15.dp
        }
    }

    override val darkStatusColor = true

    override fun setSystemBar() {
        super.setSystemBar()
        window?.navigationBarColor = ContextCompat.getColor(this, R.color.biz_color_primary)
    }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        binding.etAmount.filters = arrayOf(MoneyInputFilter(true))
        if (delegate.getAddress() == target) {
            isSelf = true
            toast("不能给自己转账")
            binding.root.postDelayed({
                finish()
            }, 1000)
        }
        if (transferType == ChatConst.TRANSFER) {
            if (!symbol.isNullOrEmpty()) {
                binding.tvTransferType.text = "${symbol}转账"
            }
            binding.tvSubmit.text = "确认转账"
            binding.rlRemoteNote.visible()
        } else {
            if (!symbol.isNullOrEmpty()) {
                binding.tvTransferType.text = "${symbol}收款"
            }
            binding.tvSubmit.text = "确认收款"
            binding.rlRemoteNote.gone()
        }
        binding.drawerContent.rvCoin.layoutManager = LinearLayoutManager(this)
        mAdapter = object : BaseQuickAdapter<Coin, BaseViewHolder>(R.layout.item_transfer_assets, null) {
            override fun convert(holder: BaseViewHolder, item: Coin) {
                holder.getView<ImageView>(R.id.ic_coin).load(item.icon)
                holder.setText(R.id.tv_coin, item.name)
                holder.setText(R.id.tv_balance, item.balance)
            }
        }
        mAdapter.setOnItemClickListener { _, _, position ->
            changeSymbol(mAdapter.data[position])
            binding.transferDrawer.closeDrawer(GravityCompat.END)
        }
        binding.drawerContent.rvCoin.adapter = mAdapter
        if (!coinAddress.isNullOrEmpty()) {
            allowChange = false
            binding.tvTitleAddress.text = highlightAddress(coinAddress, resources.getColor(R.color.biz_color_accent))
            binding.tvAddress.text = highlightAddress(coinAddress, resources.getColor(R.color.biz_color_accent))
            binding.transferDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            binding.tvTransferType.setCompoundDrawables(null, null, null, null)
//        } else {
//            allowChange = true
//            binding.transferDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }

    override fun initData() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.getTargetInfo(target).observe(this) {
            contact = it
            binding.ivTitleAvatar.load(it.getDisplayImage(), R.mipmap.default_avatar_round)
            binding.tvTitleName.text = it.getDisplayName()

            binding.ivAvatar.load(it.getDisplayImage(), R.mipmap.default_avatar_round)
            binding.tvName.text = it.getDisplayName()
            currentCoin?.also { asset ->
                if (coinAddress.isNullOrEmpty()) {
                    coinAddress = it.chains[asset.chain]
                    if (coinAddress.isNullOrEmpty()) {
                        toast("找不到对方的${asset.name}地址")
                    }
                }
            }
            binding.tvTitleAddress.text = highlightAddress(coinAddress, resources.getColor(R.color.biz_color_accent))
            binding.tvAddress.text = highlightAddress(coinAddress, resources.getColor(R.color.biz_color_accent))
        }
        viewModel.moduleAssets.observe(this) {
            if (currentCoin == null) {
                if (symbol.isNullOrEmpty()) {
                    // 默认显示列表的第一个币种
                    changeSymbol(it.firstOrNull())
                } else {
                    val coin = it.find { asset ->
                        asset.name == symbol && asset.chain == chain && asset.platform == platform
                    } ?: it.firstOrNull()
                    changeSymbol(coin)
                }
            }
            mAdapter.setList(it)
        }
        viewModel.miner.observe(this) {
            if (it != null) {
                fee = it.average.toDouble()
                binding.tvFee.text = "${it.average} ${it.name}"
                currentCoin?.also { coin ->
                    if (coin.chain != coin.name) {
                        viewModel.checkChainAssetEnough(coin.chain, fee)
                    }
                }
                getFeeFail = false
            } else {
                binding.tvFee.text = "--"
                getFeeFail = true
                toast("${currentCoin?.name}手续费获取失败")
            }
        }
        viewModel.transferResult.observe(this) {
            toast("转账成功")
            finish()
            if (sendMsg) {
                val uri = Uri.parse("${DeepLinkHelper.APP_LINK}?type=chatNotification&address=$target&channelType=${ChatConst.PRIVATE_CHANNEL}")
                ARouter.getInstance().build(AppModule.MAIN)
                    .withParcelable("route", uri)
                    .withFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .navigation()
            }
        }
    }

    private fun changeSymbol(asset: Coin?) {
        if (asset != null) {
            contact?.also {
                coinAddress = it.chains[asset.chain]
                if (coinAddress.isNullOrEmpty()) {
                    toast("找不到对方的${asset.name}地址")
                }
            }
            viewModel.getRecommendedFee(asset.chain, asset.name)
            this.currentCoin = asset
            this.symbol = asset.name
            this.chain = asset.chain
            this.platform = asset.platform
            if (transferType == ChatConst.TRANSFER) {
                binding.tvTransferType.text = "${symbol}转账"
            } else {
                binding.tvTransferType.text = "${symbol}收款"
            }
            binding.tvBalance.text = "余额：${asset.balance} $symbol"
            binding.tvTitleAddress.text = highlightAddress(coinAddress, resources.getColor(R.color.biz_color_accent))
            binding.tvAddress.text = highlightAddress(coinAddress, resources.getColor(R.color.biz_color_accent))
        }
    }

    override fun setEvent() {
        binding.ivAvatar.setOnClickListener {
            ARouter.getInstance().build(WalletModule.WALLET_INDEX).navigation()
        }
        binding.ivBack.setOnClickListener { finish() }
        binding.tvTransferType.setOnClickListener {
            if (allowChange) {
                binding.transferDrawer.openDrawer(GravityCompat.END)
            }
        }
        binding.tvBalance.setOnClickListener {
            binding.etAmount.setText(currentCoin?.balance)
            binding.etAmount.setSelection(binding.etAmount.text.length)
        }
        binding.etAmount.addTextChangedListener(onTextChanged = { s: CharSequence?, start, _, count ->
            if (editAmount) return@addTextChangedListener
            editAmount = true
            var result: CharSequence = ""
            if (!s.isNullOrEmpty()) {
                result = FinanceUtils.formatString(s, currentCoin?.decimalPlaces ?: 2)
                binding.etAmount.setText(result)
                binding.etAmount.setSelection(min(start + count, result.length))
            }
            amount = when {
                result.toString().isEmpty() -> 0.0
                else -> result.toString().toDouble()
            }
            val max = available
            if (amount > max) {
                amount = max
                binding.etAmount.setText(FinanceUtils.getPlainNum(amount, currentCoin?.decimalPlaces ?: 2))
                binding.etAmount.setSelection(binding.etAmount.text.length)
            }
            binding.tvSubmit.isEnabled = amount > 0 && !coinAddress.isNullOrEmpty()
            editAmount = false
        })
        binding.etRemoteNote.addTextChangedListener {
            note = it?.toString() ?: ""
        }
        binding.etLocalNote.addTextChangedListener {
            localNote = it?.toString() ?: ""
        }
        binding.remoteNoteTips.setOnClickListener {
            EasyDialog.Builder()
                .setHeaderTitle(getString(R.string.biz_tips))
                .setContent(getString(R.string.wallet_remote_note_tips))
                .setBottomLeftText(null)
                .setBottomRightText(getString(R.string.biz_acknowledge))
                .create(this)
                .show()
        }
        binding.localNoteTips.setOnClickListener {
            EasyDialog.Builder()
                .setHeaderTitle(getString(R.string.biz_tips))
                .setContent(getString(R.string.wallet_local_note_tips))
                .setBottomLeftText(null)
                .setBottomRightText(getString(R.string.biz_acknowledge))
                .create(this)
                .show()
        }
        binding.slContainer.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            val threshHold = binding.ivAvatar.height + 15.dp
            if (scrollY > threshHold) {
                if (!showTitleAnimator.isRunning && binding.llTarget.alpha == 0f) {
                    hideTitleAnimator.cancel()
                    showTitleAnimator.start()
                }
            } else {
                if (!hideTitleAnimator.isRunning && binding.llTarget.alpha == 1f) {
                    showTitleAnimator.cancel()
                    hideTitleAnimator.start()
                }
            }
        })
        binding.tvSubmit.setOnClickListener {
            if (isSelf) return@setOnClickListener
            if (coinAddress.isNullOrEmpty()) return@setOnClickListener
            if (getFeeFail) {
                toast("请先获取手续费")
                return@setOnClickListener
            }
            currentCoin?.also { coin ->
                if (viewModel.enoughChainAssets.value == false) {
                    toast("${coin.chain}手续费不足")
                    return@also
                }
                if (viewModel.hasPassword()) {
                    val dialog by route<PasswordVerifyFragment>(
                        MainModule.VERIFY_ENC_PWD,
                        bundleOf("type" to 0)
                    )
                    dialog?.setOnPasswordVerifyListener(object :
                        PasswordVerifier.OnPasswordVerifyListener {
                        override fun onSuccess(password: String) {
                            viewModel.transfer(coin, target!!, coinAddress!!, coin.name, amount, fee, password, note, localNote, sendMsg)
                        }
                    })
                    dialog?.show(supportFragmentManager, "VERIFY_ENC_PWD")
                } else {
                    val dialog by route<PasswordVerifyFragment>(
                        MainModule.SET_ENC_PWD,
                        bundleOf("showWords" to 0)
                    )
                    dialog?.setOnPasswordVerifyListener(object :
                        PasswordVerifier.OnPasswordVerifyListener {
                        override fun onSuccess(password: String) {
                            viewModel.transfer(coin, target!!, coinAddress!!, coin.name, amount, fee, password, note, localNote, sendMsg)
                        }
                    })
                    dialog?.show(supportFragmentManager, "SET_ENC_PWD")
                }
            }
        }
    }

    override fun onBackPressedSupport() {
        if (binding.transferDrawer.isDrawerOpen(GravityCompat.END)) {
            binding.transferDrawer.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressedSupport()
        }
    }
}