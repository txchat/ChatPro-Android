package com.fzm.chat.redpacket.di

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider
import com.fzm.chat.redpacket.ui.RedPacketViewModel
import com.fzm.chat.redpacket.data.RedPacketDataSource
import com.fzm.chat.redpacket.data.RedPacketRepository
import com.fzm.chat.redpacket.impl.NetRedPacketDataSource
import com.fzm.chat.redpacket.net.RedPacketService
import com.fzm.chat.router.redpacket.RedPacketModule
import com.zjy.architecture.di.Injector
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

/**
 * @author zhengjy
 * @since 2021/08/25
 * Description:
 */
@Route(path = RedPacketModule.INJECTOR)
class RedPacketInjector : Injector, IProvider {

    override fun inject() = module {
        viewModel {
            RedPacketViewModel(get(), get(), get(), get(), get(named("Wallet")), get(),
                get(named("Wallet")))
        }
        single<RedPacketDataSource> {
            NetRedPacketDataSource(
                get(named("Wallet")),
                get<Retrofit>(named("Contract")).create(RedPacketService::class.java),
                get(named("Wallet"))
            )
        }
        single { RedPacketRepository(get()) }
    }

    override fun init(context: Context?) {

    }
}