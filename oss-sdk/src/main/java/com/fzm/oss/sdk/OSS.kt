package com.fzm.oss.sdk

import com.fzm.oss.sdk.exception.ClientException
import com.fzm.oss.sdk.exception.ServerException
import com.fzm.oss.sdk.model.*
import com.fzm.oss.sdk.net.progress.OSSProgressCallback
import kotlin.jvm.Throws

/**
 * @author zhengjy
 * @since 2021/08/18
 * Description:
 */
interface OSS {

    /**
     * 动态修改endPoint
     */
    fun setEndPoint(endPoint: String)

    /**
     * 简单上传文件
     *
     * @param request   上传请求参数
     * @param callback  上传进度回调
     * @return
     */
    @Throws(ClientException::class, ServerException::class)
    suspend fun putObject(request: PutObjectRequest, callback: OSSProgressCallback<PutObjectRequest>? = null): OSSObject

    /**
     * 初始化分片上传
     *
     * @param request   初始化分片请求参数
     * @return
     */
    @Throws(ServerException::class)
    suspend fun initMultipartUpload(request: InitMultipartUploadRequest): InitMultipartUploadResult

    /**
     * 上传文件分片
     *
     * @param request   分片上传参数
     * @param callback  上传进度回调
     * @return
     */
    @Throws(ClientException::class, ServerException::class)
    suspend fun uploadPart(request: UploadPartRequest, callback: OSSProgressCallback<UploadPartRequest>?): UploadPartResult

    /**
     * 合并文件
     *
     * @param request   合并分片请求参数
     * @return
     */
    @Throws(ServerException::class)
    suspend fun completeMultipartUpload(request: CompleteMultipartUploadRequest): OSSObject

    /**
     * 取消文件分片上传
     *
     * @param request   取消分片请求参数
     * @return
     */
    @Throws(ServerException::class)
    suspend fun cancelMultipartUpload(request: CancelMultipartUploadRequest): Any
}