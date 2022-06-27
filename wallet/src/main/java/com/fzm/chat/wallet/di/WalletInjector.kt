package com.fzm.chat.wallet.di

import android.app.Application
import android.content.Context
import android.os.Build
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.net.source.TransactionSource
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.CipherUtils
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.wallet.WalletConfig
import com.fzm.chat.wallet.ui.WalletViewModel
import com.fzm.chat.wallet.data.WalletRepository
import com.fzm.chat.wallet.data.WalletDataSource
import com.fzm.chat.wallet.impl.NetWalletDataSource
import com.fzm.chat.wallet.net.WalletService
import com.fzm.chat.wallet.net.WalletTransactionDelegate
import com.fzm.chat.wallet.ui.coins.CoinViewModel
import com.fzm.wallet.sdk.BWallet
import com.fzm.wallet.sdk.MnemonicManager
import com.fzm.wallet.sdk.MnemonicStore
import com.fzm.wallet.sdk.base.WalletModuleApp
import com.zjy.architecture.di.Injector
import com.zjy.architecture.di.rootScope
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import walletapi.Config
import walletapi.Walletapi

/**
 * @author zhengjy
 * @since 2021/08/03
 * Description:
 */
@Route(path = WalletModule.INJECTOR)
class WalletInjector : Injector, IProvider {

    private val wallet = named("Wallet")
    private val contract = named("Contract")

    private val delegate by rootScope.inject<LoginDelegate>()

    private var context: Context? = null

    override fun inject() = module {
        // 钱包SDK的依赖
        context?.also {
            setupMnemonicStore()
            BWallet.get().init(it, this, "1", "chat_wallet", "34323131acdf", "wqqwrasfaqwawe72819", "${Build.MANUFACTURER} ${Build.MODEL}")
        }
        single(wallet) {
            Walletapi.newChainClient(Config())/*.apply {
                setCfg("user.p.proof.", "user.p.proof.none", ChatConfig.NO_BALANCE_PRIVATE_KEY)
            }*/
        }
        viewModel { WalletViewModel(get(), get(), get(), get(), get()) }
        viewModel { CoinViewModel(get()) }
        single<WalletDataSource> {
            NetWalletDataSource(
                get(),
                get(wallet),
                get(),
                get<Retrofit>(contract).create(WalletService::class.java)
            )
        }
        single { WalletRepository(get(), get()) }
        single(wallet) {
            val service = get<Retrofit>(contract).create(WalletTransactionDelegate.WalletTransactionService::class.java)
            TransactionSource(WalletTransactionDelegate(service), get(wallet))
        }
    }

    override fun init(context: Context?) {
//        DeepLinkHelper.registerWebUrl(WalletModule.WEB, "")
        this.context = context
        (context as? Application?)?.also { WalletModuleApp.init(it) }
    }
    
    private fun setupMnemonicStore() {
        MnemonicManager.store = object : MnemonicStore {
            override suspend fun checkPassword(password: String): Boolean {
                return CipherUtils.checkPassword(password, delegate.preference.CHAT_KEY_PWD)
            }

            override suspend fun getMnemonicWords(password: String): String {
                return delegate.preference.getMnemonicString(password) ?: ""
            }

            override fun hasPassword(): Boolean {
                return delegate.preference.hasChatPassword()
            }

            override suspend fun saveMnemonicWords(mnemonic: String, password: String): Boolean {
                val mnem = delegate.preference.saveMnemonicString(mnemonic, password)
                return !mnem.isNullOrEmpty()
            }
        }
    }
}