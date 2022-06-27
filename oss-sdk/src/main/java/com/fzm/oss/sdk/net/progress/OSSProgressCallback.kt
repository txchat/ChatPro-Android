package com.fzm.oss.sdk.net.progress

/**
 * @author zhengjy
 * @since 2021/08/23
 * Description:
 */
interface OSSProgressCallback<T> {

    fun onProgress(request: T, currentSize: Long, totalSize: Long)
}