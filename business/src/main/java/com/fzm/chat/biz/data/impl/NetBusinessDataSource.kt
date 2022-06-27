package com.fzm.chat.biz.data.impl

import com.fzm.chat.biz.bean.ModuleState
import com.fzm.chat.biz.data.BusinessDataSource
import com.fzm.chat.biz.data.service.BusinessService
import com.fzm.chat.core.data.bean.ServerNode
import com.zjy.architecture.data.Result
import com.zjy.architecture.ext.apiCall

/**
 * @author zhengjy
 * @since 2021/08/17
 * Description:
 */
class NetBusinessDataSource(
    private val service: BusinessService
) : BusinessDataSource {

    override suspend fun fetchModuleState(): Result<List<ModuleState>> {
        return apiCall { service.fetchModuleState() }
    }

    override suspend fun fetchServerList(): Result<ServerNode> {
        return apiCall { service.fetchServerList() }
    }
}