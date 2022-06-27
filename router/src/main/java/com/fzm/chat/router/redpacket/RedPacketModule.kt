package com.fzm.chat.router.redpacket

/**
 * @author zhengjy
 * @since 2021/08/03
 * Description:
 */
object RedPacketModule {

    private const val GROUP = "/red_packet"

    const val INJECTOR = "$GROUP/injector"

    const val SERVICE = "$GROUP/service"

    const val SEND_RED_PACKET = "$GROUP/send_red_packet"

    const val RED_PACKET_DETAIL = "$GROUP/red_packet_detail"

    const val RED_PACKET_RECORD = "$GROUP/red_packet_record"

    const val PACKET_DIALOG = "$GROUP/red_packet_dialog"

}