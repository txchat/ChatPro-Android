package com.fzm.arch.connection.protocol

import chat33.comet.Socket
import com.fzm.arch.connection.exception.ProtocolException
import com.fzm.arch.connection.utils.print
import com.fzm.arch.connection.utils.urlKey
import dtalk.proto.Api

/**
 * @author zhengjy
 * @since 2021/01/08
 * Description:
 */
class ProtocolReader(private val callback: FrameCallback) {

    interface FrameCallback {
        /**
         * 收到认证回复
         */
        fun onReadAuthReply(url: String, payload: ByteArray)

        /**
         * 收到心跳回复
         */
        fun onReadHeartBeatReply(url: String)

        /**
         * 收到ack消息
         *
         * @param seq 等待确认的seq号
         */
        fun onReadAck(url: String, seq: Int, data: ByteArray)

        /**
         * 收到事件
         */
        fun onReadEvent(url: String, message: Api.Proto, seq: Int)
    }

    fun processFrame(url: String, proto: Socket.Proto) {
        proto.print("onReceive [${url.urlKey()}]")
        if (proto.ver != Protocols.PROTOCOL_VER) {
            // 版本不同的消息不处理
            return
        }
        if (proto.ack != 0) {
            callback.onReadAck(url, proto.ack, proto.body.toByteArray())
        }
        when (Socket.Op.forNumber(proto.op)) {
            Socket.Op.AuthReply -> {
                callback.onReadAuthReply(url, proto.body.toByteArray())
            }
            Socket.Op.HeartbeatReply -> {
                callback.onReadHeartBeatReply(url)
            }
            Socket.Op.ReceiveMsg -> {
                val wrapper = Api.Proto.parseFrom(proto.body)
                callback.onReadEvent(url, wrapper, proto.seq)
            }
            Socket.Op.SendMsgReply -> {

            }
            else -> {
                throw ProtocolException("Unexpected op code ${proto.op}")
            }
        }
    }
}