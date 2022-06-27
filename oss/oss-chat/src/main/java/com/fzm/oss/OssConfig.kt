package com.fzm.oss

import com.zjy.architecture.util.AbstractConfig

/**
 * @author zhengjy
 * @since 2021/09/15
 * Description:
 */
object OssConfig {

    private object OssBase : AbstractConfig("oss-base.properties")

    /**
     * oss配置appId
     */
    val APP_ID = OssBase["APP_ID"]
}