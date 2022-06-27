package com.fzm.push.di

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider
import com.fzm.chat.router.push.PushModule
import com.fzm.push.PushManager
import com.zjy.architecture.di.Injector
import com.zjy.architecture.util.ProcessUtil
import org.koin.dsl.module

/**
 * @author zhengjy
 * @since 2021/04/02
 * Description:
 */
@Route(path = PushModule.INJECTOR)
class PushInjector : Injector, IProvider {

    override fun inject() = module {

    }

    override fun init(context: Context) {
        // 获取当前包名
        val packageName = context.packageName
        // 获取当前进程名
        val processName = ProcessUtil.getCurrentProcessName(context)
        if (processName == null || processName == packageName) {
            Thread { PushManager.init(context) }.start()
        } else {
            PushManager.init(context)
        }
    }
}