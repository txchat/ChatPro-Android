package com.fzm.chat.redpacket.data

import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Option
import com.fzm.chat.core.data.model.toMessagePO
import com.fzm.chat.core.logic.MessageSubscription
import com.fzm.chat.redpacket.data.bean.RedPacketInfo
import com.fzm.chat.redpacket.data.bean.isExpired
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2021/08/25
 * Description:
 */
class RedPacketRepository(
    private val dataSource: RedPacketDataSource
) : RedPacketDataSource by dataSource {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    override suspend fun getRedPacketInfo(packetId: String): Result<RedPacketInfo> {
        val result = dataSource.getRedPacketInfo(packetId)
        if (result.isSucceed()) {
            val packetInfo = result.data()
            val message = database.redPacketDao().getMessageByPacket(packetId)
            if (message != null) {
                // 如果自己没领取，则显示红包本身状态；否则显示已领取
                if (message.msg.state != 5) {
                    if (packetInfo.isExpired) {
                        message.msg.state = 4
                        MessageSubscription.onMessage(Option.UPDATE_CONTENT, message)
                        database.messageDao().insert(message.toMessagePO())
                    }
                    when (packetInfo.status) {
                        2 -> {
                            message.msg.state = 2
                            MessageSubscription.onMessage(Option.UPDATE_CONTENT, message)
                            database.messageDao().insert(message.toMessagePO())
                        }
                        3 -> {
                            // 如果是已退回状态，则说明已经过期
                            message.msg.state = 3
                            MessageSubscription.onMessage(Option.UPDATE_CONTENT, message)
                            database.messageDao().insert(message.toMessagePO())
                        }
                    }
                }
            }
        }
        return result
    }
}