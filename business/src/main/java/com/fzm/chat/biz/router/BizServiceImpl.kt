package com.fzm.chat.biz.router

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.FunctionModules
import com.fzm.chat.biz.data.repo.BusinessRepository
import com.fzm.chat.router.biz.BizService
import com.fzm.chat.router.biz.BizModule
import com.zjy.architecture.di.rootScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2020/12/04
 * Description:
 */
@Route(path = BizModule.SERVICE)
class BizServiceImpl : BizService {

    private val repository by rootScope.inject<BusinessRepository>()

    override fun fetchModuleState() {
        GlobalScope.launch(Dispatchers.Main.immediate) {
            repository.fetchModuleState().dataOrNull()?.also {
                FunctionModules.parseModulesState(it)
            }
        }
    }

    override fun fetchServerList() {
        GlobalScope.launch(Dispatchers.Main.immediate) {
            repository.fetchServerList()
        }
    }

    override fun getAmountScale(): Int = AppConfig.AMOUNT_SCALE

    override fun init(context: Context?) {

    }
}