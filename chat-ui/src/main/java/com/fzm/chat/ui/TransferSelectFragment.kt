package com.fzm.chat.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.vm.ContactSelectViewModel
import org.koin.androidx.viewmodel.ext.android.getViewModel

/**
 * @author zhengjy
 * @since 2021/08/10
 * Description:
 */
class TransferSelectFragment : ContactSelectFragment() {

    companion object {
        fun create(symbol: String?, chain: String?, platform: String?): TransferSelectFragment {
            return TransferSelectFragment().apply {
                arguments = bundleOf(
                    "symbol" to symbol,
                    "chain" to chain,
                    "platform" to platform
                )
            }
        }
    }

    @JvmField
    @Autowired
    var symbol: String? = null

    @JvmField
    @Autowired
    var chain: String? = null

    @JvmField
    @Autowired
    var platform: String? = null

    override val viewModel: TransferViewModel
        get() = requireActivity().getViewModel()

    override fun initView(view: View, savedInstanceState: Bundle?) {
        super.initView(view, savedInstanceState)
        ARouter.getInstance().inject(this)
    }

    override fun setEvent() {
        super.setEvent()
        binding.ctlSelectOa.root.setOnClickListener {
            openTeamMemberSelectPage(mapOf("action" to ForwardSelectFragment.ACTION_FORWARD_MESSAGE))
        }
        mAdapter.setOnItemClickListener { _, _, position ->
            ARouter.getInstance().build(WalletModule.TRANSFER)
                .withString("target", mAdapter.data[position].contact.getId())
                .withInt("transferType", ChatConst.TRANSFER)
                .withString("symbol", symbol)
                .withString("chain", chain)
                .withString("platform", platform)
                .navigation()
            requireActivity().finish()
        }
    }

    override fun onSelectTeamMembers(users: List<String>) {
        ARouter.getInstance().build(WalletModule.TRANSFER)
            .withString("target", users[0])
            .withInt("transferType", ChatConst.TRANSFER)
            .withString("symbol", symbol)
            .navigation()
        requireActivity().finish()
    }

    class TransferViewModel(delegate: LoginDelegate) : ContactSelectViewModel(delegate) {

        override val channelFilter: Int
            get() = SESSION or PRIVATE
    }
}