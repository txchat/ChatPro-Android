package com.fzm.arch.connection.utils

import chat33.comet.Socket
import com.fzm.arch.connection.protocol.Protocols.ACK_LENGTH
import com.fzm.arch.connection.protocol.Protocols.HEADER_LENGTH
import com.fzm.arch.connection.protocol.Protocols.OP_LENGTH
import com.fzm.arch.connection.protocol.Protocols.PACKAGE_LENGTH
import com.fzm.arch.connection.protocol.Protocols.SEQ_LENGTH
import com.fzm.arch.connection.protocol.Protocols.VER_LENGTH
import com.google.protobuf.ByteString
import com.zjy.architecture.Arch
import com.zjy.architecture.util.logD
import dtalk.proto.Api
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @author zhengjy
 * @since 2021/01/07
 * Description:
 */
fun Socket.Proto.toPackage(): ByteArray {
    val headerLength: Short =
        (PACKAGE_LENGTH + HEADER_LENGTH + VER_LENGTH + OP_LENGTH + SEQ_LENGTH + ACK_LENGTH).toShort()
    val packageLength: Int = headerLength + body.size()

    val _packageLength = ByteArray(PACKAGE_LENGTH)
    ByteBuffer.allocate(PACKAGE_LENGTH).apply {
        order(ByteOrder.BIG_ENDIAN)
        putInt(packageLength)
        rewind()
        get(_packageLength)
    }
    val _headerLength = ByteArray(HEADER_LENGTH)
    ByteBuffer.allocate(HEADER_LENGTH).apply {
        order(ByteOrder.BIG_ENDIAN)
        putShort(headerLength)
        rewind()
        get(_headerLength)
    }
    val _ver = ByteArray(VER_LENGTH)
    ByteBuffer.allocate(VER_LENGTH).apply {
        order(ByteOrder.BIG_ENDIAN)
        putShort(ver.toShort())
        rewind()
        get(_ver)
    }
    val _op = ByteArray(OP_LENGTH)
    ByteBuffer.allocate(OP_LENGTH).apply {
        order(ByteOrder.BIG_ENDIAN)
        putInt(op)
        rewind()
        get(_op)
    }
    val _seq = ByteArray(SEQ_LENGTH)
    ByteBuffer.allocate(SEQ_LENGTH).apply {
        order(ByteOrder.BIG_ENDIAN)
        putInt(seq)
        rewind()
        get(_seq)
    }
    val _ack = ByteArray(ACK_LENGTH)
    ByteBuffer.allocate(ACK_LENGTH).apply {
        order(ByteOrder.BIG_ENDIAN)
        putInt(ack)
        rewind()
        get(_ack)
    }
    return _packageLength + _headerLength + _ver + _op + _seq + _ack + body.toByteArray()
}

fun ByteArray.toProto(): Socket.Proto {
    val _ver = ByteArray(VER_LENGTH)
    System.arraycopy(this, 0, _ver, 0, _ver.size)
    val _op = ByteArray(OP_LENGTH)
    System.arraycopy(this, VER_LENGTH, _op, 0, _op.size)
    val _seq = ByteArray(SEQ_LENGTH)
    System.arraycopy(this, VER_LENGTH + OP_LENGTH, _seq, 0, _seq.size)
    val _ack = ByteArray(ACK_LENGTH)
    System.arraycopy(this, VER_LENGTH + OP_LENGTH + SEQ_LENGTH, _ack, 0, _ack.size)
    val _body = ByteArray(this.size - (VER_LENGTH + OP_LENGTH + SEQ_LENGTH + ACK_LENGTH))
    if (_body.isNotEmpty()) {
        System.arraycopy(this, VER_LENGTH + OP_LENGTH + SEQ_LENGTH + ACK_LENGTH, _body, 0, _body.size)
    }
    return Socket.Proto.newBuilder()
        .setVer(ByteBuffer.wrap(_ver).order(ByteOrder.BIG_ENDIAN).short.toInt())
        .setOp(ByteBuffer.wrap(_op).order(ByteOrder.BIG_ENDIAN).int)
        .setSeq(ByteBuffer.wrap(_seq).order(ByteOrder.BIG_ENDIAN).int)
        .setAck(ByteBuffer.wrap(_ack).order(ByteOrder.BIG_ENDIAN).int)
        .setBody(ByteString.copyFrom(_body))
        .build()
}

fun Socket.Proto.toPackage1(): ByteArray {
    val payload = this.toByteArray()
    val packageLength: Int = PACKAGE_LENGTH + HEADER_LENGTH + payload.size
    val headerLength: Short = (packageLength - body.size()).toShort()

    val _packageLength = ByteArray(PACKAGE_LENGTH)
    ByteBuffer.allocate(PACKAGE_LENGTH).apply {
        order(ByteOrder.BIG_ENDIAN)
        putInt(packageLength)
        rewind()
        get(_packageLength)
    }
    val _headerLength = ByteArray(HEADER_LENGTH)
    ByteBuffer.allocate(HEADER_LENGTH).apply {
        order(ByteOrder.BIG_ENDIAN)
        putShort(headerLength)
        rewind()
        get(_headerLength)
    }
    return _packageLength + _headerLength + body.toByteArray()
}

fun ByteArray.toProto1(): Socket.Proto {
    return Socket.Proto.parseFrom(this)
}

/**
 * 格式化打印
 */
fun Socket.Proto.print(tag: String) {
    if (!Arch.debug) {
        return
    }
    val message = try {
        when (op) {
            Socket.Op.Auth_VALUE -> {
                "ver=$ver op=${Socket.Op.forNumber(op)} seq=$seq ack=$ack body=${Socket.AuthMsg.parseFrom(body).printMessage()}"
            }
            Socket.Op.AuthReply_VALUE -> {
                "ver=$ver op=${Socket.Op.forNumber(op)} seq=$seq ack=$ack body=$body"
            }
            Socket.Op.SendMsg_VALUE, Socket.Op.SendMsgReply_VALUE -> {
                "ver=$ver op=${Socket.Op.forNumber(op)} seq=$seq ack=$ack body=${Api.Proto.parseFrom(body).printMessage()}"
            }
            Socket.Op.ReceiveMsg_VALUE -> {
                "ver=$ver op=${Socket.Op.forNumber(op)} seq=$seq ack=$ack body=${Api.Proto.parseFrom(body).printMessage()}"
            }
            Socket.Op.ReceiveMsgReply_VALUE -> {
                "ver=$ver op=${Socket.Op.forNumber(op)} seq=$seq ack=$ack body=[]"
            }
            else -> "ver=$ver op=${Socket.Op.forNumber(op)} seq=$seq ack=$ack body=[]"
        }
    } catch (e: Exception) {
        "ver=$ver op=${Socket.Op.forNumber(op)} seq=$seq body=$body"
    }
    logD("msg:$tag |", message)
}

fun Api.Proto.printMessage(): String {
    val sb = StringBuilder()
    sb.append("[")
    sb.append("event=$event, ")
    sb.append("body=$body")
    sb.append("]")
    return sb.toString()
}

private fun Socket.AuthMsg.printMessage(): String {
    val sb = StringBuilder()
    sb.append("[")
    sb.append("appId=$appId, ")
    sb.append("token=$token, ")
    if (ext.size() > 0) {
        sb.append("ext=$ext")
    } else {
        sb.append("ext=[]")
    }
    sb.append("]")
    return sb.toString()
}

@ExperimentalUnsignedTypes
fun ByteArray.print(): String {
    val sb = StringBuilder()
    forEach { sb.append("${it.toUByte()} ") }
    return sb.toString()
}
