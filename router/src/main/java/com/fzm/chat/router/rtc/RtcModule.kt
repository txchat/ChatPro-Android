package com.fzm.chat.router.rtc

/**
 * @author zhengjy
 * @since 2021/07/06
 * Description:
 */
object RtcModule {

    private const val GROUP = "/rtc"

    const val INJECTOR = "$GROUP/injector"

    const val SERVICE = "$GROUP/service"

    const val VIDEO_CALL = "$GROUP/video_call"
}