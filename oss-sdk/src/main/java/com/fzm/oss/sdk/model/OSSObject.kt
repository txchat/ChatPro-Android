package com.fzm.oss.sdk.model

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/18
 * Description:
 */
data class OSSObject(
    /**
     * 上传资源链接
     */
    val url: String
) : Serializable