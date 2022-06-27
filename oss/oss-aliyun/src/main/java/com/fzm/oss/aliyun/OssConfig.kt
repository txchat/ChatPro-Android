package com.fzm.oss.aliyun

import com.zjy.architecture.util.AbstractConfig

/**
 * @author zhengjy
 * @since 2021/01/28
 * Description:阿里云oss配置
 */
object OssConfig {

    private object OssDev : AbstractConfig("oss-dev.properties")
    private object OssPro : AbstractConfig("oss-pro.properties")

    private val config = if (BuildConfig.DEVELOP) OssDev else OssPro

    val END_POINT: String = config["END_POINT"]
    val BUCKET: String = config["BUCKET"]
    val AUTH_SERVER: String = config["AUTH_SERVER"]
}