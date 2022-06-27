package com.fzm.oss.huaweiyun

import com.obs.services.model.AuthTypeEnum
import com.zjy.architecture.util.AbstractConfig

/**
 * Description:华为云obs配置
 */
object OssConfig {

    private object OssDev : AbstractConfig("oss-dev.properties")
    private object OssPro : AbstractConfig("oss-pro.properties")

    private val config = if (BuildConfig.DEVELOP) OssDev else OssPro

    val END_POINT = config["END_POINT"]
    val BUCKET = config["BUCKET"]
    val AUTH_TYPE = AuthTypeEnum.OBS
    val AUTH_SERVER: String = config["AUTH_SERVER"]
}