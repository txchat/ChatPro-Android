package com.fzm.oss.sdk.model

import android.net.Uri
import java.io.File
import java.io.InputStream

/**
 * @author zhengjy
 * @since 2021/08/18
 * Description:
 */
class PutObjectRequest(
    /**
     * 文件名（包含路径）
     */
    val objectKey: String,
    /**
     * 上传文件的二进制
     */
    val uploadData: ByteArray? = null,
    /**
     * 文件Uri
     */
    val uploadUri: Uri? = null,
    /**
     * 文件输入流
     */
    var fileInput: InputStream? = null,
    /**
     * 文件
     */
    var file: File? = null,
    /**
     * 文件大小
     */
    var contentLength: Long = 0L,
    endPoint: String? = null,
) : OSSRequest(endPoint)