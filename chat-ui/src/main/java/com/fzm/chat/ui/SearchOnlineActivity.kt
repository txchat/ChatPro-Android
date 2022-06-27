package com.fzm.chat.ui

import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.databinding.ActivitySearchOnlineBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.utils.StringUtils
import org.koin.android.ext.android.inject
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/11/30
 * Description:
 */
@Route(path = MainModule.SEARCH_ONLINE)
class SearchOnlineActivity : BizActivity() {

    private val delegate by inject<LoginDelegate>()
    private lateinit var mAdapter: BaseQuickAdapter<SearchOptionBean, BaseViewHolder>

    private val binding by init<ActivitySearchOnlineBinding>()

    override val root: View
        get() = binding.root

    override fun initView() {
        binding.tvMyAccount.text = getString(R.string.chat_tips_search_my_account, StringUtils.getDisplayAddress(delegate.getAddress() ?: ""))
        binding.rvSearchOptions.layoutManager = LinearLayoutManager(this)
        mAdapter = object :
            BaseQuickAdapter<SearchOptionBean, BaseViewHolder>(R.layout.item_search_option, options) {
            override fun convert(holder: BaseViewHolder, item: SearchOptionBean) {
                holder.setImageResource(R.id.iv_icon, item.icon)
                holder.setText(R.id.tv_name, item.name)
                holder.setText(R.id.tv_sub_name, item.subname)
            }
        }
        mAdapter.addChildClickViewIds(R.id.rl_container)
        mAdapter.setOnItemChildClickListener { _, v, position ->
            if (v.id == R.id.rl_container) {
                val route = options[position].route
                if (route != null) {
                    val path = route.first
                    val bundle = route.second
                    ARouter.getInstance().build(path).with(bundle).navigation()
                } else {
                    options[position].action?.invoke()
                }
            }
        }
        binding.rvSearchOptions.adapter = mAdapter
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressedSupport() }
        binding.llSearchFriends.setOnClickListener {
            ARouter.getInstance().build(MainModule.SEARCH_USER)
                .withOptionsCompat(
                    ActivityOptionsCompat.makeSceneTransitionAnimation(
                        this,
                        it,
                        "searchView")
                )
                .navigation(this)
        }
        binding.llMyAccount.setOnClickListener {
            ARouter.getInstance().build(MainModule.QR_CODE).navigation()
        }
    }

    private val options by lazy {
        mutableListOf(
            SearchOptionBean(
                R.drawable.ic_search_scan,
                getString(R.string.chat_action_search_scan),
                getString(R.string.chat_action_search_scan_tips),
                MainModule.QR_SCAN to null
            ),
            SearchOptionBean(
                R.drawable.ic_search_import_friends,
                getString(R.string.chat_action_search_import_friends),
                getString(R.string.chat_action_search_import_friends_tips),
                MainModule.IMPORT_LOCAL_ACCOUNT to null
            ),
        )
    }

    data class SearchOptionBean(
        val icon: Int,
        val name: String,
        var subname: String,
        val route: Pair<String, Bundle?>? = null,
        val action: (() -> Unit)? = null
    ) : Serializable
}