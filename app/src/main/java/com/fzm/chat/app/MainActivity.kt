package com.fzm.chat.app

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.contains
import androidx.core.view.get
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.arch.connection.logic.MessageDispatcher
import com.fzm.chat.app.databinding.ActivityMainBinding
import com.fzm.chat.app.databinding.LayoutDialogImportOldAccountBinding
import com.fzm.chat.biz.base.*
import com.fzm.chat.biz.bus.BusEvent
import com.fzm.chat.biz.bus.LiveDataBus
import com.fzm.chat.biz.bus.event.ChangeTabEvent
import com.fzm.chat.biz.bus.event.EndPointLoginEvent
import com.fzm.chat.biz.ui.DeepLinkHelper
import com.fzm.chat.biz.utils.NotificationHelper
import com.fzm.chat.biz.widget.pullheader.WechatPullContent
import com.fzm.chat.core.backup.LocalAccountManager
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.logic.MessageSender
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.session.UserInfo
import com.fzm.chat.router.app.AppModule
import com.fzm.chat.router.core.CoreModule
import com.fzm.chat.router.core.CoreService
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.main.MainService
import com.fzm.chat.router.route
import com.fzm.chat.router.shop.ShopModule
import com.fzm.chat.router.shop.ShopService
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.widget.dialog.EasyDialog
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.*
import com.zjy.architecture.util.KeyboardUtils
import com.zjy.architecture.util.other.BadgeUtil
import com.zjy.architecture.util.other.BarUtils
import dtalk.biz.Biz
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2020/12/07
 * Description:
 */
@Route(path = AppModule.MAIN, extras = AppConst.NEED_LOGIN)
class MainActivity : BizActivity() {

    /**
     * 需要导入的本地账户
     */
    @JvmField
    @Autowired
    var importAddress: String? = null

    /**
     * 首页所包含的一级Fragment
     */
    private val fragmentConfig = mutableListOf<Pair<String, Bundle?>>(
        MainModule.SESSION to null,
        MainModule.CONTACT to null,
        MainModule.ACCOUNT to null,
    )

    private val walletPair = WalletModule.WALLET to bundleOf("showNFT" to true)

    private val otherLoginObserver = Observer<EndPointLoginEvent?> {
        if (it != null) {
            lifecycleScope.launch {
                delegate.logoutSuspend()
                ARouter.getInstance().build(MainModule.END_POINT_LOGIN)
                    .withString("deviceName", it.deviceName)
                    .withLong("datetime", it.datetime)
                    .navigation()
            }
        }
    }

    private var bindDialog: EasyDialog? = null

    private val viewModel by viewModel<MainViewModel>()

    private val binding by init {
        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        window.sharedElementsUseOverlay = false
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var unread: TextView
    private lateinit var msgItemView: BottomNavigationItemView

    private val delegate by inject<LoginDelegate>()
    private val dispatcher by inject<MessageDispatcher>()
    private val messageManager by inject<MessageSender>()
    private val mainService by route<MainService>(MainModule.SERVICE)
    private val coreService by route<CoreService>(CoreModule.SERVICE)
    private val shopService by route<ShopService>(ShopModule.SERVICE)
    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        notchSupport(window)
        BarUtils.setStatusBarColor(this, ContextCompat.getColor(this, android.R.color.transparent), 0)
        BarUtils.setStatusBarLightMode(this, true)
        window?.navigationBarColor = ContextCompat.getColor(this, R.color.biz_color_primary)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // 从后台恢复时检查更新
        binding.root.postDelayed({ mainService?.checkUpdate(this) }, 1000)
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        binding.vpMain.adapter = object :
            FragmentPagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                val fragment by route<Fragment>(fragmentConfig[position]) {
                    EmptyTabFragment.create()
                }
                return fragment!!
            }

            override fun getCount(): Int {
                return fragmentConfig.size
            }

            override fun getItemId(position: Int): Long {
                return fragmentConfig[position].first[position].toLong()
            }
        }
        binding.vpMain.offscreenPageLimit = 4
        binding.vpMain.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {}

            override fun onPageSelected(position: Int) {

            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        delegate.current.observe(this, object : Observer<UserInfo> {
            override fun onChanged(info: UserInfo) {
                if (info.isLogin()) {
                    if (info.phone.isNullOrEmpty() && info.email.isNullOrEmpty()) {
                        if (viewModel.showBind.value == true) {
                            bindDialog = EasyDialog.Builder()
                                .setHeaderTitle(getString(R.string.biz_tips))
                                .setBottomLeftText(getString(R.string.app_skip_bind_phone))
                                .setBottomRightText(getString(R.string.app_skip_bind_email))
                                .setContent(getString(R.string.app_skip_bind_tips))
                                .setBottomLeftClickListener { dialog ->
                                    dialog.dismiss()
                                    ARouter.getInstance().build(MainModule.SECURITY_SET)
                                        .withInt("bindType", ChatConst.PHONE)
                                        .navigation()
                                }
                                .setBottomRightClickListener { dialog ->
                                    dialog.dismiss()
                                    ARouter.getInstance().build(MainModule.SECURITY_SET)
                                        .withInt("bindType", ChatConst.EMAIL)
                                        .navigation()
                                }
                                .create(instance)
                            bindDialog?.show()
                            viewModel.showBind.value = false
                        }
                    } else {
                        if (bindDialog?.isShowing == true) {
                            bindDialog?.dismiss()
                        }
                        delegate.current.removeObserver(this)
                    }
                } else {
                    if (bindDialog?.isShowing == true) {
                        bindDialog?.dismiss()
                    }
                    delegate.current.removeObserver(this)
                }
            }
        })
        DeepLinkHelper.dispatchDeepLink(intent)
        // 首次启动主页面，请求更新用户信息
        delegate.updateInfo()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        importAddress = intent?.getStringExtra("importAddress")
        DeepLinkHelper.dispatchDeepLink(intent)
        importLocalAccount()
    }

    override fun initData() {
        LiveDataBus.of(BusEvent::class.java).unreadMessageNum().observe(this) {
            BadgeUtil.setBadgeCount(this, it)
            showMsgBadge(it)
        }
        LiveDataBus.of(BusEvent::class.java).changeTab().observe(this) {
            binding.vpMain.currentItem = it.tab
        }
        LiveDataBus.of(BusEvent::class.java).sessionPullState().observe(this) {
            when (it) {
                WechatPullContent.CLOSED -> {

                }
                WechatPullContent.OPENING -> {
                    binding.bottomNav.clearAnimation()
                    binding.bottomNav.gone()
                    binding.bottomNav.startAnimation(hideAnimation)
                }
                WechatPullContent.OPENED -> {

                }
                WechatPullContent.CLOSING -> {
                    binding.bottomNav.clearAnimation()
                    binding.bottomNav.visible()
                    binding.bottomNav.startAnimation(showAnimation)

                }
            }
        }
        LiveDataBus.of(BusEvent::class.java).endPointLogin().observeForever(otherLoginObserver)
        MessageSubscription.setOnShowNotification {
            NotificationHelper.showNotification(it)
        }
        viewModel.shopModule.observe(this) {
            if (it) {
                addShopTab()
            }
        }
        viewModel.walletModule.observe(this) {
            if (it) {
                addWalletTab()
            }
        }
        if (FunctionModules.enableShop) {
            viewModel.shopModule.value = true
        }
        if (FunctionModules.enableWallet) {
            viewModel.walletModule.value = true
        }
    }

    private fun addShopTab() {
        binding.bottomNav.menu.add(0, R.id.tab_shop, 3, R.string.app_tab_shop)
            .setIcon(R.drawable.app_tab_shop_selector)
    }

    private fun addWalletTab() {
        if (!fragmentConfig.contains(walletPair)) {
            fragmentConfig.add(fragmentConfig.size - 1, walletPair)
            binding.vpMain.adapter?.notifyDataSetChanged()
        }
        binding.bottomNav.menu.add(0, R.id.tab_wallet, 4, R.string.app_tab_wallet)
            .setIcon(R.drawable.app_tab_wallet_selector)
    }

    private val hideAnimation = TranslateAnimation(
        Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
        Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 1f
    ).apply {
        interpolator = DecelerateInterpolator()
        duration = 250
        fillAfter = true
    }

    private val showAnimation = TranslateAnimation(
        Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
        Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0f
    ).apply {
        interpolator = DecelerateInterpolator()
        duration = 250
        fillAfter = true
    }

    private fun showMsgBadge(number: Int?) {
        if (number == null) return
        if (!this::msgItemView.isInitialized) {
            val menuView = binding.bottomNav.getChildAt(0) as BottomNavigationMenuView
            msgItemView = menuView.findViewById(R.id.tab_session)
        }
        if (!this::unread.isInitialized) {
            unread = layoutInflater.inflate(R.layout.layout_unread_dot, msgItemView, false) as TextView
        }
        if (!msgItemView.contains(unread)) {
            msgItemView.addView(unread)
            unread.layoutParams = (unread.layoutParams as FrameLayout.LayoutParams).apply {
                gravity = Gravity.TOP or Gravity.END
                rightMargin = (msgItemView.measuredWidth / 2) - 20.dp
                topMargin = 5.dp
            }
        }
        when {
            number > 99 -> {
                if (BuildConfig.DEBUG) {
                    unread.text = "$number"
                } else {
                    unread.text = "99+"
                }
                unread.visible()
            }
            number > 0 -> {
                unread.text = "$number"
                unread.visible()
            }
            else -> unread.gone()
        }
    }

    override fun setEvent() {
        binding.bottomNav.run {
            // 去除icon着色
            itemIconTintList = null
            setOnNavigationItemSelectedListener {
                if (binding.bottomNav.isGone) return@setOnNavigationItemSelectedListener true
                when (it.itemId) {
                    R.id.tab_session -> binding.vpMain.setCurrentItem(0, false)
                    R.id.tab_contract -> binding.vpMain.setCurrentItem(1, false)
                    R.id.tab_shop -> {
                        shopService?.openShopHomePage(instance)
                        return@setOnNavigationItemSelectedListener false
                    }
                    R.id.tab_wallet -> {
                        binding.vpMain.setCurrentItem(2, false)
                    }
                    R.id.tab_account -> {
                        if (viewModel.walletModule.value == true) {
                            binding.vpMain.setCurrentItem(3, false)
                        } else {
                            binding.vpMain.setCurrentItem(2, false)
                        }
                    }
                }
                return@setOnNavigationItemSelectedListener true
            }
        }
        binding.vpMain.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                if (viewModel.shopModule.value == true) {
                    // 如果启用了商城模块，因为商城模块的特殊性（不创建单独的Fragment），导致position发生偏移
                    if (position > 1) {
                        binding.bottomNav.selectedItemId = binding.bottomNav.menu[position + 1].itemId
                    } else {
                        binding.bottomNav.selectedItemId = binding.bottomNav.menu[position].itemId
                    }
                } else {
                    binding.bottomNav.selectedItemId = binding.bottomNav.menu[position].itemId
                }
            }
        })
    }

    /**
     * 导入本地其他账户
     */
    private fun importLocalAccount() {
        importAddress?.also { account ->
            lifecycleScope.launch {
                val manager by inject<LocalAccountManager>()
                val layout = LayoutDialogImportOldAccountBinding.inflate(layoutInflater).apply {
                    cbSelect.setOnCheckedChangeListener { _, isChecked ->
                        etMsg.isEnabled = isChecked
                        if (isChecked) {
                            KeyboardUtils.showKeyboard(etMsg)
                        } else {
                            KeyboardUtils.hideKeyboard(etMsg)
                        }
                    }
                }
                EasyDialog.Builder()
                    .setHeaderTitle("导入好友")
                    .setContent("检测到原账号在本设备有备份的好友，是否导入好友至新账号？")
                    .setView(layout.root)
                    .setBottomLeftText(getString(R.string.biz_cancel))
                    .setBottomRightText("导入好友")
                    .setBottomRightClickListener {
                        lifecycleScope.launch {
                            val text by lazy(LazyThreadSafetyMode.NONE) { layout.etMsg.text.trim().toString() }
                            if (layout.etMsg.isEnabled && text.isEmpty()) {
                                toast("消息内容不能为空")
                            } else {
                                loading(false)
                                val result = manager.importByOldAddress(account)
                                dismiss()
                                if (result.isSucceed()) {
                                    it.dismiss()
                                    toast("导入成功")
                                    sendNotifyToFriends(text)
                                    LiveDataBus.of(BusEvent::class.java).changeTab().postValue(ChangeTabEvent(1, 0))

                                } else {
                                    toast(result.error().message ?: "")
                                }
                            }
                        }
                    }
                    .create(this@MainActivity)
                    .show()
                // 执行过一次导入后重置参数
                this@MainActivity.importAddress = null
            }
        }
    }

    /**
     * 给所有好友发送一条消息
     */
    private suspend fun sendNotifyToFriends(text: String?) {
        if (text.isNullOrEmpty()) return
        val liveData = database.friendUserDao().getFriendUserList(Contact.RELATION)
        liveData.observe(this, object : Observer<List<FriendUser>> {
            override fun onChanged(t: List<FriendUser>?) {
                if (t.isNullOrEmpty()) return
                liveData.removeObserver(this)
                t.forEach {
                    val message = ChatMessage.create(
                        delegate.getAddress(),
                        it.address,
                        ChatConst.PRIVATE_CHANNEL,
                        Biz.MsgType.Text,
                        MessageContent.text(text)
                    )
                    val servers = it.getServerList()
                    if (servers.isNotEmpty()) {
                        messageManager.send(servers[0].address, message)
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        coreService?.checkService()
        setCustomDensity(this)
        importLocalAccount()
    }

    override fun onDestroy() {
        super.onDestroy()
        LiveDataBus.of(BusEvent::class.java).endPointLogin().setValue(null)
        LiveDataBus.of(BusEvent::class.java).endPointLogin().removeObserver(otherLoginObserver)
        dispatcher.dispose()
        messageManager.dispose()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val intent = Intent()
            intent.action = Intent.ACTION_MAIN
            intent.addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}