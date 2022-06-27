package com.fzm.oss.sdk.model

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/20
 * Description:
 */
class CompleteMultipartUploadRequest(
    /**
     * 分段上传任务全局唯一标识
     */
    val uploadId: String,
    /**
     * 文件名（包含路径）
     */
    val objectKey: String,
    /**
     * 分段信息
     */
    val parts: List<Part>,
) : OSSRequest() {

    data class Part(
        val Etag: String,
        val partNumber: Int
    ) : Serializable
}