package com.fzm.chat.core.session.data

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.Field
import com.fzm.chat.core.data.bean.ChatQuery
import com.fzm.chat.core.data.bean.ServerGroupInfo
import com.fzm.chat.core.data.contract.ContractRequest
import com.fzm.chat.core.data.contract.apiCall2
import com.fzm.chat.core.net.source.TransactionSource
import com.fzm.chat.core.session.UserDataSource
import com.fzm.chat.core.session.UserInfo
import com.fzm.chat.core.session.api.UserService
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2020/12/04
 * Description:
 */
class NetUserDataSource(
    private val service: UserService,
    private val transaction: TransactionSource
) : UserDataSource {

    override suspend fun getUserInfo(address: String, publicKey: String, query: ChatQuery): Result<UserInfo> {
        val result = apiCall2 { service.getUser(ContractRequest.createQuery(query)) }
        return if (result.isSucceed()) {
            val user = result.data()
            val builder = UserInfo.Builder(address).setServers(user.chatServers.sortedBy { it.id.toInt() }.toMutableList())
            val map = mutableMapOf<String, String>()
            user.fields.forEach {
                when {
                    it.name == ChatConst.UserField.NICKNAME -> builder.setNickname(it.value)
                    it.name == ChatConst.UserField.AVATAR -> builder.setAvatar(it.value)
                    it.name == ChatConst.UserField.PUB_KEY -> builder.setPublicKey(it.value)
                    it.name == ChatConst.UserField.PHONE -> builder.setPhone(it.value)
                    it.name == ChatConst.UserField.EMAIL -> builder.setEmail(it.value)
                    it.name.startsWith(ChatConst.UserField.CHAIN_PREFIX) -> {
                        map[it.name.substring(ChatConst.UserField.CHAIN_PREFIX.length)] = it.value
                    }
                }
            }
            builder.setChainAddress(map)
            Result.Success(builder.build())
        } else {
            Result.Error(result.error())
        }
    }

    override suspend fun logout(address: String) {
        // do nothing
    }

    override suspend fun setUserInfo(fields: List<Field>): Result<String> {
        return transaction.handle {
            val request = ContractRequest.create("chat.CreateRawUpdateUserTx", mapOf("fields" to fields))
            apiCall2 { service.updateUser(request) }
        }
    }

    override suspend fun modifyServerGroup(groupInfo: List<ServerGroupInfo>): Result<String> {
        return transaction.handle {
            val groups = groupInfo.flatMap {
                listOf(mapOf("id" to it.id, "type" to it.type, "name" to it.name, "value" to it.value))
            }
            val request = ContractRequest.create(
                "chat.CreateRawUpdateServerGroupTx",
                mapOf("groups" to groups)
            )
            apiCall2 { service.modifyServerGroup(request) }
        }
    }

}