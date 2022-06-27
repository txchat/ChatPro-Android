package com.fzm.chat.router.redpacket

import com.alibaba.android.arouter.facade.template.IProvider

interface RedPacketService : IProvider {

    suspend fun withDrawRedPacket(asset: ModuleAsset): String?

    fun getPlatform(): String
}