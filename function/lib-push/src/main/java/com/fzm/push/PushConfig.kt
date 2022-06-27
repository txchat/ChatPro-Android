package com.fzm.push

import com.zjy.architecture.util.AbstractConfig

/**
 * @author zhengjy
 * @since 2021/04/02
 * Description:
 */
object PushConfig {

    private object PushDev : AbstractConfig("push-dev.properties")
    private object PushPro : AbstractConfig("push-pro.properties")

    private val config = if (BuildConfig.DEVELOP) PushDev else PushPro

    /**
     * 友盟Key
     */
    val UMENG_APP_KEY = config["UMENG_APP_KEY"]
    val UMENG_MESSAGE_SECRET = config["UMENG_MESSAGE_SECRET"]

    /**
     * 小米系统推送通道
     */
    val MI_PUSH_ID = config["MI_PUSH_ID"]
    val MI_PUSH_KEY = config["MI_PUSH_KEY"]

    /**
     * 华为系统推送通道
     */
    val HUAWEI_PUSH_ID = config["HUAWEI_PUSH_ID"]

    /**
     * 魅族系统推送通道
     */
    val MEIZU_PUSH_ID: String = ""
    val MEIZU_PUSH_KEY: String = ""
}