package com.fzm.oss.sdk.model

/**
 * @author zhengjy
 * @since 2021/08/20
 * Description:
 */
class CancelMultipartUploadRequest(
    /**
     * 分段上传任务全局唯一标识
     */
    val uploadId: String,
    /**
     * 文件名（包含路径）
     */
    val objectKey: String,
) : OSSRequest()