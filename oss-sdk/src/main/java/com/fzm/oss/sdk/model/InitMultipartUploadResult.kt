package com.fzm.oss.sdk.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/20
 * Description:
 */
data class InitMultipartUploadResult(
    /**
     * 文件名（包含路径）
     */
    @SerializedName("key")
    val objectKey: String,
    /**
     * 分段上传任务全局唯一标识
     */
    val uploadId: String,
) : Serializable