package com.fzm.oss.sdk

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.fzm.oss.sdk.common.OSSCredentialProvider
import com.fzm.oss.sdk.exception.ClientException
import com.fzm.oss.sdk.model.*
import com.fzm.oss.sdk.net.progress.OSSProgressCallback
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.math.min

/**
 * @author zhengjy
 * @since 2021/08/18
 * Description:
 */
class OSSClient(
    private val mContext: Context,
    endPoint: String,
    provider: OSSCredentialProvider,
    conf: ClientConfiguration? = null,
) : OSS {

    companion object {

        private const val TAG = "FZM-OSS"

        const val debug = false

        /**
         * 文件分片阈值
         */
        private const val MULTIPART_LIMIT = 15 * 1024 * 1024L

        /**
         * 文件分片大小
         */
        private const val CHUNK_SIZE = 10 * 1024 * 1024L

        /**
         * 最大并发分片数
         */
        private const val MAX_CONCURRENT_CHUNK = 5
    }

    private val oss: OSS

    init {
        oss = OSSImpl(mContext, endPoint, provider, conf)
    }

    override fun setEndPoint(endPoint: String) {
        oss.setEndPoint(endPoint)
    }

    override suspend fun putObject(request: PutObjectRequest, callback: OSSProgressCallback<PutObjectRequest>?): OSSObject {
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

        if (inputStream == null) {
            throw ClientException("input is empty")
        }
        val start = SystemClock.uptimeMillis()
        return if (request.contentLength < MULTIPART_LIMIT) {
            oss.putObject(request.apply {
                if (fileInput == null) fileInput = inputStream
            }, callback).also {
                if (debug) {
                    //putObject cost:7187
                    Log.d(TAG, "putObject cost:${SystemClock.uptimeMillis() - start}")
                }
            }
        } else {
            startMultipartUpload(inputStream, request, callback).also {
                if (debug) {
                    //startMultipartUpload cost:6875
                    Log.d(TAG, "startMultipartUpload cost:${SystemClock.uptimeMillis() - start}")
                }
            }
        }
    }

    override suspend fun initMultipartUpload(request: InitMultipartUploadRequest): InitMultipartUploadResult {
        return oss.initMultipartUpload(request)
    }

    override suspend fun uploadPart(request: UploadPartRequest, callback: OSSProgressCallback<UploadPartRequest>?): UploadPartResult {
        return oss.uploadPart(request, callback)
    }

    override suspend fun completeMultipartUpload(request: CompleteMultipartUploadRequest): OSSObject {
        return oss.completeMultipartUpload(request)
    }

    override suspend fun cancelMultipartUpload(request: CancelMultipartUploadRequest): Any {
        return oss.cancelMultipartUpload(request)
    }

    /**
     * 开始分片上传流程
     *
     * @param input         文件的输入流
     * @param original      原始请求
     *
     */
    private suspend fun startMultipartUpload(input: InputStream, original: PutObjectRequest, callback: OSSProgressCallback<PutObjectRequest>?): OSSObject {
        val contentLength = original.contentLength
        val objectKey = original.objectKey

        val init = initMultipartUpload(InitMultipartUploadRequest(objectKey))
        val uploadId = init.uploadId

        return withContext(Dispatchers.IO + Job()/* 任何一个分片失败都要取消父协程，所以使用Job() */) {
            val queue = ArrayDeque<Deferred<CompleteMultipartUploadRequest.Part>>(MAX_CONCURRENT_CHUNK)
            val progressMap by lazy { mutableMapOf<UploadPartRequest, Long>() }
            try {
                val blockNum = if (contentLength % CHUNK_SIZE == 0L) {
                    (contentLength / CHUNK_SIZE).toInt()
                } else (contentLength / CHUNK_SIZE + 1).toInt()

                val parts = mutableListOf<CompleteMultipartUploadRequest.Part>()
                var sum = 0L
                for (i in 1..blockNum) {
                    val buffer = if (i == blockNum) {
                        // 最后一块
                        ByteArray(min(CHUNK_SIZE, contentLength - sum).toInt())
                    } else {
                        ByteArray(CHUNK_SIZE.toInt())
                    }
                    val count = input.read(buffer)
                    sum += count
                    if (callback != null) {
                        addUploadTask(queue, uploadId, objectKey, i, buffer, object : OSSProgressCallback<UploadPartRequest> {
                            var lastProgressTime = 0L
                            override fun onProgress(request: UploadPartRequest, currentSize: Long, totalSize: Long) {
                                progressMap[request] = currentSize
                                var upload = 0L
                                progressMap.forEach { upload += it.value }
                                if (upload > 0 && (System.currentTimeMillis() - lastProgressTime > 100 || upload == contentLength)) {
                                    callback.onProgress(original, upload, contentLength)
                                    lastProgressTime = System.currentTimeMillis()
                                }
                            }
                        })
                    } else {
                        addUploadTask(queue, uploadId, objectKey, i, buffer, null)
                    }
                    while (queue.isNotEmpty()) {
                        if (i == blockNum) {
                            // 如果最后一块上传任务已经提交，则一直等待所有任务完成
                            val job = queue.removeFirst()
                            parts.add(job.await())
                        } else {
                            val job = queue.first()
                            if (job.isCompleted || queue.size == MAX_CONCURRENT_CHUNK) {
                                parts.add(job.await())
                                queue.remove(job)
                            } else {
                                break
                            }
                        }
                    }
                }
                completeMultipartUpload(CompleteMultipartUploadRequest(uploadId, objectKey, parts))
            } catch (e: Exception) {
                if (debug) Log.e(TAG, "multipart upload fail, canceled:${e.message}")
                withContext(NonCancellable) {
                    cancelMultipartUpload(CancelMultipartUploadRequest(uploadId, objectKey))
                }
                throw e
            } finally {
                input.close()
            }
        }
    }

    /**
     * 添加分片任务
     *
     * @param queue     任务队列
     * @param uploadId  全局唯一分片任务id
     * @param objectKey 上传文件key
     * @param number    分片号
     * @param buffer    分片数据
     *
     */
    private fun CoroutineScope.addUploadTask(
        queue: ArrayDeque<Deferred<CompleteMultipartUploadRequest.Part>>,
        uploadId: String,
        objectKey: String,
        number: Int,
        buffer: ByteArray,
        callback: OSSProgressCallback<UploadPartRequest>?
    ) {
        val deferred = async {
            val result = uploadPart(UploadPartRequest(uploadId, number, objectKey, buffer), callback)
            CompleteMultipartUploadRequest.Part(result.ETag, result.partNumber)
        }
        queue.add(deferred)
    }
}