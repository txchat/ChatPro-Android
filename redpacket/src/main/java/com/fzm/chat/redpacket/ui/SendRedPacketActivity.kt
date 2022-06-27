package com.fzm.chat.redpacket.ui

import android.annotation.SuppressLint
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.base.EmptyTabFragment
import com.fzm.chat.biz.ui.PasswordVerifyFragment
import com.fzm.chat.biz.utils.MoneyInputFilter
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.bean.isGroup
import com.fzm.chat.redpacket.R
import com.fzm.chat.core.crypto.ECCUtils
import com.fzm.chat.core.crypto.ECCUtils.toBytes
import com.fzm.chat.core.data.platform
import com.fzm.chat.core.utils.mul
import com.fzm.chat.redpacket.RedPacketConfig
import com.fzm.chat.redpacket.data.bean.SendRedPacketParams
import com.fzm.chat.redpacket.databinding.ActivitySendRedPacketBinding
import com.fzm.chat.redpacket.toAsset
import com.fzm.chat.router.biz.PasswordVerifier
import com.fzm.chat.router.biz.ResultReceiver
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.redpacket.ModuleAsset
import com.fzm.chat.router.redpacket.RedPacketModule
import com.fzm.chat.router.redpacket.assetExec
import com.fzm.chat.router.route
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.widget.dialog.EasyDialog
import com.zjy.architecture.ext.bytes2Hex
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.toast
import com.zjy.architecture.ext.visible
import com.zjy.architecture.util.ArithUtils
import com.zjy.architecture.util.FinanceUtils
import com.zjy.architecture.util.KeyboardUtils
import com.zjy.architecture.widget.LoadingDialog
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

/**
 * @author zhengjy
 * @since 2021/08/24
 * Description:
 */
@Route(path = RedPacketModule.SEND_RED_PACKET)
class SendRedPacketActivity : BizActivity(), View.OnClickListener {

    companion object {
        val ONE_DAY = TimeUnit.DAYS.toSeconds(1)

        const val MAX_PACKET_NUM = 2000
    }

    /**
     * 红包类型
     * 0：单人红包
     * 1：拼手气红包
     * 2：固定金额红包
     */
    @JvmField
    @Autowired
    var packetType: Int = -1

    @JvmField
    @Autowired
    var token: ModuleAsset? = null

    /**
     * 红包发送对象
     */
    @JvmField
    @Autowired
    var target: ChatTarget? = null

    /**
     * 是否需要发送红包消息
     */
    @JvmField
    @Autowired
    var sendMsg: Boolean = true

    private var currentToken: ModuleAsset? = null

    private var editAmount = false

    private var amount: Double = 0.0
    private var remark: String = ""
    private var packetNum: Int = 0

    private var privateKey = ""
    private var publicKey = ""

    private var sendDialog: LoadingDialog? = null

    private val params: SendRedPacketParams
        get() = SendRedPacketParams(
            amount = amount.mul(AppConfig.AMOUNT_SCALE),
            assetExec = currentToken?.assetExec ?: "",
            assetSymbol = currentToken?.symbol ?: "",
            decimalPlaces = currentToken?.decimalPlaces ?: 2,
            // 红包过期时间为一天
            expiresTime = System.currentTimeMillis() / 1000 + ONE_DAY,
            remark = remark.ifEmpty { "恭喜发财，大吉大利！" },
            signPrikey = privateKey,
            signPubkey = publicKey,
            size = if (packetType == ChatConst.RED_PACKET_SINGLE) 1 else packetNum,
            // 群红包暂时不限制领取人
            toAddr = if (target!!.isGroup) /*groupUsers*/ emptyList() else listOf(target!!.targetId),
            // 单人红包和固定金额红包类型是同一种
            type = if (packetType == ChatConst.RED_PACKET_LUCKY) packetType else ChatConst.RED_PACKET_FAIR
        ).also { it.moduleAsset = currentToken }

    private val viewModel by viewModel<RedPacketViewModel>()

    private val binding by init<ActivitySendRedPacketBinding>()

    override val root: View
        get() = binding.root

    private fun checkParams() {
        requireNotNull(target) { "红包发送对象不能为空" }
        if (target!!.channelType == ChatConst.PRIVATE_CHANNEL && packetType != ChatConst.RED_PACKET_SINGLE) {
            toast("私聊只能发单人红包")
            finish()
            return
        }
        if (target!!.channelType == ChatConst.GROUP_CHANNEL && packetType == ChatConst.RED_PACKET_SINGLE) {
            toast("群聊不能发单人红包")
            finish()
            return
        }
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        binding.etAmount.filters = arrayOf(MoneyInputFilter(true))
        binding.etPacketNum.filters = arrayOf(MoneyInputFilter(false))
        checkParams()
        changeToken(token)
        if (packetType == -1) {
            packetType = if (target!!.channelType == ChatConst.PRIVATE_CHANNEL) {
                ChatConst.RED_PACKET_SINGLE
            } else {
                ChatConst.RED_PACKET_LUCKY
            }
        }
        when (packetType) {
            ChatConst.RED_PACKET_SINGLE -> {
                binding.tvGroupNum.gone()
                binding.slPacketNum.gone()
                binding.ivPacketType.gone()

                binding.tvAmountTips.text = "单个数额"
            }
            ChatConst.RED_PACKET_LUCKY -> {
                binding.tvGroupNum.visible()
                binding.slPacketNum.visible()
                binding.ivPacketType.visible()

                binding.tvAmountTips.text = "总数额"
            }

            ChatConst.RED_PACKET_FAIR -> {
                binding.tvGroupNum.visible()
                binding.slPacketNum.visible()
                binding.ivPacketType.visible()

                binding.tvAmountTips.text = "总数额"
            }
        }
        val showNFT = false
        if (showNFT) {
            binding.packetDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            val token by route<Fragment>(
                WalletModule.DRAWER_ASSETS,
                // 暂时不能发NFT红包
                bundleOf(
                    "showNFT" to showNFT,
                    "chain" to "BTY",
                    "platform" to RedPacketConfig.FULL_CHAIN.platform
                )
            ) {
                EmptyTabFragment.create()
            }
            (token as? ResultReceiver<Coin>?)?.setOnResultReceiver {
                changeToken(it.toAsset())
                binding.packetDrawer.closeDrawer(GravityCompat.END)
            }
            supportFragmentManager.commit { add(R.id.fcv_container, token!!) }
        } else {
            binding.packetDrawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            val token by route<Fragment>(
                WalletModule.DRAWER_ASSETS,
                // 暂时不能发NFT红包
                bundleOf(
                    "showNFT" to showNFT,
                    "chain" to "BTY",
                    "platform" to RedPacketConfig.FULL_CHAIN.platform
                )
            ) {
                EmptyTabFragment.create()
            }
            (token as? ResultReceiver<Coin>?)?.setOnResultReceiver {
                changeToken(it.toAsset())
                binding.packetDrawer.closeDrawer(GravityCompat.END)
            }
            supportFragmentManager.commit { add(R.id.fcv_container, token!!) }
        }

        checkSendEnabled()
    }

    override fun initData() {
        viewModel.loading.observe(this) { setupLoading(it) }
        if (target!!.channelType == ChatConst.GROUP_CHANNEL) {
            viewModel.fetchGroupUsers(target!!.targetId.toLong()).observe(this) { users ->
                binding.tvGroupNum.text = "本群共${users.size}人"
            }
        }
        viewModel.packetAsset.observe(this) {
            if (it.isEmpty()) return@observe
            if (token == null) {
                changeToken(it.firstOrNull { asset ->
                    asset.chain == RedPacketConfig.FULL_CHAIN
                })
            }
        }
        viewModel.sendResult.observe(this) {
            finish()
        }
        viewModel.sendError.observe(this) {
            toast(it.message ?: "红包发送失败")
        }
        viewModel.sendState.observe(this) { state ->
            when (state) {
                1 -> {
                    sendDialog?.dismiss()
                    LoadingDialog(this, false)
                        .setTipsText("正在转移资产到红包…")
                        .also { sendDialog = it }
                        .show()
                }
                2 -> sendDialog?.setTipsText("红包上链中…")
                3 -> sendDialog?.setTipsText("正在发送红包…")
                4 -> sendDialog?.setTipsText("红包上链中…")
                5 -> sendDialog?.setTipsText("发送失败，退回中…")
                6 -> sendDialog?.dismiss()
            }
        }

        viewModel.getRedPacketAssets()
    }

    @SuppressLint("SetTextI18n")
    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressed() }
        binding.ctbTitle.setOnRightClickListener {
            ARouter.getInstance().build(RedPacketModule.RED_PACKET_RECORD).navigation()
        }
        binding.tvSelectedToken.setOnClickListener(this)
        binding.ivPacketType.setOnClickListener(this)
        binding.tvAmountTips.setOnClickListener(this)
        binding.ivRemarkHelp.setOnClickListener(this)
        binding.sendRedPacket.setOnClickListener(this)
        binding.packetDrawer.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                
            }

            override fun onDrawerOpened(drawerView: View) {

            }

            override fun onDrawerClosed(drawerView: View) {
                
            }

            override fun onDrawerStateChanged(newState: Int) {
                
            }
        })
        binding.etAmount.addTextChangedListener(onTextChanged = { s: CharSequence?, start, _, count ->
            if (editAmount) return@addTextChangedListener
            editAmount = true
            var result: CharSequence = ""
            if (!s.isNullOrEmpty()) {
                result = FinanceUtils.formatString(s, currentToken?.decimalPlaces ?: 2)
                binding.etAmount.setText(result)
                binding.etAmount.setSelection(min(start + count, result.length))
            }
            amount = when {
                result.toString().isEmpty() -> 0.0
                else -> result.toString().toDouble()
            }
            val available = currentToken?.balance?.toDouble() ?: 0.0
            if (amount > available) {
                amount = available
                binding.etAmount.setText(FinanceUtils.getPlainNum(amount, currentToken?.decimalPlaces ?: 2))
                binding.etAmount.setSelection(binding.etAmount.text.length)
            }
            binding.tvTotal.text = FinanceUtils.getPlainNum(amount, currentToken?.decimalPlaces ?: 2)
            checkSendEnabled()
            editAmount = false
        })
        binding.etPacketNum.addTextChangedListener {
            val text = it?.toString() ?: ""
            packetNum = if (text.isEmpty()) 0 else text.toInt()
            checkSendEnabled()
        }
        binding.etRemark.addTextChangedListener {
            val text = it?.toString() ?: ""
            binding.tvRemarkCount.text = "${text.length}/20"
            remark = text
            checkSendEnabled()
        }
    }

    private fun changeToken(token: ModuleAsset?) {
        if (token == null) return
        currentToken = token
        binding.etAmount.setText("")
        binding.etPacketNum.setText("")
        token.apply {
            binding.tvSelectedToken.text = symbol
            binding.tvTokenName.text = symbol
            binding.tvToken.text = symbol

            binding.tvBalance.text = "可用 $balance $symbol"

            binding.tvTotal.text = FinanceUtils.getPlainNum(amount, currentToken?.decimalPlaces ?: 2)
        }
    }

    /**
     * 切换普通红包和拼手气红包
     */
    private fun switchPacketType() {
        if (packetType == ChatConst.RED_PACKET_LUCKY) {
            packetType = ChatConst.RED_PACKET_FAIR
            binding.ivPacketType.setImageResource(R.mipmap.icon_packet_type_fair)
            val drawable = ResourcesCompat.getDrawable(resources, R.drawable.ic_help, null)?.apply {
                setBounds(0, 0, minimumWidth, minimumHeight)
            }
            binding.tvAmountTips.setCompoundDrawables(null, null, drawable, null)
        } else if (packetType == ChatConst.RED_PACKET_FAIR) {
            packetType = ChatConst.RED_PACKET_LUCKY
            binding.ivPacketType.setImageResource(R.mipmap.icon_packet_type_lucky)
            binding.tvAmountTips.setCompoundDrawables(null, null, null, null)
        }
    }

    private fun checkSendEnabled() {
        binding.sendRedPacket.isEnabled = when (packetType) {
            ChatConst.RED_PACKET_SINGLE -> amount > 0
            else -> amount > 0 && binding.etPacketNum.text.isNotEmpty()
        }
    }

    override fun onBackPressedSupport() {
        if (binding.packetDrawer.isDrawerOpen(GravityCompat.END)) {
            binding.packetDrawer.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressedSupport()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_selected_token -> {
                binding.packetDrawer.openDrawer(GravityCompat.END)
                KeyboardUtils.hideKeyboard(v)
            }
            R.id.iv_packet_type -> {
                switchPacketType()
            }
            R.id.tv_amount_tips -> {
                if (packetType == ChatConst.RED_PACKET_FAIR) {
                    EasyDialog.Builder()
                        .setHeaderTitle(getString(R.string.biz_tips))
                        .setContent(getString(R.string.red_packet_total_amount_tips))
                        .setBottomLeftText(null)
                        .setBottomRightText(getString(R.string.biz_acknowledge))
                        .create(this)
                        .show()
                }
            }
            R.id.iv_remark_help -> {
                EasyDialog.Builder()
                    .setHeaderTitle(getString(R.string.biz_tips))
                    .setContent(getString(R.string.red_packet_remote_remark_tips))
                    .setBottomLeftText(null)
                    .setBottomRightText(getString(R.string.biz_acknowledge))
                    .create(this)
                    .show()
            }
            R.id.send_red_packet -> {
                if (packetType != ChatConst.RED_PACKET_SINGLE) {
                    if (packetNum > MAX_PACKET_NUM) {
                        toast("红包个数不能超过${MAX_PACKET_NUM}个")
                        return
                    }
                    val single = ArithUtils.div(amount, packetNum.toDouble())
                    val min = 10.0.pow(-(currentToken?.decimalPlaces?.toDouble() ?: 0.0))
                    if (single < min) {
                        toast("单份红包不能小于最小精度")
                        return
                    }
                }
                // 生成密钥
                val keyPair = ECCUtils.genECKeyPair()
                val private = keyPair.private.toBytes()
                // 不需要把私钥加密上传，因为收到红包的人都有私钥
                // privateKey = ECCUtils.encrypt(keyPair.private.toBytes(), keyPair.public).bytes2Hex()
                publicKey = keyPair.public.toBytes().bytes2Hex()

                if (viewModel.hasPassword()) {
                    val dialog by route<PasswordVerifyFragment>(
                        MainModule.VERIFY_ENC_PWD,
                        bundleOf("type" to 0)
                    )
                    dialog?.setOnPasswordVerifyListener(object :
                        PasswordVerifier.OnPasswordVerifyListener {
                        override fun onSuccess(password: String) {
                            viewModel.sendRedPacket(params, target!!, private.bytes2Hex())
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
                            viewModel.sendRedPacket(params, target!!, private.bytes2Hex())
                        }
                    })
                    dialog?.show(supportFragmentManager, "SET_ENC_PWD")
                }
            }
        }
    }
}