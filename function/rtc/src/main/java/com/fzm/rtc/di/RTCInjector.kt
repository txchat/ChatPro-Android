package com.fzm.rtc.di

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider
import com.fzm.chat.core.rtc.calling.RTCCalling
import com.fzm.chat.router.rtc.RtcModule
import com.fzm.rtc.ui.RTCCallViewModel
import com.fzm.rtc.impl.RTCCallingImpl
import com.fzm.rtc.msg.LocalRTCMessageManager
import com.fzm.rtc.trtc.TRTCManager
import com.zjy.architecture.di.Injector
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * @author zhengjy
 * @since 2021/07/06
 * Description:
 */
@Route(path = RtcModule.INJECTOR)
class RTCInjector : Injector, IProvider {

    override fun inject() = module {
        // 预先创建，以接收音视频通话信令
        single<RTCCalling>(createdAtStart = true) { RTCCallingImpl(get(), get(), get()) }
        viewModel { RTCCallViewModel(get()) }
        single { LocalRTCMessageManager(get()) }
    }

    override fun init(context: Context?) {
        TRTCManager.init(context!!)
    }
}