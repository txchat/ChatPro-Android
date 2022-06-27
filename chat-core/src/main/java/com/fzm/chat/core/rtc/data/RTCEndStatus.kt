package com.fzm.chat.core.rtc.data

/**
 * @author zhengjy
 * @since 2021/07/22
 * Description:音视频通话状态
 *
 * 1：正常结束
 * 2：拒绝
 * 3：取消
 * 4：忙线
 * 5：未响应
 * 6：通话失败
 */
object RTCEndStatus {
    const val NORMAL = 1
    const val REJECT = 2
    const val CANCEL = 3
    const val BUSY = 4
    const val TIMEOUT = 5
    const val FAIL = 6
}