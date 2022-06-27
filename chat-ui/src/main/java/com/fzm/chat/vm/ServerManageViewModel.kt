package com.fzm.chat.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.biz.db.AppDatabase
import com.fzm.chat.biz.db.po.ServerHistory
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.ServerGroupInfo
import com.fzm.chat.core.data.bean.ServerNode
import com.fzm.chat.core.repo.ContractRepository
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.arch.connection.socket.ConnectionManager
import com.fzm.chat.biz.data.repo.BusinessRepository
import com.zjy.architecture.data.Result
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request

/**
 * @author zhengjy
 * @since 2021/02/22
 * Description:
 */
class ServerManageViewModel(
    private val repository: ContractRepository,
    private val discover: BusinessRepository,
    private val database: AppDatabase,
    val connection: ConnectionManager,
    delegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by delegate {

    private val _serverGroups by lazy { MutableLiveData<List<ServerGroupInfo>>() }
    val serverGroups: LiveData<List<ServerGroupInfo>>
        get() = _serverGroups

    private val _addResult by lazy { MutableLiveData<Any>() }
    val addResult: LiveData<Any>
        get() = _addResult

    private val _delResult by lazy { MutableLiveData<Any>() }
    val delResult: LiveData<Any>
        get() = _delResult

    private val _editResult by lazy { MutableLiveData<Any>() }
    val editResult: LiveData<Any>
        get() = _editResult

    private val _chatList = MutableLiveData<List<ServerNode.Node>>()
    val chatList: LiveData<List<ServerNode.Node>>
        get() = _chatList

    fun getServerGroupList() {
        request<Unit> {
            onRequest {
                val list = repository.getServerGroup()
                _serverGroups.value = list
                Result.Success(Unit)
            }
        }
    }

    fun addServerGroup(name: String, address: String) {
        request<String> {
            onRequest {
                database.serverHistoryDao().insert(ServerHistory(name = name, address = address, type = ChatConst.CHAT_SERVER))
                repository.modifyServerGroup(listOf(ServerGroupInfo("", 1, name, address)))
            }
            onSuccess {
                repository.updateInfo()
                _addResult.value = it
            }
        }
    }

    fun deleteServerGroup(id: String) {
        request<String> {
            onRequest {
                repository.modifyServerGroup(listOf(ServerGroupInfo(id, 2, "", "")))
            }
            onSuccess {
                repository.updateInfo()
                _delResult.value = it
            }
        }
    }

    fun editServerGroup(id: String, name: String, address: String) {
        request<String> {
            onRequest {
                database.serverHistoryDao().insert(ServerHistory(name = name, address = address, type = ChatConst.CHAT_SERVER))
                repository.modifyServerGroup(listOf(ServerGroupInfo(id, 3, name, address)))
            }
            onSuccess {
                repository.updateInfo()
                _editResult.value = it
            }
        }
    }

    fun fetchServerList() {
        request<ServerNode>(false) {
            onRequest {
                val result = discover.fetchServerList()
                val chat = database.serverHistoryDao().getChatServerHistory().map {
                    ServerNode.Node(it.name, it.address).apply { id = it.id }
                }
                val contract = database.serverHistoryDao().getContractServerHistory().map {
                    ServerNode.Node(it.name, it.address).apply { id = it.id }
                }
                if (result.isSucceed()) {
                    val data = result.data()
                    Result.Success(ServerNode(data.servers + chat, data.nodes + contract))
                } else {
                    Result.Success(ServerNode(chat, contract))
                }
            }
            onSuccess {
                _chatList.value = it.servers
            }
        }
    }
}