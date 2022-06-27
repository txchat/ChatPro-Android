package com.fzm.oa

import com.fzm.chat.core.BuildConfig
import com.zjy.architecture.util.AbstractConfig

/**
 * @author zhengjy
 * @since 2021/09/06
 * Description:
 */
object OAConfig {

    private object OADev : AbstractConfig("oa-dev.properties")
    private object OAPro : AbstractConfig("oa-pro.properties")

    val config: AbstractConfig = if (BuildConfig.DEVELOP) OADev else OAPro

    val OA_WEB = config["OA_WEB"]

    val OKR_WEB = config["OKR_WEB"]
}