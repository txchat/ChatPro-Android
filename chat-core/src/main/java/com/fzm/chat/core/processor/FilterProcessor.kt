package com.fzm.chat.core.processor

import com.fzm.arch.connection.processor.Processor
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.data.po.isBlock
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/04/14
 * Description:
 */
class FilterProcessor(
    private val contactManager: ContactManager,
) : Processor<Biz.Message> {

    override suspend fun process(server: String, message: Biz.Message): Biz.Message? {
        if (message.from == AppPreference.ADDRESS && message.target == AppPreference.ADDRESS) {
            // 自己发给自己的消息忽略
            return null
        }
        if (message.channelType == ChatConst.GROUP_CHANNEL) {
            return message
        }
        val target = getTargetAddress(message)
        val sender = contactManager.getUserInfo(target, true)
        if (sender is FriendUser && sender.isBlock) {
            // 如果是黑名单用户发过来的消息则直接丢弃
            return null
        }
        return message
    }

    private fun getTargetAddress(message: Biz.Message): String {
        return if (AppPreference.ADDRESS == message.from) {
            message.target
        } else {
            message.from
        }
    }
}