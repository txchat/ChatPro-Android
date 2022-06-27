package com.fzm.oss.sdk.model

/**
 * @author zhengjy
 * @since 2021/08/20
 * Description:
 */
class UploadPartRequest(
    /**
     * 分段上传任务全局唯一标识
     */
    val uploadId: String,
    /**
     * 分段序号, 范围是1~10000
     */
    val partNumber: Int,
    /**
     * 文件名（包含路径）
     */
    val objectKey: String,
    /**
     * 上传文件的二进制
     */
    val uploadData: ByteArray,
) : OSSRequest()