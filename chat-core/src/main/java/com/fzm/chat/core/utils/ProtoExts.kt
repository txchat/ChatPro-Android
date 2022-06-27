package com.fzm.chat.core.utils

import com.fzm.chat.core.crypto.encrypt
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.po.MessageContent
import com.google.protobuf.ByteString
import com.zjy.architecture.util.logD
import dtalk.biz.Biz
import dtalk.biz.msg.Msg
import dtalk.proto.Api

/**
 * @author zhengjy
 * @since 2021/01/07
 * Description:
 */

/**
 * 序列化要发送的私聊消息
 */
fun ChatMessage.buildProto(publicKey: String? = null, privateKey: String? = null): Api.Proto {
    val type = Biz.MsgType.forNumber(msgType)
    val content = messageContentToBytes(type, msg, false)
    val encrypt = try {
        if (publicKey.isNullOrEmpty() || privateKey.isNullOrEmpty()) {
            content
        } else {
            CipherUtils.encrypt(content, publicKey, privateKey)
        }
    } catch (e: Exception) {
        logD("Encrypt failed! user:$target, pub_key:$publicKey, pri_key:$privateKey")
        content
    }
    return buildInner(type, encrypt)
}

/**
 * 序列化要发送的群聊消息
 */
fun ChatMessage.buildProto(aesKey: String?): Api.Proto {
    val type = Biz.MsgType.forNumber(msgType)
    val content = messageContentToBytes(type, msg, false)
    val encrypt = try {
        if (aesKey.isNullOrEmpty()) {
            content
        } else {
            content.encrypt(aesKey)
        }
    } catch (e: Exception) {
        logD("Encrypt failed! group:$target, aes_key:$aesKey")
        content
    }
    return buildInner(type, encrypt)
}

private fun ChatMessage.buildInner(type: Biz.MsgType, content: ByteArray): Api.Proto {
    val body = Biz.Message.newBuilder()
        .setLogId(logId)
        .setMsgId(msgId)
        .setChannelType(channelType)
        .setFrom(from)
        .setTarget(target)
        .setMsgType(type)
        .setMsg(ByteString.copyFrom(content))
        .setDatetime(datetime)
        .also {
            // 添加消息转发来源（如果存在）
            source?.apply {
                it.source = Biz.Source.newBuilder()
                    .setChannelType(channelType)
                    .setFrom(Biz.SourceUser.newBuilder().setId(from.id).setName(/*from.name*/""))
                    .setTarget(Biz.SourceUser.newBuilder().setId(target.id).setName(/*target.name*/""))
                    .build()
            }
            // 添加消息引用（如果存在）
            reference?.apply {
                it.reference = Biz.Reference.newBuilder()
                    .setTopic(topic)
                    .setRef(ref)
                    .build()
            }
        }
        .build()
        .toByteString()
    return Api.Proto.newBuilder()
        // 聊天消息的eventType固定为0
        .setEvent(Biz.Event.message.number)
        .setBody(body)
        .build()
}

private fun messageContentToBytes(
    msgType: Biz.MsgType,
    msg: MessageContent,
    fromForward: Boolean
): ByteArray {
    return when (msgType) {
        Biz.MsgType.System, Biz.MsgType.Text -> Msg.Text.newBuilder().setContent(msg.content)
            .addAllMention(if (fromForward) emptyList() else msg.atList ?: emptyList()).build()
            .toByteArray()
        Biz.MsgType.Audio -> {
            if (fromForward) return ByteArray(0)
            Msg.Audio.newBuilder().setUrl(msg.mediaUrl).setTime(msg.duration)
                .build().toByteArray()
        }
        Biz.MsgType.Image -> Msg.Image.newBuilder().setUrl(msg.mediaUrl).setHeight(msg.height)
            .setWidth(msg.width).build().toByteArray()
        Biz.MsgType.Video -> Msg.Video.newBuilder().setUrl(msg.mediaUrl).setHeight(msg.height)
            .setWidth(msg.width).setTime(msg.duration).build().toByteArray()
        Biz.MsgType.File -> Msg.File.newBuilder().setUrl(msg.mediaUrl).setName(msg.fileName)
            .setSize(msg.size).setMd5(msg.md5).build().toByteArray()
        Biz.MsgType.Forward -> {
            if (fromForward) return ByteArray(0)
            val builder = Msg.Forward.newBuilder()
            msg.forwardLogs.forEach {
                builder.addLogs(
                    Msg.ForwardMsg.newBuilder()
                        .setAvatar(it.avatar)
                        .setName(it.name)
                        .setMsgType(it.msgType)
                        .setMsg(
                            ByteString.copyFrom(
                                messageContentToBytes(
                                    Biz.MsgType.forNumber(it.msgType),
                                    it.msg,
                                    true
                                )
                            )
                        )
                        .setDatetime(it.datetime)
                )
            }
            builder.build().toByteArray()
        }
        Biz.MsgType.RTCCall -> ByteArray(0)
        Biz.MsgType.Transfer -> {
            Msg.Transfer.newBuilder()
                .setChain(msg.chain)
                .setPlatform(msg.platform ?: "")
                .setTxHash(msg.txHash)
                .setCoinName(msg.txSymbol)
                .setCoinTypeValue(msg.coinType)
                .build()
                .toByteArray()
        }
        Biz.MsgType.RedPacket -> {
            Msg.RedPacket.newBuilder()
                .setPacketId(msg.packetId)
                .setExec(msg.exec)
                .setCoinTypeValue(msg.coinType)
                .setCoinName(msg.symbol)
                .setPacketTypeValue(msg.packetType)
                .setPrivateKey(msg.privateKey)
                .setRemark(msg.remark)
                .setExpire(msg.expire)
                .build()
                .toByteArray()
        }
        Biz.MsgType.ContactCard -> {
            Msg.ContactCard.newBuilder()
                .setType(msg.contactType)
                .setId(msg.contactId)
                .setAvatar(msg.contactAvatar)
                .setName(msg.contactName)
                .apply {
                    msg.contactServer?.also { server = it }
                    msg.contactInviter?.also { inviter = it }
                }
                .build()
                .toByteArray()
        }
        else -> throw IllegalArgumentException("Unsupported msgType:${msgType}")
    }

}