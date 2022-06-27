package com.fzm.oss.sdk.model

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/18
 * Description:
 */
open class OSSRequest(
    /**
     * 具体请求的oss endPoint
     * 优先于oss整体的endPoint设置
     */
    @Transient
    val endPoint: String? = null,
) : Serializable