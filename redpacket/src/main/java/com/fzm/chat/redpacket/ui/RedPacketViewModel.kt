package com.fzm.chat.redpacket.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.AppConst
import com.fzm.chat.core.chain.waitForChainResult
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.ChatTarget
import com.fzm.chat.core.data.bean.Option
import com.fzm.chat.core.data.bean.isGroup
import com.fzm.chat.core.data.bean.isSuccess
import com.fzm.chat.core.data.execer
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.toMessagePO
import com.fzm.chat.core.data.po.*
import com.fzm.chat.core.logic.MessageSender
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.core.net.source.TransactionSource
import com.fzm.chat.core.repo.GroupRepository
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.mapResult
import com.fzm.chat.core.utils.mul
import com.fzm.chat.redpacket.RedPacketConfig
import com.fzm.chat.redpacket.data.RedPacketRepository
import com.fzm.chat.redpacket.data.bean.*
import com.fzm.chat.redpacket.data.bean.RedPacketInfo.Companion.FAKE_PACKET
import com.fzm.chat.redpacket.exception.RedPacketFallbackException
import com.fzm.chat.redpacket.exception.RedPacketStateException
import com.fzm.chat.router.redpacket.ModuleAsset
import com.fzm.chat.router.route
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.router.wallet.WalletService
import com.fzm.wallet.sdk.BWallet
import com.zjy.architecture.data.Result
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request
import com.zjy.architecture.util.logI
import dtalk.biz.Biz
import dtalk.biz.msg.Msg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import walletapi.ChainClient

/**
 * @author zhengjy
 * @since 2021/08/25
 * Description:
 */
class RedPacketViewModel(
    private val repository: RedPacketRepository,
    private val groupRepo: GroupRepository,
    val delegate: LoginDelegate,
    private val manager: ContactManager,
    private val transaction: TransactionSource,
    private val messageSender: MessageSender,
    private val client: ChainClient,
) : LoadingViewModel() {

    companion object {
        const val INITIAL_DELAY = 2000L
        const val QUERY_PERIOD = 800L
    }

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val walletService by route<WalletService>(WalletModule.SERVICE)

    private val _packetAssets by lazy { MutableLiveData<List<ModuleAsset>>() }
    val packetAsset: LiveData<List<ModuleAsset>>
        get() = _packetAssets

    private val _packetInfo by lazy { MutableLiveData<RedPacketInfo>() }
    val packetInfo: LiveData<RedPacketInfo>
        get() = _packetInfo

    private val _receiveList by lazy { MutableLiveData<List<ReceiveInfo>>() }
    val receiveList: LiveData<List<ReceiveInfo>>
        get() = _receiveList

    private val _myReceiveRecord by lazy { MutableLiveData<ReceiveInfo>() }
    val myReceiveRecord: LiveData<ReceiveInfo>
        get() = _myReceiveRecord

    private val _statisticInfo by lazy { MutableLiveData<StatisticInfo>() }
    val statisticInfo: LiveData<StatisticInfo>
        get() = _statisticInfo

    private val _backPacketResult by lazy { MutableLiveData<String>() }
    val backPacketResult: LiveData<String>
        get() = _backPacketResult

    private val _sendResult by lazy { MutableLiveData<String>() }
    val sendResult: LiveData<String>
        get() = _sendResult

    private val _sendError by lazy { MutableLiveData<Throwable>() }
    val sendError: LiveData<Throwable>
        get() = _sendError

    private val _receiveResult by lazy { MutableLiveData<String>() }
    val receiveResult: LiveData<String>
        get() = _receiveResult

    private val _receiveError by lazy { MutableLiveData<Throwable>() }
    val receiveError: LiveData<Throwable>
        get() = _receiveError

    private val _sendState by lazy { MutableLiveData<Int>() }
    val sendState: LiveData<Int>
        get() = _sendState

    private val _receiveState by lazy { MutableLiveData<Int>() }
    val receiveState: LiveData<Int>
        get() = _receiveState

    private val _browserUrl = MutableLiveData<String>()
    val browserUrl: LiveData<String>
        get() = _browserUrl

    init {
        client.setCfg(RedPacketConfig.FULL_CHAIN.execer(""), RedPacketConfig.FULL_CHAIN.execer("none"), ChatConfig.NO_BALANCE_PRIVATE_KEY)
    }

    fun hasPassword() = delegate.preference.hasChatPassword()

    fun sendRedPacket(params: SendRedPacketParams, target: ChatTarget, privateKey: String) {
        launch {
            logI("zhengjy start", (System.nanoTime() / 1000000).toString())
            _sendState.value = 1
            flow {
                // 资产转移到红包合约
                emit(Unit)
            }.map {
                val tx1 = repository.transferRedPacket(params.moduleAsset!!, params.amount)
                // 用红包合约中的资产发送红包
                val tx2 = repository.sendRedPacket(params)
                if (tx1.isSucceed() && tx2.isSucceed()) {
                    transaction.handleForRawHash2("redpacket") {
                        val fee = (walletService?.getDefaultFee() ?: 0.0) * 3
                        transaction.createTxGroup(tx1.data(), tx2.data(), fee.mul(AppConfig.AMOUNT_SCALE))
                    }
                } else {
                    Result.Error(RedPacketFallbackException("交易创建失败"))
                }
            }.mapResult({ RedPacketFallbackException("红包发送请求失败", it) }) {
                _sendState.value = 4
                // 等待区块链确认结果
                val result = waitForChainResult(INITIAL_DELAY, QUERY_PERIOD, 20, checker = { tx -> tx.isSuccess }) {
                    transaction.queryTransaction(it)
                }
                if (result.isSucceed()) return@mapResult it
                throw RedPacketFallbackException("红包确认失败，请到区块链浏览器上查看", result.error())
            }.catch {
                logI("zhengjy   end", (System.nanoTime() / 1000000).toString())
                _sendState.value = 6
                _sendError.value = it
            }.collect {
                logI("zhengjy   end", (System.nanoTime() / 1000000).toString())
                val contact = if (target.isGroup) {
                    manager.getGroupInfo(target.targetId)
                } else {
                    manager.getUserInfo(target.targetId)
                }
                val servers = contact.getServerList()
                if (servers.isNotEmpty()) {
                    val url = servers[0].address
                    val payload = MessageContent.redPacket(
                        it,
                        params.moduleAsset!!.chain,
                        if (params.moduleAsset!!.isCoins) Msg.CoinType.Coins_VALUE else Msg.CoinType.Token_VALUE,
                        params.moduleAsset!!.symbol,
                        params.remark,
                        params.bizPacketType,
                        privateKey,
                        params.expiresTime
                    )
                    val message = ChatMessage.create(
                        delegate.getAddress(),
                        target.targetId,
                        target.channelType,
                        Biz.MsgType.RedPacket,
                        payload
                    )
                    database.redPacketDao().insert(RedPacketMessage(message.msgId, it))
                    messageSender.send(url, message)
                }
                _sendState.value = 6
                // 获取红包id
                _sendResult.value = it
            }
        }
    }

    fun receiveRedPacket(params: ReceiveRedPacketParams, message: ChatMessage) {
        launch {
            _receiveState.value = 1
            flow {
                if (System.currentTimeMillis() - message.datetime < 10_000) {
                    // 如果是10s内刚发的红包，则不确认红包状态，直接抢红包
                    emit(Result.Success(FAKE_PACKET))
                } else {
                    emit(repository.getRedPacketInfo(params.packetId))
                }
            }.mapResult { packetInfo ->
                val record = repository.getReceiveRecord(delegate.getAddress() ?: "", params.packetId)
                if (record.isSucceed() && !record.data().isEmpty) {
                    throw RedPacketStateException(5, "红包已领取")
                }
                if (packetInfo.isExpired) {
                    throw RedPacketStateException(4, "红包已过期")
                }
                when (packetInfo.status) {
                    1 -> {
                        _receiveState.value = 2
                        val result = repository.receiveRedPacket(params)
                        if (result.isSucceed()) {
                            _receiveState.value = 3
                            waitForChainResult(INITIAL_DELAY, QUERY_PERIOD, 20, checker = { tx -> tx.isSuccess }) {
                                transaction.queryTransaction(result.data())
                            }
                        } else {
                            throw result.error()
                        }
                    }
                    2 -> throw RedPacketStateException(2, "红包已领完")
                    // 如果是已退回状态，则说明已经过期
                    3 -> throw RedPacketStateException(3, "红包已退回")
                    else -> throw RedPacketStateException(packetInfo.status, "未知的红包状态")
                }
            }.mapResult {
                val record = repository.getReceiveRecord(delegate.getAddress() ?: "", params.packetId)
                if (!record.isSucceed()) {
                    Result.Error(RedPacketFallbackException("红包记录查询失败"))
                }
                _receiveState.value = 4
                val result = repository.withDrawRedPacket(message.msg.fullExec, message.msg.exec!!.execer("redpacket"), message.msg.symbol!!, record.data().amount)
                if (result.isSucceed()) {
                    _receiveState.value = 5
                    waitForChainResult(INITIAL_DELAY, QUERY_PERIOD, checker = { tx -> tx.isSuccess }) {
                        transaction.queryTransaction(result.data())
                    }
                } else {
                    throw result.error()
                }
            }.mapResult({ RedPacketFallbackException("红包领取成功，未能提取到钱包：${it.message}") }) {
                message.msg.state = 5
                MessageSubscription.onMessage(Option.UPDATE_CONTENT, message)
                database.messageDao().insert(message.toMessagePO())
                packetInfo
            }.catch {
                _receiveState.value = 6
                if (it is RedPacketStateException && it.state == 5) {
                    // 红包已领取
                    message.msg.state = 5
                    MessageSubscription.onMessage(Option.UPDATE_CONTENT, message)
                    database.messageDao().insert(message.toMessagePO())
                }
                _receiveError.value = it
            }.collect {
                _receiveState.value = 6
                _receiveResult.value = params.packetId
            }
        }
    }

    fun getRedPacketAssets() {
        launch {
            _packetAssets.value =
                walletService?.getModuleAssets(delegate.getAddress() ?: "")?.filter { it.chain == RedPacketConfig.FULL_CHAIN } ?: emptyList()
        }
    }

    fun getRedPacketInfo(packetId: String) {
        request<RedPacketInfo> {
            onRequest {
                val result = repository.getRedPacketInfo(packetId)
                result.dataOrNull()?.also {
                    it.sender = manager.getUserInfo(it.addr, true)
                }
                result
            }
            onSuccess {
                _packetInfo.value = it
            }
        }
    }

    fun getMyReceiveRecord(packetId: String) {
        request<ReceiveInfo> {
            onRequest {
                repository.getReceiveRecord(delegate.getAddress() ?: "", packetId)
            }
            onSuccess {
                _myReceiveRecord.value = it
            }
        }
    }

    fun getReceiveList(packetId: String, receiveId: String) {
        request<List<ReceiveInfo>> {
            onRequest {
                val result = repository.getReceiveDetail(packetId, receiveId, AppConst.PAGE_SIZE)
                result.dataOrNull()?.onEach {
                    it.receiver = manager.getUserInfo(it.addr, true)
                }
                result
            }
            onSuccess {
                _receiveList.value = it
            }
        }
    }

    fun getStatisticRecordList(map: Map<String, String>) {
        request<StatisticInfo>(loading = false) {
            onRequest {
                val result = repository.getStatisticRecordList(map)
                result.dataOrNull()?.receiveRecords?.onEach {
                    it.sender = manager.getUserInfo(it.fromAddr, true)
                }
                result
            }
            onSuccess {
                _statisticInfo.value = it
            }
        }
    }

    fun backRedPacket(redPacket: RedPacketInfo) {
        launch {
            loading(false)
            flow {
                emit(repository.backRedPacket(redPacket.packetId))
            }.mapResult {
                // 等待区块链确认结果
                waitForChainResult(INITIAL_DELAY, QUERY_PERIOD, 20, checker = { tx -> tx.isSuccess }) {
                    transaction.queryTransaction(it)
                }
            }.mapResult { result ->
                var frozen = 0L
                // 获取可被退回金额
                result.receipt.logs?.forEach {
                    if (it["tyName"] == "LogExecActive") {
                        val map = (it["log"] as? Map<String, Any?>)?.get("current") as? Map<String, Any?>
                        frozen = (map?.get("balance") as? String?)?.toLong()?:0L
                    }
                }
                if (frozen == 0L) {
                    Result.Error(RedPacketFallbackException("可退回余额不足"))
                } else {
                    Result.Success(frozen)
                }
            }.mapResult {
                val packetExec = RedPacketConfig.FULL_CHAIN.execer("redpacket")
                val assetExec = RedPacketConfig.FULL_CHAIN.execer(redPacket.assetExec)
                val result = repository.withDrawRedPacket(assetExec, packetExec, redPacket.assetSymbol, it)
                if (result.isSucceed()) {
                    waitForChainResult(INITIAL_DELAY, QUERY_PERIOD, checker = { tx -> tx.isSuccess }) {
                        transaction.queryTransaction(result.data())
                    }
                } else {
                    throw result.error()
                }
            }.catch {
                dismiss()
                _receiveError.value = it
            }.collect {
                dismiss()
                _backPacketResult.value = redPacket.packetId
            }
        }
    }

    fun fetchGroupUsers(gid: Long) = liveData(Dispatchers.Main) {
        emitSource(database.groupUserDao().getGroupUserList(gid))
        groupRepo.getGroupUserList(null, gid)
    }

    fun getBrowserUrl(platform: String) {
        launch {
            _browserUrl.value = BWallet.get().getBrowserUrl(platform)
        }
    }
}