package com.fzm.oss.sdk

import android.content.Context
import com.fzm.oss.sdk.common.OSSCredentialProvider
import com.fzm.oss.sdk.exception.ClientException
import com.fzm.oss.sdk.model.*
import com.fzm.oss.sdk.net.OssApi
import com.fzm.oss.sdk.net.progress.OSSProgressCallback
import com.fzm.oss.sdk.net.progress.ProgressTouchableRequestBody
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * @author zhengjy
 * @since 2021/08/20
 * Description:
 */
class OSSImpl(
    private val mContext: Context,
    private var endPoint: String,
    private val provider: OSSCredentialProvider,
    private val conf: ClientConfiguration? = null,
) : OSS {

    companion object {
        private const val HEADER_SIGN = "FZM-SIGNATURE"

        /**
         * 最大并发请求数
         */
        private const val MAX_REQUEST = 10
    }

    private var okHttpClient: OkHttpClient
    private var retrofit: Retrofit

    private val api: OssApi

    init {
        val builder = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .cache(null)

        if (conf != null) {
            builder.connectTimeout(conf.connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(conf.socketTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(conf.socketTimeout, TimeUnit.MILLISECONDS)
                .dispatcher(Dispatcher().apply { maxRequests = MAX_REQUEST })
        }
        if (OSSClient.debug) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.HEADERS
            builder.addInterceptor(loggingInterceptor)
        }

        builder.addInterceptor { chain ->
            val original = chain.request()
            val newRequest = original.newBuilder().apply {
                addHeader(HEADER_SIGN, provider.getFederationToken().signature)
            }.build()
            chain.proceed(newRequest)
        }

        okHttpClient = builder.build()

        retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .baseUrl(endPoint)
            .build()

        api = retrofit.create(OssApi::class.java)
    }

    private fun obtainPartMap(): MutableMap<String, RequestBody> =
        mutableMapOf<String, RequestBody>().also {
            conf?.ossType?.also { type ->
                it["ossType"] = RequestBody.create(MultipartBody.FORM, type)
            }
            it["appId"] = RequestBody.create(MultipartBody.FORM, conf?.appId ?: "")
        }

    private fun obtainBodyMap(): MutableMap<String, Any> =
        mutableMapOf<String, Any>().also {
            conf?.ossType?.also { type ->
                it["ossType"] = type
            }
            it["appId"] = conf?.appId ?: ""
        }

    /**
     * 动态修改endPoint
     */
    override fun setEndPoint(endPoint: String) {
        this.endPoint = endPoint
    }

    override suspend fun putObject(request: PutObjectRequest, callback: OSSProgressCallback<PutObjectRequest>?): OSSObject {
        val ossServer = if (!request.endPoint.isNullOrEmpty()) request.endPoint else endPoint
        var inputStream: InputStream? = null
        try {
            when {
                request.fileInput != null -> {
                    inputStream = request.fileInput
                    if (request.contentLength == 0L) {
                        request.contentLength = request.fileInput!!.available().toLong()
                    }
                }
                request.file != null -> {
                    inputStream = request.file!!.inputStream()
                    if (request.contentLength == 0L) {
                        request.contentLength = request.file!!.length()
                    }
                }
                request.uploadData != null -> {
                    inputStream = ByteArrayInputStream(request.uploadData)
                    if (request.contentLength == 0L) {
                        request.contentLength = request.uploadData.size.toLong()
                    }
                }
                request.uploadUri != null -> {
                    inputStream = withContext(Dispatchers.IO) {
                        try {
                            if (request.contentLength == 0L) {
                                request.contentLength = mContext.contentResolver.openFileDescriptor(
                                    request.uploadUri,
                                    "r"
                                )?.statSize ?: 0
                            }
                            mContext.contentResolver.openInputStream(request.uploadUri)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw ClientException(e)
        }
        val map = obtainPartMap().apply {
            put("key", RequestBody.create(MultipartBody.FORM, request.objectKey))
            put(
                "file\"; filename=\"file",
                ProgressTouchableRequestBody(
                    inputStream,
                    request.contentLength,
                    null,
                    request,
                    callback
                )
            )
        }
        return call { api.putObject("${ossServer}/oss/upload", map) }
    }

    override suspend fun initMultipartUpload(request: InitMultipartUploadRequest): InitMultipartUploadResult {
        val ossServer = if (!request.endPoint.isNullOrEmpty()) request.endPoint else endPoint
        val map = obtainBodyMap().apply {
            put("key", request.objectKey)
        }
        return call { api.initMultipartUpload("${ossServer}/oss/init-multipart-upload", map) }
    }

    override suspend fun uploadPart(request: UploadPartRequest, callback: OSSProgressCallback<UploadPartRequest>?): UploadPartResult {
        val ossServer = if (!request.endPoint.isNullOrEmpty()) request.endPoint else endPoint
        val map = obtainPartMap().apply {
            put("uploadId", RequestBody.create(MultipartBody.FORM, request.uploadId))
            put("partNumber", RequestBody.create(MultipartBody.FORM, request.partNumber.toString()))
            put("key", RequestBody.create(MultipartBody.FORM, request.objectKey))
            put(
                "file\"; filename=\"file",
                ProgressTouchableRequestBody(
                    request.uploadData,
                    request.uploadData.size.toLong(),
                    null,
                    request,
                    callback
                )
            )
        }
        return call { api.uploadPart("${ossServer}/oss/upload-part", map) }
    }

    override suspend fun completeMultipartUpload(request: CompleteMultipartUploadRequest): OSSObject {
        val ossServer = if (!request.endPoint.isNullOrEmpty()) request.endPoint else endPoint
        val map = obtainBodyMap().apply {
            put("key", request.objectKey)
            put("parts", request.parts)
            put("uploadId", request.uploadId)
        }
        return call { api.completeMultipartUpload("${ossServer}/oss/complete-multipart-upload", map) }
    }

    override suspend fun cancelMultipartUpload(request: CancelMultipartUploadRequest): Any {
        val ossServer = if (!request.endPoint.isNullOrEmpty()) request.endPoint else endPoint
        val map = obtainBodyMap().apply {
            put("key", request.objectKey)
            put("uploadId", request.uploadId)
        }
        return call { api.cancelMultipartUpload("${ossServer}/oss/abort-multipart-upload", map) }
    }
}