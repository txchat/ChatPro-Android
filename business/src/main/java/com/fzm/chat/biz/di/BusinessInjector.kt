package com.fzm.chat.biz.di

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.data.BusinessDataSource
import com.fzm.chat.biz.data.impl.NetBusinessDataSource
import com.fzm.chat.biz.data.repo.BusinessRepository
import com.fzm.chat.biz.data.service.BusinessService
import com.fzm.chat.biz.db.AppDatabase
import com.fzm.chat.router.biz.BizModule
import com.zjy.architecture.di.Injector
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

/**
 * @author zhengjy
 * @since 2020/12/04
 * Description:
 */
@Route(path = BizModule.INJECTOR)
class BusinessInjector : Injector, IProvider {

    override fun inject() = module {
        single { AppDatabase.build(get()) }
        single<BusinessDataSource> { NetBusinessDataSource(get<Retrofit>().create(BusinessService::class.java)) }
        single { BusinessRepository(get()) }
        single(named("APP_NAME_EN")) { AppConfig.APP_NAME_EN }
    }

    override fun init(context: Context?) {

    }
}