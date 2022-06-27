package com.fzm.chat.di

import android.content.Context
import android.net.Uri
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.template.IProvider
import com.alibaba.android.arouter.launcher.ARouter
import com.danikula.videocache.HttpProxyCacheServer
import com.fzm.chat.R
import com.fzm.chat.account.BusinessViewModel
import com.fzm.chat.account.ImportLocalAccountViewModel
import com.fzm.chat.biz.QRCodeHelper
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.contact.AddFriendViewModel
import com.fzm.chat.contact.ContactListViewModel
import com.fzm.chat.contact.ContactViewModel
import com.fzm.chat.conversation.ChatViewModel
import com.fzm.chat.conversation.SessionViewModel
import com.fzm.chat.core.data.bean.Option
import com.fzm.chat.core.data.bean.message
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.core.media.DownloadManager2
import com.fzm.chat.group.GroupViewModel
import com.fzm.chat.login.words.ImportAccountViewModel
import com.fzm.chat.media.SearchFileViewModel
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.search.SearchLocalViewModel
import com.fzm.chat.ui.ContactCardSelectFragment
import com.fzm.chat.ui.ForwardSelectFragment
import com.fzm.chat.ui.TransferSelectFragment
import com.fzm.chat.vm.*
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.zjy.architecture.di.Injector
import com.zjy.architecture.ext.toast
import dtalk.biz.Biz
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import xyz.doikki.videoplayer.ijk.IjkPlayerFactory
import xyz.doikki.videoplayer.player.VideoViewConfig
import xyz.doikki.videoplayer.player.VideoViewManager

/**
 * @author zhengjy
 * @since 2020/12/11
 * Description:
 */
@Route(path = MainModule.INJECTOR)
class MainInjector : Injector, IProvider {

    override fun inject() = module {
        setupRefresh()
        setupQRCodeProcessor()
        subscribeMessage()
        setupVideoPlayer()
        viewModel { ImportAccountViewModel(get()) }
        viewModel { ChooseServerViewModel(get(), get(), get()) }
        viewModel { ServerManageViewModel(get(), get(), get(), get(), get()) }
        viewModel { EditInfoViewModel(get()) }
        viewModel { SecuritySettingViewModel(get(), get()) }
        viewModel { EncryptPasswordViewModel(get(), get(), get()) }
        viewModel { ChatViewModel(get(), get(), get(), get(), get(), get(), get()) }
        viewModel { ContactViewModel(get(), get(), get(), get()) }
        viewModel { ContactListViewModel(get()) }
        viewModel { SessionViewModel(get()) }
        viewModel { SearchUserViewModel(get(), get(), get()) }
        viewModel { QRCodeViewModel(get()) }
        viewModel { BackupViewModel(get(), get(), get()) }
        viewModel { AccountViewModel(get()) }
        viewModel { ChatFileViewModel() }
        viewModel { SearchLocalViewModel(get(), get(), get()) }
        viewModel { SearchFileViewModel() }
        viewModel { GroupViewModel(get(), get(), get()) }
        viewModel { ForwardSelectFragment.ForwardSelectViewModel(get(), get(), get()) }
        viewModel { TransferSelectFragment.TransferViewModel(get()) }
        viewModel { ContactCardSelectFragment.ContactCardViewModel(get(), it.component1()) }
        viewModel { BusinessViewModel(get()) }
        viewModel { AddFriendViewModel(get(), get(), get(), get()) }
        viewModel { ImportLocalAccountViewModel(get(), get(), get()) }
    }

    private fun setupRefresh() {
        // 下拉加载风格
        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, layout ->
            layout.setPrimaryColorsId(R.color.biz_color_primary, R.color.biz_text_grey_light)
            ClassicsHeader(context)
        }
        // 上拉加载风格
        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, layout ->
            layout.setPrimaryColorsId(R.color.biz_color_primary, R.color.biz_text_grey_light)
            ClassicsFooter(context)
        }
    }

    private fun setupQRCodeProcessor() {
        QRCodeHelper.registerQRProcessor(object : QRCodeHelper.Processor {
            override fun process(context: Context, result: String?): Boolean {
                val uri = Uri.parse(result)
                val groupId = uri.getQueryParameter("gid")
                val friendId = uri.getQueryParameter("uid")
                val target = uri.getQueryParameter("transfer_target")
                if (uri.toString().contains(AppConfig.APP_DOWNLOAD_URL)) {
                    return if (!groupId.isNullOrEmpty()) {
                        val server = uri.getQueryParameter("server")
                        val inviterId = uri.getQueryParameter("inviterId")
                        val createTime = uri.getQueryParameter("createTime")
                        if (System.currentTimeMillis() - (createTime?.toLong() ?: 0) > 7 * 24 * 60 * 60 * 1000) {
                            context.toast(context.getString(R.string.chat_tips_group_join_invalid_qrcode))
                            return true
                        }
                        ARouter.getInstance().build(MainModule.JOIN_GROUP_INFO)
                            .withLong("groupId", groupId.toLongOrNull() ?: 0L)
                            .withString("server", server)
                            .withString("inviterId", inviterId)
                            .navigation()
                        true
                    } else if (!friendId.isNullOrEmpty()) {
                        ARouter.getInstance().build(MainModule.CONTACT_INFO)
                            .withString("address", friendId)
                            .navigation()
                        true
                    } else if (!target.isNullOrEmpty()) {
                        val address = uri.getQueryParameter("address")
                        val chain = uri.getQueryParameter("chain")
                        val platform = uri.getQueryParameter("platform")
                        ARouter.getInstance().build(WalletModule.TRANSFER)
                            .withString("target", target)
                            .withString("coinAddress", address)
                            .withString("chain", chain)
                            .withString("platform", platform)
                            .navigation()
                        true
                    } else {
                        false
                    }
                } else {
                    return false
                }
            }
        })
    }

    private fun Module.setupVideoPlayer() {
        VideoViewManager.setConfig(
            VideoViewConfig.newBuilder()
                .setPlayerFactory(IjkPlayerFactory.create())
                .build()
        )
        single { HttpProxyCacheServer(get()) }
    }

    private fun subscribeMessage() {
        GlobalScope.launch {
            MessageSubscription.registerChannel(this, actor {
                for (it in channel) {
                    when (it.option) {
                        Option.ADD_MSG -> {
                            if (it.message.msgType == Biz.MsgType.Audio_VALUE
                                || it.message.msgType == Biz.MsgType.Image_VALUE
                            ) {
                                // 收到语音、图片消息自动下载
                                // 视频、文件太大暂时不自动下载
                                DownloadManager2.downloadToApp(it.message)
                            }
                        }
                        else -> {

                        }
                    }
                }
            })
        }
    }

    override fun init(context: Context?) {

    }
}