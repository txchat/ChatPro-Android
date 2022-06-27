package com.fzm.chat.app

import android.app.Application
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.GlobalConfig
import com.fzm.chat.core.logic.ServerManager
import com.fzm.chat.router.biz.BizModule
import com.fzm.chat.router.biz.BizService
import com.fzm.chat.router.core.CoreModule
import com.fzm.chat.router.live.LiveModule
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.oa.OAModule
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.bugly.crashreport.CrashReport.UserStrategy
import com.fzm.chat.router.push.PushModule
import com.fzm.chat.router.redpacket.RedPacketModule
import com.fzm.chat.router.route
import com.fzm.chat.router.rtc.RtcModule
import com.fzm.chat.router.shop.ShopModule
import com.fzm.chat.router.wallet.WalletModule
import com.zjy.architecture.Arch
import com.zjy.architecture.ext.versionName
import com.zjy.architecture.util.ProcessUtil

/**
 * @author zhengjy
 * @since 2020/12/04
 * Description:
 */
class App : Application() {

    private val bizService by route<BizService>(BizModule.SERVICE)

    override fun onCreate() {
        super.onCreate()

        Arch.init(this, GlobalConfig.DEBUG, GlobalConfig.LOG_ENC_KEY, arrayOf(
            BizModule.INJECTOR, CoreModule.INJECTOR, MainModule.INJECTOR, PushModule.INJECTOR,
            RtcModule.INJECTOR, WalletModule.INJECTOR, RedPacketModule.INJECTOR, OAModule.INJECTOR,
            LiveModule.INJECTOR, ShopModule.INJECTOR
        ))
        setupBugly()
        // 加载上次服务器地址配置
        ServerManager.loadLastServer()

        bizService?.fetchModuleState()
        bizService?.fetchServerList()
    }

    private fun setupBugly() {
        // 获取当前包名
        val packageName = packageName
        // 获取当前进程名
        val processName = ProcessUtil.getCurrentProcessName(this)
        val strategy = UserStrategy(this).apply {
            isUploadProcess = processName == null || processName == packageName
            appChannel = "default"
            appVersion = versionName
            appPackageName = packageName
        }
        // 初始化Bugly
        CrashReport.initCrashReport(applicationContext, AppConfig.BUGLY_APP_ID, GlobalConfig.DEBUG, strategy)
    }

    override fun onTerminate() {
        super.onTerminate()
        Arch.release()
    }
}