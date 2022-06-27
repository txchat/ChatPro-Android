package com.fzm.chat.account

import android.content.Intent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.utils.encryptAccount
import com.fzm.chat.biz.utils.encryptAddress
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.core.backup.LocalAccountType
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.enum.CodeType
import com.fzm.chat.core.data.po.LocalAccount
import com.fzm.chat.core.utils.formatMnemonic
import com.fzm.chat.databinding.ActivityImportLocalAccountBinding
import com.fzm.chat.databinding.LayoutVerifyAddressMnemonicBinding
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.main.MainModule
import com.fzm.widget.dialog.EasyDialog
import com.fzm.widget.verify.CodeLength
import com.fzm.widget.verify.VerifyCodeDialog
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.KeyboardUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/12/01
 * Description:
 */
@Route(path = MainModule.IMPORT_LOCAL_ACCOUNT)
class ImportLocalAccountActivity : BizActivity() {

    private val viewModel by viewModel<ImportLocalAccountViewModel>()
    private lateinit var adapter: BaseQuickAdapter<LocalAccount, BaseViewHolder>

    private val binding by init<ActivityImportLocalAccountBinding>()

    override val root: View
        get() = binding.root

    override val darkStatusColor: Boolean get() = true

    override fun initView() {
        binding.rvLocalAccount.layoutManager = LinearLayoutManager(this)
        adapter = object : BaseQuickAdapter<LocalAccount, BaseViewHolder>(
            R.layout.item_import_local_account,
            null
        ) {
            override fun convert(holder: BaseViewHolder, item: LocalAccount) {
                val address = encryptAccount(item.address)
                holder.setText(R.id.tv_name, item.nickname.ifEmpty { address })
                holder.getView<ChatAvatarView>(R.id.avatar)
                    .load(item.avatar, R.mipmap.default_avatar_round)
                holder.setText(R.id.tv_address, address)
                if (item.phone.isNullOrEmpty()) {
                    holder.setGone(R.id.rl_phone, true)
                } else {
                    holder.setGone(R.id.rl_phone, false)
                    holder.setText(R.id.tv_phone, item.phone)
                }
                if (item.email.isNullOrEmpty()) {
                    holder.setGone(R.id.rl_email, true)
                } else {
                    holder.setGone(R.id.rl_email, false)
                    holder.setText(R.id.tv_email, item.email)
                }
            }
        }
        adapter.addChildClickViewIds(R.id.rl_mnem, R.id.rl_phone, R.id.rl_email)
        adapter.setOnItemChildClickListener { _, view, position ->
            val item = adapter.data[position]
            when (view.id) {
                R.id.rl_mnem -> {
                    val mnemView = LayoutVerifyAddressMnemonicBinding.inflate(layoutInflater)
                    mnemView.tvAddress.text = "验证地址 ${encryptAddress(item.address)} 的助记词"
                    EasyDialog.Builder()
                        .setHeaderTitle("助记词验证")
                        .setHeaderTitleColor(ResourcesCompat.getColor(resources, R.color.biz_text_grey_dark, null))
                        .setCancelable(false)
                        .setView(mnemView.root)
                        .setBottomLeftText(getString(R.string.biz_cancel))
                        .setBottomRightText(getString(R.string.biz_confirm))
                        .setBottomRightClickListener {
                            val mnem = mnemView.etMnem.text.trim().toString()
                            viewModel.importAccount(mnem.formatMnemonic(), LocalAccountType.BACKUP_MNEMONIC, item.addressHash)
                        }
                        .create(this)
                        .show()
                    mnemView.etMnem.postDelayed({
                        KeyboardUtils.showKeyboard(mnemView.etMnem)
                    }, 300)
                }
                R.id.rl_phone -> {
                    VerifyCodeDialog.Builder(this, this, CodeLength.MEDIUM)
                        .setPhone(item.phone!!)
                        .setOnSendCodeListenerSuspend {
                            viewModel.sendCodeSuspend(item.phone!!, ChatConst.PHONE, CodeType.EXPORT)
                        }
                        .setOnCodeCompleteListener { _, _, code ->
                            viewModel.verifyPhoneExport(item.phone!!, code, item.address, item.addressHash)
                        }
                        .build().show()
                }
                R.id.rl_email -> {
                    VerifyCodeDialog.Builder(this, this, CodeLength.MEDIUM)
                        .setPhone(item.email!!)
                        .setOnSendCodeListenerSuspend {
                            viewModel.sendCodeSuspend(item.email!!, ChatConst.EMAIL, CodeType.EXPORT)
                        }
                        .setOnCodeCompleteListener { _, _, code ->
                            viewModel.verifyEmailExport(item.email!!, code, item.address, item.addressHash)
                        }
                        .build().show()
                }
            }
        }
        adapter.setHeaderView(TextView(this).apply {
            text = "选择导入以下账号的好友"
            setTextColor(ResourcesCompat.getColor(resources, R.color.biz_text_grey_light, null))
            height = 50.dp
            width = ViewGroup.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER_VERTICAL
            setPadding(15.dp, 0, 15.dp, 0)
        })
        binding.rvLocalAccount.adapter = adapter
        adapter.setEmptyView(R.layout.layout_empty_common)
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.accountList.observe(this) {
            adapter.setList(it)
        }
        viewModel.verifyResult.observe(this) {
            viewModel.importAccount(it.first, it.second, it.third)
        }
        viewModel.importResult.observe(this) {
            dismiss()
            toast("导入成功")
            ARouter.getInstance().build(AppModule.MAIN)
                .withFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .navigation(this)
            finish()
        }
    }

    override fun initData() {
        viewModel.getAccountList()
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressedSupport() }
    }
}