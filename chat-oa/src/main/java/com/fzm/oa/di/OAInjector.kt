package com.fzm.oa.di

import android.content.Context
import android.net.Uri
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.QRCodeHelper
import com.fzm.chat.biz.ui.DeepLinkHelper
import com.fzm.chat.router.oa.OAModule
import com.fzm.oa.OAConfig
import com.fzm.oa.OADataSource
import com.fzm.oa.impl.NetOADataSource
import com.fzm.oa.impl.OARepository
import com.fzm.oa.net.OANetService
import com.zjy.architecture.di.Injector
import org.koin.dsl.module
import retrofit2.Retrofit

/**
 * @author zhengjy
 * @since 2021/09/04
 * Description:
 */
@Route(path = OAModule.INJECTOR)
class OAInjector : Injector, IProvider {

    override fun inject() = module {
        setupQRProcessor()
        single<OADataSource> { NetOADataSource(get<Retrofit>().create(OANetService::class.java)) }
        single { OARepository(get(), get()) }
    }

    private fun setupQRProcessor() {
        QRCodeHelper.registerQRProcessor(object : QRCodeHelper.Processor {
            override fun process(context: Context, result: String?): Boolean {
                val uri = Uri.parse(result)
                return if (uri.toString().contains(OAConfig.OA_WEB)) {
                    ARouter.getInstance().build(OAModule.WEB)
                        .withString("url", uri.toString())
                        .navigation()
                    true
                } else {
                    false
                }
            }
        })
    }

    override fun init(context: Context?) {
        DeepLinkHelper.registerWebUrl(
            OAConfig.OA_WEB,
            ARouter.getInstance().build(OAModule.WEB)
                .withTransition(0, 0)
        )
    }
}