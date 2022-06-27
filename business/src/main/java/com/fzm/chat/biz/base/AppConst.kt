package com.fzm.chat.biz.base

import com.king.zxing.Intents

/**
 * @author zhengjy
 * @since 2018/10/22
 * Description:
 */
object AppConst {

    const val SECOND: Long = 1000
    const val MINUTE = 60 * SECOND
    const val HOUR = 60 * MINUTE
    const val DAY = 24 * HOUR
    const val PAGE_SIZE = 20

    const val NEED_LOGIN = 0x00000001

    /**
     * 二维码扫描返回的key
     */
    const val SCAN_RESULT = Intents.Scan.RESULT
}