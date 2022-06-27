package com.fzm.chat.core.processor

import com.fzm.arch.connection.exception.CipherException
import com.fzm.arch.connection.processor.Processor
import com.fzm.chat.core.crypto.decrypt
import com.fzm.chat.core.data.AppPreference
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.getPubKey
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.CipherUtils
import com.google.protobuf.ByteString
import com.zjy.architecture.util.logD
import com.zjy.architecture.util.logI
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2020/12/07
 * Description:消息解密相关操作
 */
class DecryptProcessor(
    private val contactManager: ContactManager,
    private val delegate: LoginDelegate
) : Processor<Biz.Message> {

    override suspend fun process(server: String, message: Biz.Message): Biz.Message? {
        if (message.channelType == ChatConst.GROUP_CHANNEL) {
            val sender = contactManager.getGroupInfo(message.target, server)
            try {
                val start = System.nanoTime()
                if (sender.key == null || sender.key == ChatConst.INVALID_AES_KEY) {
                    throw CipherException("Decrypt failed! group: empty key")
                }
                val decrypted = message.msg.toByteArray().decrypt(sender.key)
                val end = System.nanoTime()
                logI("Decrypt time group: ${(end - start) / 1e6}ms")
                return message.rebuildContent(decrypted)
            } catch (e: Exception) {
                logD("Decrypt failed! group:${message.target}, logId:${message.logId}, aes_key:${sender.key}")
                if (sender.key == ChatConst.INVALID_AES_KEY) {
                    throw Exception("can't get group key, ignore message")
                } else {
                    throw CipherException("Decrypt failed! group:${message.target}, logId:${message.logId}", e)
                }
            }
        } else {
            val target = getTargetAddress(message)
            val sender = contactManager.getUserInfo(target, true)
            try {
                val start = System.nanoTime()
                val pub = sender.getPubKey()
                val pri = delegate.preference.PRI_KEY
                if (pub.isEmpty()) {
                    throw CipherException("Decrypt failed! user: empty public key")
                }
                if (pri.isEmpty()) {
                    throw CipherException("Decrypt failed! user: empty private key")
                }
                val decrypted = CipherUtils.decrypt(message.msg.toByteArray(), pub, pri)
                val end = System.nanoTime()
                logI("Decrypt time user: ${(end - start) / 1e6}ms")
                return message.rebuildContent(decrypted)
            } catch (e: Exception) {
                logD("Decrypt failed! user:$target, logId:${message.logId}, " +
                    "pub_key:${sender.getPubKey()}, pri_key:${delegate.preference.PRI_KEY}")
                throw CipherException("Decrypt failed! user:$target, logId:${message.logId}", e)
            }
        }
    }

    private fun Biz.Message.rebuildContent(content: ByteArray) : Biz.Message {
        return Biz.Message.newBuilder()
            .setLogId(logId)
            .setMsgId(msgId)
            .setChannelType(channelType)
            .setFrom(from)
            .setTarget(target)
            .setMsgType(msgType)
            .setMsg(ByteString.copyFrom(content))
            .setDatetime(datetime)
            .apply {
                if (this@rebuildContent.hasSource()) {
                    source = this@rebuildContent.source
                }
                if (this@rebuildContent.hasReference()) {
                    reference = this@rebuildContent.reference
                }
            }
            .build()
    }

    private fun getTargetAddress(message: Biz.Message): String {
        return if (AppPreference.ADDRESS == message.from) {
            message.target
        } else {
            message.from
        }
    }
}