package com.fzm.chat.router.biz

/**
 * @author zhengjy
 * @since 2020/12/04
 * Description:
 */
object BizModule {

    private const val GROUP = "/biz"

    const val INJECTOR = "$GROUP/injector"

    const val SERVICE = "$GROUP/service"

    const val WEB_ACTIVITY = "$GROUP/web_activity"

    const val DEEP_LINK = "$GROUP/deep_link"
}