package com.fzm.rtc

/**
 * @author zhengjy
 * @since 2021/07/20
 * Description:
 */
object RTCConst {
    /**
     * 进入当前正在等待处理的通话
     */
    const val ACTION_ENTER_CALL = "com.fzm.rtc.action.ACTION_ENTER_CALL"

    /**
     * 接听当前正在等待处理的通话
     */
    const val ACTION_ACCEPT = "com.fzm.rtc.action.ACTION_ACCEPT_CALL"

    /**
     * 拒绝当前正在等待处理的通话
     */
    const val ACTION_REJECT = "com.fzm.rtc.action.ACTION_REJECT_CALL"

    /**
     * 当前正在等待处理的通话被切到后台
     */
    const val ACTION_INCOMING_BACKGROUND = "com.fzm.rtc.action.ACTION_INCOMING_BACKGROUND"
}