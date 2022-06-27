package com.fzm.arch.connection.protocol

/**
 * @author zhengjy
 * @since 2021/01/07
 * Description:
 */
object Protocols {

    /**
     * 包长度字段所占字节
     */
    const val PACKAGE_LENGTH = 4

    /**
     * 包头长度字段所占字节
     */
    const val HEADER_LENGTH = 2

    /**
     * 版本字段所占字节
     */
    const val VER_LENGTH = 2

    /**
     * 操作码字段所占字节
     */
    const val OP_LENGTH = 4

    /**
     * 序列号字段所占字节
     */
    const val SEQ_LENGTH = 4

    /**
     * 确认号字段所占字节
     */
    const val ACK_LENGTH = 4

    /**
     * socket消息协议版本号
     */
    const val PROTOCOL_VER = 1

    /**
     * 额外参数字段名
     */
    const val OPTION = "OPTION"
    const val SEQ = "SEQ"
    const val ACK = "ACK"
    const val REQUIRE_ACK = "REQUIRE_ACK"
    const val SEQ_IDENTIFIER = "SEQ_IDENTIFIER"
    const val RETRY_SEQ_IDENTIFIER = "RETRY_SEQ_IDENTIFIER"
}

