package com.fzm.chat.wallet.ui

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager.widget.ViewPager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.base.EmptyFeatureFragment
import com.fzm.chat.biz.widget.FadePageTransformer
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.redpacket.ModuleAsset
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.wallet.R
import com.fzm.chat.wallet.databinding.FragmentWalletIndexBinding
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.alpha.EmptyWallet
import com.fzm.widget.dialog.EasyDialog
import com.zjy.architecture.ext.*
import com.zjy.architecture.util.ActivityUtils
import com.zjy.architecture.util.other.BarUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/10/13
 * Description:
 */
@Route(path = WalletModule.WALLET)
class WalletIndexFragment : BizFragment() {

    private val viewModel by viewModel<WalletViewModel>()

    companion object {
        fun create(showBack: Boolean, showNFT: Boolean): WalletIndexFragment {
            return WalletIndexFragment().apply {
                arguments = bundleOf(
                    "showBack" to showBack,
                    "showNFT" to showNFT
                )
            }
        }
    }

    @JvmField
    @Autowired
    var showBack: Boolean = false

    @JvmField
    @Autowired
    var showNFT: Boolean = true

    private var accountAssets = mutableListOf<ModuleAsset>()

    private val binding by init<FragmentWalletIndexBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        BarUtils.addPaddingTopEqualStatusBarHeight(requireContext(), binding.llHeader)
        ARouter.getInstance().inject(this)

        if (showNFT) {
            binding.tabLayout.visible()
            binding.vpAssets.apply {
                adapter = object : FragmentPagerAdapter(
                    childFragmentManager,
                    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
                ) {
                    override fun getCount() = 2

                    override fun getItem(position: Int): Fragment {
                        return if (position == 0)
                            TokenListFragment.create({
                                ARouter.getInstance().build(WalletModule.ASSET_DETAIL)
                                    .withSerializable("coin", it)
                                    .navigation()
                            }, itemLayout = R.layout.layout_wallet_index_assets)
                        else
//                            NFTListFragment.create({
//                                ARouter.getInstance().build(WalletModule.ASSET_DETAIL)
//                                    .withSerializable("coin", it)
//                                    .navigation()
//                            }, itemLayout = R.layout.layout_wallet_index_assets)
                            EmptyFeatureFragment.create()
                    }

                    override fun getPageTitle(position: Int): CharSequence {
                        return if (position == 0) "票券" else "NFT"
                    }
                }
                addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                    override fun onPageScrolled(
                        position: Int,
                        positionOffset: Float,
                        positionOffsetPixels: Int
                    ) {

                    }

                    override fun onPageSelected(position: Int) {
                        if (position == 0) {
                            binding.fabAddCoins.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(150)
                                .start()
                        } else {
                            binding.fabAddCoins.animate()
                                .scaleX(0f)
                                .scaleY(0f)
                                .setDuration(150)
                                .start()
                        }
                    }

                    override fun onPageScrollStateChanged(state: Int) {

                    }

                })
                setPageTransformer(false, FadePageTransformer())
                offscreenPageLimit = 1
                binding.tabLayout.setViewPager(this)
                binding.tabLayout.onPageSelected(0)
            }
        } else {
            binding.tabLayout.gone()
            binding.vpAssets.apply {
                adapter = object : FragmentPagerAdapter(
                    childFragmentManager,
                    BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
                ) {
                    override fun getCount() = 1

                    override fun getItem(position: Int): Fragment {
                        return TokenListFragment.create({
                            ARouter.getInstance().build(WalletModule.ASSET_DETAIL)
                                .withSerializable("coin", it)
                                .navigation()
                        }, itemLayout = R.layout.layout_wallet_index_assets)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAccountAssetByAddress()
    }

    override fun initData() {
        viewModel.loading.observe(viewLifecycleOwner) {
            setupLoading(it)
        }
        viewModel.accountAsset.observe(viewLifecycleOwner) {
            if (it.none { account -> account.balance != "0" }) {
                binding.extractLayout.gone()
            } else {
                accountAssets.clear()
                accountAssets.addAll(it)
                binding.extractLayout.visible()
                binding.tvExtractTips.mediumBold()
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                BWallet.get().current.collect {
                    if (viewModel.isLogin()) {
                        if (it is EmptyWallet) {
                            if (binding.fabAddCoins.isVisible) {
                                binding.fabAddCoins.gone()
                            }
                            EasyDialog.Builder()
                                .setHeaderTitle(resources.getString(R.string.biz_tips))
                                .setContent("需要重新登录以创建钱包")
                                .setBottomLeftText(resources.getString(R.string.biz_cancel))
                                .setBottomRightText("重新登录")
                                .setBottomRightClickListener { dialog ->
                                    dialog.dismiss()
                                    viewModel.performLogout()
                                    ActivityUtils.popUpTo("")
                                    ARouter.getInstance().build(MainModule.CHOOSE_LOGIN).navigation()
                                }
                                .create(requireContext())
                                .show()
                        } else {
                            if (binding.fabAddCoins.isGone) {
                                binding.fabAddCoins.visible()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setLeftVisible(showBack)
        binding.ctbTitle.setOnLeftClickListener { activity?.finish() }
        binding.ctbTitle.setOnRightClickListener {
            ARouter.getInstance().build(MainModule.QR_SCAN).navigation()
        }
        binding.fabAddCoins.singleClick {
            ARouter.getInstance().build(WalletModule.ADD_COINS)
                .withTransition(R.anim.biz_slide_bottom_in, 0)
                .navigation(requireContext())
        }

        binding.extractBtn.singleClick {
            lifecycleScope.launch {
                viewModel.withDrawAssets(accountAssets)
                binding.extractLayout.gone()
                toast("已提交提币申请，请稍后查看")
            }
        }
    }
}