package com.fzm.chat.biz.data.service

import com.fzm.chat.biz.bean.ModuleState
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.ServerNode
import com.zjy.architecture.net.HttpResult
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * @author zhengjy
 * @since 2021/08/17
 * Description:
 */
interface BusinessService {

    /**
     * 获取app模块启用状态
     */
    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CENTRALIZED_DOMAIN])
    @POST("/app/modules/all")
    suspend fun fetchModuleState(): HttpResult<List<ModuleState>>

    /**
     * 获取服务器地址列表
     */
    @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CENTRALIZED_DOMAIN])
    @GET("/disc/nodes")
    suspend fun fetchServerList(): HttpResult<ServerNode>
}