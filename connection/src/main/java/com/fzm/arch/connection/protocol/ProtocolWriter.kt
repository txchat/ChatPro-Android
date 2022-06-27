package com.fzm.arch.connection.protocol

import chat33.comet.Socket
import com.fzm.arch.connection.socket.ConnectionManager
import com.fzm.arch.connection.utils.extra
import com.fzm.chat.router.core.CoreModule
import com.fzm.chat.router.core.CoreService
import com.fzm.chat.router.route
import com.google.protobuf.ByteString

/**
 * @author zhengjy
 * @since 2021/01/08
 * Description:
 */
class ProtocolWriter(private val manager: ConnectionManager) {

    private val coreService by route<CoreService>(CoreModule.SERVICE)

    internal fun writePing(url: String) {
        writeControlFrame(url, Socket.Op.Heartbeat_VALUE, 0, ByteString.EMPTY)
    }

    internal fun writeAck(url: String, seq: Int) {
        writeControlFrame(url, Socket.Op.ReceiveMsgReply_VALUE, seq, ByteString.EMPTY)
    }

    internal fun auth(url: String, firstConnect: Boolean) {
        val auth = Socket.AuthMsg.newBuilder()
            .setAppId("dtalk")
            .setToken(coreService?.signAuth())
            .apply {
                val ext = coreService?.authPayload(firstConnect)
                if (ext != null) {
                    setExt(ByteString.copyFrom(ext))
                }
            }
            .build()
        writeControlFrame(url, Socket.Op.Auth_VALUE, 0, auth.toByteString())
    }

    private fun writeControlFrame(url: String, op: Int, ack: Int, payload: ByteString) {
        manager.send(url, payload.toByteArray(), extra(op, ack, false))
    }
}