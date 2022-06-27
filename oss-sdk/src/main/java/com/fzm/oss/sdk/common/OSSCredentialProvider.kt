package com.fzm.oss.sdk.common

import com.fzm.oss.sdk.exception.ClientException
import kotlin.jvm.Throws

/**
 * @author zhengjy
 * @since 2021/08/19
 * Description:
 */
interface OSSCredentialProvider {
    /**
     * get OSSFederationToken instance
     *
     * @return
     */
    @Throws(ClientException::class)
    fun getFederationToken(): OSSFederationToken
}