package com.fzm.oss.sdk.net

import com.fzm.oss.sdk.model.*
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * @author zhengjy
 * @since 2021/08/20
 * Description:
 */
@JvmSuppressWildcards
interface OssApi {

    @Multipart
    @POST
    suspend fun putObject(
        @Url url: String,
        @PartMap map: Map<String, RequestBody>,
    ): OSSResult<OSSObject>

    @POST
    suspend fun initMultipartUpload(
        @Url url: String,
        @Body map: Map<String, Any>
    ): OSSResult<InitMultipartUploadResult>

    @Multipart
    @POST
    suspend fun uploadPart(
        @Url url: String,
        @PartMap map: Map<String, RequestBody>
    ): OSSResult<UploadPartResult>

    @POST
    suspend fun completeMultipartUpload(
        @Url url: String,
        @Body map: Map<String, Any>
    ): OSSResult<OSSObject>

    @POST
    suspend fun cancelMultipartUpload(
        @Url url: String,
        @Body map: Map<String, Any>
    ): OSSResult<Any>
}