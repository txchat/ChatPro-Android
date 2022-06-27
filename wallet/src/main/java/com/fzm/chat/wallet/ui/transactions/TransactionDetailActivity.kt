package com.fzm.chat.wallet.ui.transactions

import android.content.ClipData
import android.graphics.Paint
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Option
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.toMessagePO
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.biz.BizModule
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.wallet.R
import com.fzm.chat.wallet.WalletConfig
import com.fzm.chat.wallet.data.bean.TransactionInfo
import com.fzm.chat.wallet.databinding.ActivityTransactionDetailBinding
import com.fzm.chat.wallet.ui.LocalNoteDialog
import com.fzm.chat.wallet.ui.WalletViewModel
import com.zjy.architecture.ext.*
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/08/09
 * Description:
 */
@Route(path = WalletModule.TRANSACTION_DETAIL)
class TransactionDetailActivity : BizActivity(), View.OnClickListener {

    @JvmField
    @Autowired
    var txId: String = ""

    @JvmField
    @Autowired
    var chain: String = ""

    @JvmField
    @Autowired
    var platform: String = ""

    @JvmField
    @Autowired
    var symbol: String = ""

    @JvmField
    @Autowired
    var message: ChatMessage? = null

    private var txInfo: TransactionInfo? = null
    private var localNote: CharSequence? = null

    /**
     * 区块链浏览器地址
     */
    private var chainBrowser: String = ""

    private val delegate by inject<LoginDelegate>()
    private val viewModel by viewModel<WalletViewModel>()
    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val binding by init { ActivityTransactionDetailBinding.inflate(layoutInflater) }

    override val darkStatusColor: Boolean = true

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        viewModel.getBrowserUrl(platform)
    }

    override fun initData() {
        viewModel.loading.observe(this) {
            binding.refresh.isRefreshing = it.loading
        }
        viewModel.txInfo.observe(this) {
            if (it == null) {
                toast("找不到交易记录")
                return@observe
            }
            txInfo = it
            binding.llContainer.visible()
            val type = if (it.from == delegate.getAddress()) "-" else if (it.to == delegate.getAddress()) "+" else ""
            when (it.status) {
                TransactionInfo.FAIL -> {
                    binding.refresh.isEnabled = false
                    binding.tvStatus.text = "交易失败"
                    binding.ivStatus.setImageResource(R.drawable.ic_transaction_fail)
                }
                TransactionInfo.PENDING -> {
                    binding.refresh.isEnabled = true
                    binding.tvStatus.text = "确认中"
                    binding.ivStatus.setImageResource(R.drawable.ic_transaction_waiting)
                }
                TransactionInfo.SUCCESS -> {
                    binding.refresh.isEnabled = false
                    binding.tvStatus.text = "交易完成"
                    binding.ivStatus.setImageResource(R.drawable.ic_transaction_success)
                }
            }
            binding.tvAmount.text = "$type${it.value}"
            binding.tvCoin.text = symbol
            binding.fromAddress.text = it.from
            binding.targetRemark.text = it.getFriendName().ifEmpty { "无" }
            binding.toAddress.text = it.to
            if (type.isEmpty()) {
                binding.fromAddress.setTextColor(ResourcesCompat.getColor(resources, R.color.biz_red_tips, null))
                binding.toAddress.setTextColor(ResourcesCompat.getColor(resources, R.color.biz_red_tips, null))
                binding.tvWarning.visible()
            } else {
                binding.fromAddress.setTextColor(ResourcesCompat.getColor(resources, R.color.biz_text_grey_dark, null))
                binding.toAddress.setTextColor(ResourcesCompat.getColor(resources, R.color.biz_text_grey_dark, null))
                binding.tvWarning.gone()
            }
            binding.blockHeight.text = it.height.toString()
            binding.txHash.paint.apply {
                flags = Paint.UNDERLINE_TEXT_FLAG
                isAntiAlias = true
            }
            binding.txHash.text = it.txId
            binding.txTime.text = (it.blockTime * 1000L).format("yyyy/MM/dd HH:mm:ss")
            binding.remoteNote.text = it.note?.ifEmpty { "无" } ?: "无"
            message?.apply {
                if (msg.txStatus != it.status) {
                    msg.txAmount = it.value
                    msg.txInvalid = it.from != from || it.to != target
                    msg.txStatus = it.status
                    lifecycleScope.launch {
                        database.messageDao().insert(toMessagePO())
                        MessageSubscription.onMessage(Option.UPDATE_CONTENT, this@apply)
                    }
                }
            }
        }
        viewModel.getLocalNoteByHash(txId).observe(this) {
            localNote = it?.note
            binding.localNote.text = it?.note?.ifEmpty { "无" } ?: "无"
        }
        viewModel.browserUrl.observe(this) {
            if (it.isEmpty()) {
                toast("区块链浏览器地址获取失败")
            } else {
                chainBrowser = "$it$txId"
            }
        }
        viewModel.getTransactionById(chain, txId, symbol)
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.refresh.setColorSchemeColors(resources.getColor(R.color.biz_wallet_accent))
        binding.refresh.setOnRefreshListener {
            if (txInfo == null || txInfo?.status == TransactionInfo.PENDING) {
                viewModel.getTransactionById(chain, txId, symbol)
            } else {
                binding.refresh.isRefreshing = false
            }
        }
        binding.fromAddress.setOnClickListener(this)
        binding.toAddress.setOnClickListener(this)
        binding.txHash.setOnClickListener(this)
        binding.tvCopyHash.setOnClickListener(this)
        binding.localNote.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.from_address -> {
                txInfo?.apply {
                    val data = ClipData.newPlainText("from", from)
                    clipboardManager?.setPrimaryClip(data)
                    toast("转账地址已复制到剪贴板")
                }
            }
            R.id.to_address -> {
                txInfo?.apply {
                    val data = ClipData.newPlainText("to", to)
                    clipboardManager?.setPrimaryClip(data)
                    toast("收款地址已复制到剪贴板")
                }
            }
            R.id.tx_hash -> {
                txInfo?.apply {
                    ARouter.getInstance().build(BizModule.WEB_ACTIVITY)
                        .withString("url", chainBrowser)
                        .withBoolean("showTitle", true)
                        .withString("title", "区块链浏览器")
                        .navigation()
                }
            }
            R.id.tv_copy_hash -> {
                txInfo?.apply {
                    val data = ClipData.newPlainText("hash", txId)
                    clipboardManager?.setPrimaryClip(data)
                    toast("交易哈希已复制到剪贴板")
                }
            }
            R.id.local_note -> {
                LocalNoteDialog(this, localNote) {
                    viewModel.saveLocalNote(txId, it)
                    toast("保存成功")
                }.show()
            }
        }
    }
}