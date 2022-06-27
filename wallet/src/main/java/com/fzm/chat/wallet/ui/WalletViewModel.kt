package com.fzm.chat.wallet.ui

import androidx.lifecycle.*
import com.fzm.chat.biz.db.AppDatabase
import com.fzm.chat.biz.db.po.LocalNote
import com.fzm.chat.core.chain.waitForChainResult
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Option
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.toMessagePO
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.logic.MessageSender
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.redpacket.ModuleAsset
import com.fzm.chat.router.redpacket.RedPacketModule
import com.fzm.chat.router.redpacket.RedPacketService
import com.fzm.chat.router.route
import com.fzm.chat.wallet.data.WalletRepository
import com.fzm.chat.wallet.data.bean.TransactionInfo
import com.fzm.chat.wallet.data.bean.WalletAsset
import com.fzm.chat.wallet.data.coinType
import com.fzm.wallet.sdk.utils.tokenSymbol
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.bean.Miner
import com.fzm.wallet.sdk.db.entity.Coin
import com.fzm.wallet.sdk.utils.decimalPlaces
import com.fzm.wallet.sdk.utils.fullChain
import com.fzm.wallet.sdk.utils.isCoin
import com.zjy.architecture.data.Result
import com.zjy.architecture.exception.ApiException
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request
import dtalk.biz.Biz
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2021/08/03
 * Description:
 */
class WalletViewModel(
    private val contactManager: ContactManager,
    private val repository: WalletRepository,
    private val delegate: LoginDelegate,
    private val messageSender: MessageSender,
    private val database: AppDatabase,
) : LoadingViewModel(), LoginDelegate by delegate {

    private var coinFilter: ((Coin) -> Boolean)? = null

    private val chatDatabase: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val _transferResult = MutableLiveData<String>()
    val transferResult: LiveData<String>
        get() = _transferResult

    /**
     * 资产
     */
    val moduleAssets: LiveData<List<Coin>> by lazy {
        BWallet.get().getCoinBalance(0L, 8000L, true, coinFilter)
            .asLiveData(coroutineContext)
    }

    /**
     * nft资产
     */
    private val _nftAssets = MutableLiveData<List<Coin>>()
    val nftAssets: LiveData<List<Coin>>
        get() = _nftAssets

    private val _asset = MutableLiveData<WalletAsset>()
    val asset: LiveData<WalletAsset>
        get() = _asset

    private val _txList = MutableLiveData<TransactionInfo.Wrapper>()
    val txList: LiveData<TransactionInfo.Wrapper>
        get() = _txList

    private val _txOutList = MutableLiveData<TransactionInfo.Wrapper>()
    val txOutList: LiveData<TransactionInfo.Wrapper>
        get() = _txOutList

    private val _txInList = MutableLiveData<TransactionInfo.Wrapper>()
    val txInList: LiveData<TransactionInfo.Wrapper>
        get() = _txInList

    private val _txInfo = MutableLiveData<TransactionInfo>()
    val txInfo: LiveData<TransactionInfo>
        get() = _txInfo

    private val _accountAsset = MutableLiveData<List<ModuleAsset>>()
    val accountAsset: LiveData<List<ModuleAsset>>
        get() = _accountAsset

    private val _browserUrl = MutableLiveData<String>()
    val browserUrl: LiveData<String>
        get() = _browserUrl

    private val _miner = MutableLiveData<Miner?>()
    val miner: LiveData<Miner?>
        get() = _miner

    private val _enoughChainAssets = MutableLiveData(true)
    val enoughChainAssets: LiveData<Boolean>
        get() = _enoughChainAssets

    private val redPacketService by route<RedPacketService>(RedPacketModule.SERVICE)

    fun setCoinFilter(coinFilter: (Coin) -> Boolean) {
        this.coinFilter = coinFilter
    }

    private var feeJob: Job? = null

    fun getRecommendedFee(chain: String, name: String) {
        feeJob?.cancel()
        feeJob = launch {
            val fee = BWallet.get().getRecommendedFee(chain)
            if (isActive) {
                if (fee != null) {
                    _miner.value = fee
                } else {
                    _miner.value = null
                }
            }
        }
    }

    /**
     * 向target转账
     */
    fun transfer(
        coin: Coin,
        target: String,
        coinAddress: String,
        assets: String,
        amount: Double,
        fee: Double,
        password: String,
        note: String? = null,
        localNote: String? = null,
        sendMsg: Boolean = true
    ) {
        request<String> {
            onRequest {
                val result = repository.transfer(coinAddress, coin, amount, fee, password, note)
                if (result.isSucceed()) {
                    val hash = result.data()
                    if (sendMsg) {
                        val contact = contactManager.getUserInfo(target)
                        val url = contact.getServerList().firstOrNull()?.address ?: ""
                        val payload = MessageContent.transfer(coin.chain, coin.platform, hash, assets, coin.coinType)
                        val message = ChatMessage.create(
                            delegate.getAddress(),
                            target,
                            ChatConst.PRIVATE_CHANNEL,
                            Biz.MsgType.Transfer,
                            payload
                        )
                        queryTransaction(delegate.getAddress() ?: "", coinAddress, message, coin)
                        messageSender.send(url, message)
                        return@onRequest Result.Success(hash)
                    } else {
                        return@onRequest Result.Success(hash)
                    }
                } else {
                    return@onRequest Result.Error(result.error())
                }
            }
            onSuccess { hash ->
                localNote?.also { note ->
                    launch {
                        database.localTxNoteDao().insert(LocalNote(hash, note))
                    }
                }
                _transferResult.value = hash
            }
        }
    }

    /**
     * 查询转账交易的详情
     */
    private fun queryTransaction(from: String, target: String, message: ChatMessage, coin: Coin) {
        GlobalScope.launch {
            val payload = message.msg
            waitForChainResult(checker = { it.status != 0 }) {
                try {
                    val trans = BWallet.get().getTransactionByHash(coin.chain, coin.tokenSymbol, payload.txHash!!)
                    Result.Success(trans)
                } catch (e: Exception) {
                    Result.Error(e)
                }
            }.dataOrNull()?.also { tx ->
                payload.txAmount = tx.value
                payload.txStatus = tx.status.toString()
                payload.txInvalid = tx.from != from || tx.to != target
                MessageSubscription.onMessage(Option.UPDATE_CONTENT, message)
                chatDatabase.messageDao().insert(message.toMessagePO())
            }
        }
    }

    private var checkChainAssetJob: Job? = null

    fun checkChainAssetEnough(chain: String, fee: Double) {
        checkChainAssetJob?.cancel()
        checkChainAssetJob = launch {
//            val balance = BWallet.get().getMainCoin(chain)?.balance ?: "0"
//            _enoughChainAssets.value = balance.toDouble() > fee

            // 2022年3月10日12点03分：token使用代扣，所以不检查主链资产
            _enoughChainAssets.value = true
        }
    }

    fun hasPassword() = delegate.preference.hasChatPassword()

    fun getLocalNoteByHash(hash: String) = database.localTxNoteDao().getLocalTxNote(hash)

    fun saveLocalNote(hash: String, note: String) {
        launch { database.localTxNoteDao().insert(LocalNote(hash, note)) }
    }

    fun observeTxList(
        type: Int,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<TransactionInfo.Wrapper>
    ) {
        when (type) {
            0 -> txList
            1 -> txOutList
            2 -> txInList
            else -> throw Exception("交易列表type类型错误")
        }.observe(lifecycleOwner, observer)
    }

    fun getBrowserUrl(platform: String) {
        launch {
            _browserUrl.value = BWallet.get().getBrowserUrl(platform)
        }
    }

    fun getTargetInfo(targetId: String?) = liveData {
        if (targetId != null) emit(contactManager.getUserInfo(targetId))
    }

    fun getNFTAssetList(showLoading: Boolean = true) {
        request<List<Coin>>(showLoading) {
            onRequest {
                // TODO : 改成获取NFT资产
                Result.Error(ApiException("正在开发中，暂不支持NFT"))
            }
            onSuccess {
                _nftAssets.value = it
            }
        }
    }

    fun getCoinBalance(coin: Coin) {
        request<Coin> {
            onRequest {
                _asset.value = WalletAsset(
                    coin.name,
                    coin.address,
                    coin.balance,
                    null,
                    "${coin.platform}.${coin.name}"
                )
                Result.Success(BWallet.get().getCoinBalance(coin, true))
            }
            onSuccess {
                _asset.value = WalletAsset(
                    coin.name,
                    coin.address,
                    coin.balance,
                    null,
                    "${coin.platform}.${coin.name}"
                )
            }
        }
    }

    fun getTransactionList(coin: Coin, type: Int, index: Long, refreshAsset: Boolean = true) {
        if (refreshAsset) {
            // 每次刷新交易列表的时候，同时刷新资产
            getCoinBalance(coin)
        }
        request<List<TransactionInfo>> {
            onRequest {
                repository.getTransactionList(coin, type, index)
            }
            onSuccess {
                val wrapper = TransactionInfo.Wrapper(index == 0L, it)
                when (type) {
                    0 -> _txList.value = wrapper
                    1 -> _txOutList.value = wrapper
                    2 -> _txInList.value = wrapper
                }
            }
        }
    }

    fun getTransactionById(chain: String, txId: String, symbol: String) {
        request<TransactionInfo?> {
            onRequest {
                repository.getTransactionById(chain, txId, symbol)
            }
            onSuccess {
                _txInfo.value = it
            }
        }
    }

    fun getAccountAssetByAddress() {
        launch {
            _accountAsset.value = getAddress()?.let { address ->
                BWallet.get().getRedPacketAssets(address)
                    .filter { it.platform == redPacketService?.getPlatform() }
                    .map {
                        ModuleAsset(
                            it.nickname,
                            it.fullChain,
                            it.isCoin,
                            it.name ?: "",
                            it.balance ?: "",
                            it.decimalPlaces
                        )
                    }
            } ?: emptyList()
        }
    }

    suspend fun withDrawAssets(assets: List<ModuleAsset>) {
        loading(true)
        for (item in assets) {
            if(item.balance != "0") {
                redPacketService?.withDrawRedPacket(item)
            }
        }
        dismiss()
    }
}