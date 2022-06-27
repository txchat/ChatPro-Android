package com.fzm.chat.redpacket.impl

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.core.data.platform
import com.fzm.chat.core.utils.mul
import com.fzm.chat.redpacket.RedPacketConfig
import com.fzm.chat.redpacket.data.RedPacketRepository
import com.fzm.chat.router.redpacket.*
import com.zjy.architecture.di.rootScope

@Route(path = RedPacketModule.SERVICE)
class RedPacketServiceImp : RedPacketService {

    private val repository by rootScope.inject<RedPacketRepository>()

    override suspend fun withDrawRedPacket(asset: ModuleAsset): String? {
        val balance = asset.balance.mul(AppConfig.AMOUNT_SCALE)
        return repository.withDrawRedPacket(asset.fullExec, asset.exexer("redpacket"), asset.symbol, balance).dataOrNull()
    }

    override fun getPlatform(): String {
        return RedPacketConfig.FULL_CHAIN.platform
    }

    override fun init(context: Context?) {
    }
}