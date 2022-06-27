package com.fzm.chat.vm

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.biz.data.repo.BusinessRepository
import com.fzm.chat.biz.db.AppDatabase
import com.fzm.chat.biz.db.po.ServerHistory
import com.fzm.chat.biz.utils.ping
import com.fzm.chat.core.data.bean.ServerNode
import com.fzm.chat.core.repo.ContractRepository
import com.fzm.chat.core.session.LoginDelegate
import com.zjy.architecture.data.Result
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2021/01/20
 * Description:
 */
class ChooseServerViewModel(
    private val repository: BusinessRepository,
    private val contract: ContractRepository,
    private val database: AppDatabase
) : LoadingViewModel(), LoginDelegate by contract {

    private val _chatList = MutableLiveData<List<ServerNode.Node>>()
    val chatList: LiveData<List<ServerNode.Node>>
        get() = _chatList

    private val _contractList = MutableLiveData<List<ServerNode.Node>>()
    val contractList: LiveData<List<ServerNode.Node>>
        get() = _contractList

    private val _chatState = MutableLiveData<Int>()
    val chatState: LiveData<Int>
        get() = _chatState

    private val _contractState = MutableLiveData<Int>()
    val contractState: LiveData<Int>
        get() = _contractState

    private val _serverFetchFail = MutableLiveData<String>()
    val serverFetchFail: LiveData<String>
        get() = _serverFetchFail

    private val _addResult = MutableLiveData<Unit>()
    val addResult: LiveData<Unit>
        get() = _addResult

    private val _delResult = MutableLiveData<Unit>()
    val delResult: LiveData<Unit>
        get() = _delResult

    private var chatJob: Job? = null
    private var contractJob: Job? = null

    fun fetchServerList(refreshState: Boolean = true) {
        request<ServerNode> {
            onRequest {
                val result = repository.fetchServerList()
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
                    _serverFetchFail.postValue(result.error().message)
                    Result.Success(ServerNode(chat, contract))
                }
            }
            onSuccess {
                _chatList.value = it.servers
                _contractList.value = it.nodes
                if (refreshState) {
                    chatJob?.cancel()
                    contractJob?.cancel()
                    chatJob = launch {
                        watchChatServerState(it.servers)
                    }
                    contractJob = launch {
                        watchContractServerState(it.nodes)
                    }
                }
            }
        }
    }

    private suspend fun watchChatServerState(servers: List<ServerNode.Node>) {
        servers.forEachIndexed { index, node ->
            launch {
                val host = Uri.parse(node.address).host
                if (host != null) {
                    val state = ping(host)
                    if (node.active != state) {
                        node.active = state
                        _chatState.value = index
                    }
                }
            }
        }
    }

    private suspend fun watchContractServerState(nodes: List<ServerNode.Node>) {
        nodes.forEachIndexed { index, node ->
            launch {
                val host = Uri.parse(node.address).host
                if (host != null) {
                    val state = ping(host)
                    if (node.active != state) {
                        node.active = state
                        _contractState.value = index
                    }
                }
            }
        }
    }

    fun addHistory(id: Int, name: String, address: String, type: Int) {
        launch {
            loading(true)
            if (id != -1) {
                database.serverHistoryDao().update(id, name, address)
            } else {
                database.serverHistoryDao().insert(ServerHistory(name = name, address = address, type = type))
            }
            dismiss()
            _addResult.value = Unit
        }
    }

    fun deleteHistory(id: Int) {
        launch {
            loading(true)
            database.serverHistoryDao().delete(id)
            dismiss()
            _delResult.value = Unit
        }
    }
}