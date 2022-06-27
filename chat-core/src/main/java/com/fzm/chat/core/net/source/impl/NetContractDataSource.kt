package com.fzm.chat.core.net.source.impl

import com.fzm.chat.core.data.bean.UserAddress
import com.fzm.chat.core.data.bean.UserResult
import com.fzm.chat.core.data.bean.ChatQuery
import com.fzm.chat.core.data.bean.ServerGroupInfo
import com.fzm.chat.core.data.contract.ContractRequest
import com.fzm.chat.core.data.contract.apiCall2
import com.fzm.chat.core.net.api.ContractService
import com.fzm.chat.core.net.source.ContractDataSource
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2020/12/22
 * Description:
 */
class NetContractDataSource(
    private val service: ContractService,
) : ContractDataSource {

    override suspend fun getUser(query: ChatQuery): Result<UserResult> {
        val request = ContractRequest.createQuery(query)
        return apiCall2 { service.getUser(request) }
    }

    override suspend fun getFriendList(query: ChatQuery): Result<UserAddress.Wrapper> {
        val request = ContractRequest.createQuery(query)
        return apiCall2 { service.getFriendList(request) }
    }

    override suspend fun getBlockList(query: ChatQuery): Result<UserAddress.Wrapper> {
        val request = ContractRequest.createQuery(query)
        return apiCall2 { service.getBlockList(request) }
    }

    override suspend fun getServerGroup(query: ChatQuery): Result<ServerGroupInfo.Wrapper> {
        val request = ContractRequest.createQuery(query)
        return apiCall2 { service.getServerGroup(request) }
    }

    override suspend fun modifyFriend(address: List<String?>, type: Int, groups: List<String>): Result<String> {
        val friendAddress = address.flatMap {
            listOf(mapOf("friendAddress" to it, "type" to type, "groups" to groups))
        }
        val request = ContractRequest.create(
            "chat.CreateRawUpdateFriendTx",
            mapOf("friends" to friendAddress)
        )
        return apiCall2 { service.modifyFriend(request) }
    }

    override suspend fun modifyBlock(address: List<String?>, type: Int): Result<String> {
        val friendAddress = address.flatMap {
            listOf(mapOf("targetAddress" to it, "type" to type))
        }
        val request = ContractRequest.create(
            "chat.CreateRawUpdateBlackTx",
            mapOf("list" to friendAddress)
        )
        return apiCall2 { service.modifyBlock(request) }
    }

    override suspend fun modifyServerGroup(groupInfo: List<ServerGroupInfo>): Result<String> {
        val groups = groupInfo.flatMap {
            listOf(mapOf("id" to it.id, "type" to it.type, "name" to it.name, "value" to it.value))
        }
        val request = ContractRequest.create(
            "chat.CreateRawUpdateServerGroupTx",
            mapOf("groups" to groups)
        )
        return apiCall2 { service.modifyServerGroup(request) }
    }
}