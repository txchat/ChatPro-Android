package com.fzm.chat.conversation

import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.arch.connection.socket.ConnectionManager
import com.fzm.chat.R
import com.fzm.chat.biz.widget.FadePageTransformer
import com.fzm.chat.biz.base.*
import com.fzm.chat.biz.bus.BusEvent
import com.fzm.chat.biz.bus.LiveDataBus
import com.fzm.chat.biz.widget.pullheader.WechatPullContent
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.databinding.FragmentSessionBinding
import com.fzm.chat.databinding.LayoutNormalHeaderBinding
import com.fzm.chat.databinding.LayoutWalletHeaderBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.oa.OAModule
import com.fzm.chat.router.oa.OAService
import com.fzm.chat.router.route
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.widget.HomeActionPopup
import com.fzm.widget.ScrollPagerAdapter
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.zjy.architecture.ext.*
import com.zjy.architecture.util.other.BarUtils
import org.koin.android.ext.android.inject

/**
 * @author zhengjy
 * @since 2020/12/09
 * Description:
 */
@Route(path = MainModule.SESSION)
class SessionFragment : BizFragment(), View.OnClickListener {

    private val binding by init<FragmentSessionBinding>()

    private val connectionManager by inject<ConnectionManager>()

    private lateinit var person: SessionListFragment
    private lateinit var group: SessionListFragment

    private var privateUnread = 0
    private var groupUnread = 0

    private val delegate by inject<LoginDelegate>()
    private val oaService by route<OAService>(OAModule.SERVICE)

    private var normalHeader: LayoutNormalHeaderBinding? = null
    private var walletHeader: LayoutWalletHeaderBinding? = null

    /**
     * 旧的用户地址
     */
    private var oldAddress: String? = null
    /**
     * 旧的钱包模块启用状态
     */
    private var oldWalletState = false

    private val statusHeight by lazy { BarUtils.getStatusBarHeight(requireContext()) }

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        BarUtils.addPaddingTopEqualStatusBarHeight(requireContext(), binding.homeHeader)
        BarUtils.addPaddingTopEqualStatusBarHeight(requireContext(), binding.homeContent)
        binding.vpMessage.apply {
            setCanScroll(true)
            person = SessionListFragment.create(ChatConst.PRIVATE_CHANNEL)
            group = SessionListFragment.create(ChatConst.GROUP_CHANNEL)
            adapter = ScrollPagerAdapter(
                childFragmentManager,
                listOf(getString(R.string.chat_title_private_chat), getString(R.string.chat_title_group_chat)),
                listOf(person, group)
            )
            setPageTransformer(false, FadePageTransformer())
            offscreenPageLimit = 2
        }
        binding.tabLayout.setViewPager(binding.vpMessage)
        for (i in 0 until binding.tabLayout.tabCount) {
            binding.tabLayout.getMsgView(i).apply {
                strokeWidth = 0
                backgroundColor = resources.getColor(R.color.biz_red_tips)
            }
        }
        LiveDataBus.of(BusEvent::class.java).changeTab().observe(this) {
            if (it.tab == 0) {
                binding.vpMessage.currentItem = it.subTab
            }
        }
    }

    private fun updateHeaderView(address: String?) {
        if (oldAddress == address && !oldAddress.isNullOrEmpty()) {
            if (oldWalletState == FunctionModules.enableWallet) {
                return
            }
        }
        val header = binding.homeHeader.getChildAt(0)
        if (header != null) {
            oldAddress = address
            oldWalletState = FunctionModules.enableWallet
            val color = if (FunctionModules.enableWallet) {
                R.color.biz_wallet_accent_light
            } else {
                R.color.biz_color_primary
            }
            header.findViewById<ImageView>(R.id.iv_address_code).setImageBitmap(
                createQRCode(
                    AppConfig.shareCode(address),
                    200.dp,
                    Color.BLACK,
                    ResourcesCompat.getColor(resources, color, null)
                )
            )
            header.findViewById<TextView>(R.id.tv_address).text = address
        }
    }

    private val transition = TransitionSet().apply {
        ordering = TransitionSet.ORDERING_SEQUENTIAL
        addTransition(Fade(Fade.OUT))
        addTransition(ChangeBounds())
        addTransition(Fade(Fade.IN))
        this.duration = 200
        interpolator = DecelerateInterpolator()
    }

    override fun initData() {
        connectionManager.observeState(this) {
            // 防止socket状态变化动画频繁触发，导致首页下拉出现问题
            if (it) {
                if (!binding.tvDisconnect.isGone) {
                    TransitionManager.beginDelayedTransition(binding.root, transition)
                    binding.tvDisconnect.gone()
                }
            } else {
                if (!binding.tvDisconnect.isVisible) {
                    TransitionManager.beginDelayedTransition(binding.root, transition)
                    binding.tvDisconnect.visible()
                }
            }
        }
        person.setSessionsChangedListener { sessions ->
            privateUnread = sessions.filter {
                it.getType() == ChatConst.PRIVATE_CHANNEL && !it.isNoDisturb()
            }.sumOf {
                it.unreadNum()
            }
            LiveDataBus.of(BusEvent::class.java).unreadMessageNum().setValue(privateUnread + groupUnread)
            if (privateUnread > 0) {
                binding.tabLayout.showMsg(0, privateUnread)
            } else {
                binding.tabLayout.hideMsg(0)
            }
        }
        group.setSessionsChangedListener { sessions ->
            groupUnread = sessions.filter {
                it.getType() == ChatConst.GROUP_CHANNEL && !it.isNoDisturb()
            }.sumOf {
                it.unreadNum()
            }
            LiveDataBus.of(BusEvent::class.java).unreadMessageNum().setValue(privateUnread + groupUnread)
            if (groupUnread > 0) {
                binding.tabLayout.showMsg(1, groupUnread)
            } else {
                binding.tabLayout.hideMsg(1)
            }
        }
        FunctionModules.enableWalletLive.observe(this) { enable ->
            if (binding.homeHeader.childCount > 0) {
                binding.homeHeader.removeViewAt(0)
            }
            if (enable) {
                setupWalletHeader()
                binding.pullExtend.setBackgroundColor(resources.getColor(R.color.biz_wallet_accent))
                binding.homeHeader.addView(walletHeader?.root, 0)
                walletHeader?.root?.layoutParams = walletHeader?.root?.layoutParams?.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            } else {
                setupNormalHeader()
                binding.pullExtend.setBackgroundColor(resources.getColor(R.color.biz_color_accent))
                binding.homeHeader.addView(normalHeader?.root, 0)
                normalHeader?.root?.layoutParams = normalHeader?.root?.layoutParams?.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
            updateHeaderView(delegate.getAddress())
        }
    }

    private fun setupNormalHeader() {
        if (normalHeader == null) {
            normalHeader = LayoutNormalHeaderBinding.inflate(layoutInflater)
            normalHeader?.tvAddress?.setOnClickListener(this)
        }
    }

    private fun setupWalletHeader() {
        if (walletHeader == null) {
            walletHeader = LayoutWalletHeaderBinding.inflate(layoutInflater)
            walletHeader?.tvAddress?.setOnClickListener(this)
            walletHeader?.rlTransfer?.setOnClickListener(this)
            walletHeader?.rlWallet?.setOnClickListener(this)
        }
    }

    /**
     * 屏蔽会话列表点击
     */
    private fun blockClick(block: Boolean) {
        binding.vpMessage.setCanScroll(!block)
        binding.ivScan.isClickable = !block
        binding.ivSearch.isClickable = !block
        binding.ivAdd.isClickable = !block
        binding.tvDisconnect.isClickable = !block
        person.setBlockClick(block)
        group.setBlockClick(block)
    }

    override fun setEvent() {
        binding.ivScan.setOnClickListener(this)
        binding.ivSearch.setOnClickListener(this)
        binding.ivAdd.setOnClickListener(this)
        binding.tvDisconnect.setOnClickListener(this)
        // Header中的布局元素
        val openRate = binding.homeContent.getOpenRate()
        binding.homeContent.addOnStateChangedListener(object : WechatPullContent.OnStateChangedListener {
            override fun onStateChanged(state: Int) {
                when (state) {
                    WechatPullContent.CLOSED -> {
                        blockClick(false)
                    }
                    WechatPullContent.OPENING -> {
                        updateHeaderView(delegate.getAddress())
                        blockClick(true)
                        LiveDataBus.of(BusEvent::class.java).sessionPullState().postValue(state)
                    }
                    WechatPullContent.OPENED -> {
                        blockClick(true)
                    }
                    WechatPullContent.CLOSING -> {
                        blockClick(true)
                        LiveDataBus.of(BusEvent::class.java).sessionPullState().postValue(state)
                    }
                }
            }

            override fun onScrollOffset(view: View, offset: Int, maxOffset: Int) {
                val beforeOpenPercent = ((offset / maxOffset.toFloat()) / openRate).coerceAtMost(1f)
                binding.homeContent.setPadding(
                    0,
                    // 在拉开Header之前恢复WechatPullContent的paddingTop
                    ((1 - beforeOpenPercent) * statusHeight).toInt(),
                    0,
                    0
                )
            }
        })
    }

    override fun onBackPressedSupport(): Boolean {
        if (binding.homeContent.isOpen()) {
            binding.homeContent.close()
            return true
        }
        return super.onBackPressedSupport()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_disconnect -> {
                ARouter.getInstance().build(MainModule.SERVER_MANAGE).navigation()
            }
            R.id.iv_scan -> {
                v.checkSingle(1000) { ARouter.getInstance().build(MainModule.QR_SCAN).navigation() }
            }
            R.id.iv_search -> {
                ARouter.getInstance().build(MainModule.SEARCH_LOCAL).navigation()
            }
            R.id.iv_add -> {
                HomeActionPopup.create(requireContext(), this).show(binding.ivAdd)
            }
            // 弹窗中的按钮
            R.id.scan -> {
                v.checkSingle(1000) { ARouter.getInstance().build(MainModule.QR_SCAN).navigation() }
            }
            R.id.create_group -> {
                ARouter.getInstance().build(MainModule.CREATE_INVITE_GROUP).navigation()
            }
            R.id.join -> {
                ARouter.getInstance().build(MainModule.SEARCH_ONLINE).navigation()
            }
            R.id.qr_code -> {
                ARouter.getInstance().build(MainModule.QR_CODE).navigation()
            }
            R.id.create_company -> {
                oaService?.openCreateCompanyPage()
            }
            R.id.okr -> {
                oaService?.openOKRPage(null)
            }
            // Header中的布局元素
            R.id.tv_address -> {
                val data = ClipData.newPlainText("address", delegate.getAddress())
                requireContext().clipboardManager?.setPrimaryClip(data)
                toast(R.string.chat_tips_copy_address)
            }
            R.id.rl_transfer -> {
                ARouter.getInstance().build(MainModule.CONTACT_SELECT)
                    .withString("action", "transfer")
                    .navigation()
                binding.homeContent.closeWithNoAnimation()
                binding.homeContent.setPadding(0, statusHeight, 0, 0)
            }
            R.id.rl_wallet -> {
                ARouter.getInstance().build(WalletModule.WALLET_INDEX).navigation()
                binding.homeContent.closeWithNoAnimation()
                binding.homeContent.setPadding(0, statusHeight, 0, 0)
            }
        }
    }

    private fun createQRCode(
        content: String?,
        heightPix: Int,
        @ColorInt codeColor: Int,
        @ColorInt backgroundColor: Int
    ): Bitmap? {
        val hints = hashMapOf<EncodeHintType, Any>()
        hints[EncodeHintType.CHARACTER_SET] = "utf-8"
        //容错级别
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        //设置空白边距的宽度
        hints[EncodeHintType.MARGIN] = 0
        try {
            val bitMatrix =
                QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, heightPix, heightPix, hints)
            val pixels = IntArray(heightPix * heightPix)
            for (y in 0 until heightPix) {
                for (x in 0 until heightPix) {
                    if (bitMatrix[x, y]) {
                        pixels[y * heightPix + x] = codeColor
                    } else {
                        pixels[y * heightPix + x] = backgroundColor
                    }
                }
            }

            // 生成二维码图片的格式
            val bitmap = Bitmap.createBitmap(heightPix, heightPix, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, heightPix, 0, 0, heightPix, heightPix)
            return bitmap
        } catch (e: WriterException) {

        }
        return null
    }
}