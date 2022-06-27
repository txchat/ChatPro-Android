package com.fzm.oss.sdk.model

/**
 * @author zhengjy
 * @since 2021/08/20
 * Description:
 */
class InitMultipartUploadRequest(
    /**
     * 文件名（包含路径）
     */
    val objectKey: String,
) : OSSRequest()