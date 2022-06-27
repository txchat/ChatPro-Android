package com.fzm.chat.login.words

import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.bean.mnem.PWallet
import com.fzm.chat.bean.mnem.WalletBackup
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.CipherUtils
import com.fzm.chat.databinding.ActivityBackupMnemBinding
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.widget.mnem.MnemItemDecoration
import com.google.android.flexbox.FlexboxLayoutManager
import com.zjy.architecture.ext.bytes2Hex
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.screenSize
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.other.BarUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import walletapi.Walletapi

/**
 * @author zhengjy
 * @since 2020/12/10
 * Description:
 */
@Route(path = MainModule.BACKUP_MNEM)
class BackupMnemActivity : BizActivity() {

    @JvmField
    @Autowired
    var wallet: PWallet? = null

    private val loginDelegate by inject<LoginDelegate>()

    private lateinit var mResultAdapter: BaseQuickAdapter<WalletBackup, BaseViewHolder>
    private val mnemResultList = mutableListOf<WalletBackup>()

    private lateinit var mAdapter: BaseQuickAdapter<WalletBackup, BaseViewHolder>
    private val mnemList = mutableListOf<WalletBackup>()

    private val binding by init { ActivityBackupMnemBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        BarUtils.setStatusBarColor(this, Color.parseColor("#333649"), 0)
        BarUtils.setStatusBarLightMode(this, false)
        window?.navigationBarColor = Color.parseColor("#2B292F")
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        val mnemArrays = wallet?.mnem?.split(" ")
        if (mnemArrays != null) {
            for (i in mnemArrays.indices) {
                mnemList.add(WalletBackup(mnemArrays[i], false))
            }
            mnemList.shuffle()
        }

        if (wallet?.mnemType == PWallet.TYPE_CHINESE) {
            val layoutManager = FlexboxLayoutManager(this)
            binding.mnemResult.layoutManager = layoutManager
            binding.mnemResult.addItemDecoration(MnemItemDecoration())
        } else {
            val layoutManager = FlexboxLayoutManager(this)
            binding.mnemResult.layoutManager = layoutManager
        }
        mResultAdapter = object : BaseQuickAdapter<WalletBackup, BaseViewHolder>(
            R.layout.item_backup_mnem,
            mnemResultList
        ) {
            override fun convert(holder: BaseViewHolder, item: WalletBackup) {
                val view = holder.getView<TextView>(R.id.tv_tag)
                view.setTextColor(Color.WHITE)
                if (wallet?.mnemType == PWallet.TYPE_CHINESE) {
                    view.setPadding(0, 0, 0, 0)
                    view.layoutParams = (view.layoutParams as LinearLayout.LayoutParams).apply {
                        width = 40.dp
                        height = 40.dp
                        leftMargin = 8.dp
                    }
                } else {
                    view.setPadding(9.dp, 5.dp, 9.dp, 6.dp)
                    view.layoutParams = (view.layoutParams as LinearLayout.LayoutParams).apply {
                        width = LinearLayout.LayoutParams.WRAP_CONTENT
                        height = LinearLayout.LayoutParams.WRAP_CONTENT
                        leftMargin = 8.dp
                    }
                }
                view.text = item.mnem
            }
        }
        mResultAdapter.addChildClickViewIds(R.id.tv_tag)
        mResultAdapter.setOnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.tv_tag) {
                val backUp = adapter.getItem(position) as? WalletBackup
                backUp?.selected = false
                mnemResultList.remove(backUp)
                mResultAdapter.notifyItemRemoved(position)
                mAdapter.notifyDataSetChanged()
                checkButton()
            }
        }
        binding.mnemResult.adapter = mResultAdapter

        binding.rvMnem.layoutManager = FlexboxLayoutManager(this)
        mAdapter = object : BaseQuickAdapter<WalletBackup, BaseViewHolder>(
            R.layout.item_backup_mnem,
            mnemList
        ) {
            override fun convert(holder: BaseViewHolder, item: WalletBackup) {
                val view = holder.getView<TextView>(R.id.tv_tag)
                if (wallet?.mnemType == PWallet.TYPE_CHINESE) {
                    view.setPadding(0, 0, 0, 0)
                    view.layoutParams = (view.layoutParams as LinearLayout.LayoutParams).apply {
                        width = 40.dp
                        height = 40.dp
                        rightMargin = if (mnemList.indexOf(item) % 6 == 5) {
                            0
                        } else {
                            (context.screenSize.x - 34.dp - 6 * 40.dp) / 5
                        }
                    }
                } else {
                    view.setPadding(9.dp, 5.dp, 9.dp, 6.dp)
                    view.layoutParams = (view.layoutParams as LinearLayout.LayoutParams).apply {
                        width = LinearLayout.LayoutParams.WRAP_CONTENT
                        height = LinearLayout.LayoutParams.WRAP_CONTENT
                        rightMargin = 14.dp
                    }
                }
                view.isSelected = item.selected
                if (item.selected) {
                    view.setTextColor(Color.parseColor("#8E92A3"))
                } else {
                    view.setTextColor(Color.WHITE)
                }
                view.text = item.mnem
            }
        }
        mAdapter.addChildClickViewIds(R.id.tv_tag)
        mAdapter.setOnItemChildClickListener { _, view, position ->
            if (view.id == R.id.tv_tag) {
                val item = mnemList[position]
                if (!item.selected) {
                    item.selected = !item.selected
                    mnemResultList.add(item)
                    mResultAdapter.notifyDataSetChanged()
                    mAdapter.notifyDataSetChanged()
                    checkButton()
                }
            }
        }
        binding.rvMnem.adapter = mAdapter
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.btnOk.setOnClickListener {
            lifecycleScope.launch {
                loading(true)
                val mnemWithSpace = wallet?.mnem ?: ""
                val mnemCheck: String = getMnemString()
                val mnem = mnemWithSpace.replace(" ", "")
                if (mnem.isNotEmpty() && mnemCheck != mnem) {
                    toast(R.string.chat_login_mnem_wrong)
                    dismiss()
                    return@launch
                }
                val hdWallet = withContext(Dispatchers.IO) {
                    CipherUtils.getHDWallet(Walletapi.TypeBtyString, mnemWithSpace)
                }
                if (hdWallet == null) {
                    toast(R.string.chat_login_mnem_backup_fail)
                    dismiss()
                    return@launch
                }
                loginDelegate.loginSuspend(hdWallet.newKeyPub(0).bytes2Hex(), hdWallet.newKeyPriv(0).bytes2Hex(), mnemWithSpace)
                toast(R.string.chat_login_mnem_backup_success)
                ARouter.getInstance().build(AppModule.MAIN).navigation()
                dismiss()
                binding.root.postDelayed({ finish() }, 500)
            }
        }
    }

    private fun getMnemString(): String {
        val sb = StringBuilder()
        mnemResultList.forEach {
            sb.append(it.mnem)
        }
        return sb.toString()
    }

    private fun checkButton() {
        var clickEnable = true
        for (i in mnemList.indices) {
            if (!mnemList[i].selected) {
                clickEnable = false
                break
            }
        }
        if (clickEnable) {
            binding.btnOk.setTextColor(Color.WHITE)
        } else {
            binding.btnOk.setTextColor(Color.parseColor("#9EA2AD"))
        }
        binding.btnOk.isEnabled = clickEnable
    }
}