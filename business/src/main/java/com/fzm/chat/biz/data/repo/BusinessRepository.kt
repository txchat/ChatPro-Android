package com.fzm.chat.biz.data.repo

import com.fzm.chat.biz.data.BusinessDataSource
import com.fzm.chat.core.data.bean.ServerNode
import com.fzm.chat.core.logic.ServerManager
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2021/08/18
 * Description:
 */
class BusinessRepository(
    private val dataSource: BusinessDataSource
) : BusinessDataSource by dataSource {

    override suspend fun fetchServerList(): Result<ServerNode> {
        val result = dataSource.fetchServerList()
        if (result.isSucceed()) {
            if (!ServerManager.hasChatServer()) {
                // 自动设置默认聊天服务器
                result.data().servers.firstOrNull()?.also { node ->
                    ServerManager.changeLocalChatServer(node.address)
                }
            }
            if (!ServerManager.hasContractServer()) {
                // 自动设置默认合约服务器
                result.data().nodes.firstOrNull()?.also { node ->
                    ServerManager.changeContractServer(node.address)
                }
            }
        }
        return result
    }
}