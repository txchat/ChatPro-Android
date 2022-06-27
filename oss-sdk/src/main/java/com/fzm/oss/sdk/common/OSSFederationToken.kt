package com.fzm.oss.sdk.common

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/19
 * Description:
 */
class OSSFederationToken(
    /**
     * 签名
     */
    val signature: String
) : Serializable