package com.fzm.oss.sdk.model

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/20
 * Description:
 */
data class UploadPartResult(
    /**
     * 段数据的MD5值
     */
    val ETag: String,
    /**
     * 文件名(包含路径)
     */
    val key: String,
    /**
     * 分段序号, 范围是1~10000
     */
    val partNumber: Int,
    /**
     * 分段上传任务全局唯一标识
     */
    val uploadId: String
) : Serializable