package com.fzm.chat.wallet.ui.coins

import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.FadePageTransformer
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.wallet.databinding.ActivityAddCoinBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2022/02/25
 * Description:
 */
@Route(path = WalletModule.ADD_COINS)
class AddCoinActivity : BizActivity() {

    private val viewModel by viewModel<CoinViewModel>()

    private val binding by init<ActivityAddCoinBinding>()

    private val fragments = mutableListOf<Fragment>()
    private val titles = mutableListOf<String>()

    override val root: View
        get() = binding.root

    override fun initView() {
        fragments.clear()
        fragments.add(HomeCoinFragment.create())
        titles.clear()
        titles.add("首页币种")
        binding.vpCoins.apply {
            adapter = object : FragmentStatePagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
                override fun getCount(): Int {
                    return fragments.size
                }

                override fun getItem(position: Int): Fragment {
                    return fragments[position]
                }

                override fun getPageTitle(position: Int): CharSequence {
                    return titles[position]
                }
            }
            setPageTransformer(false, FadePageTransformer())
            binding.stlChain.setViewPager(this)
        }
    }

    override fun initData() {
        viewModel.chainAssets.observe(this) { tabs ->
            tabs.forEach { bean ->
                bean.items?.apply {
                    if (this.isNotEmpty()) {
                        titles.add(bean.name)
                        fragments.add(ChainCoinFragment.create(this))
                    }
                }
            }
            binding.stlChain.notifyDataSetChanged()
            binding.vpCoins.adapter?.notifyDataSetChanged()
        }
        viewModel.getChainAssets()
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressedSupport() }
        binding.llSearchCoins.setOnClickListener {
            ARouter.getInstance().build(WalletModule.SEARCH_COINS)
                .withOptionsCompat(
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        it,
                        "searchView")
                )
                .navigation(this)
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.biz_slide_bottom_out)
    }
}