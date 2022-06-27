package com.fzm.chat.biz.data

import com.fzm.chat.biz.bean.ModuleState
import com.fzm.chat.core.data.bean.ServerNode
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2021/08/17
 * Description:
 */
interface BusinessDataSource {

    /**
     * 获取应用模块的启用状态
     */
    suspend fun fetchModuleState(): Result<List<ModuleState>>

    /**
     * 获取服务器地址列表
     */
    suspend fun fetchServerList(): Result<ServerNode>
}