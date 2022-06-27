package com.fzm.oss.aliyun

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.sdk.android.oss.*
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.PutObjectResult
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.oss.MediaType
import com.fzm.chat.router.oss.MediaType.*
import com.fzm.chat.router.oss.OssModule
import com.fzm.chat.router.oss.OssService
import com.zjy.architecture.di.rootScope
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * @author zhengjy
 * @since 2021/01/28
 * Description:
 */
@Route(path = OssModule.ALIYUN_OSS)
class OssServiceImpl : OssService, CoroutineScope {

    private lateinit var mContext: Context
    private lateinit var oss: OSS
    private val delegate by rootScope.inject<LoginDelegate>()

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Main

    private var folder = "chatList/picture/"

    override fun init(context: Context) {
        mContext = context
        val provider = OSSCustomAuthCredentialsProvider(OssConfig.AUTH_SERVER)
        val conf = ClientConfiguration().apply {
            // 连接超时，默认15秒。
            connectionTimeout = 15 * 1000
            // socket超时，默认15秒。
            socketTimeout = 15 * 1000
            // 最大并发请求数，默认5个。
            maxConcurrentRequest = 5
            // 失败后最大重试次数，默认2次。
            maxErrorRetry = 2
        }
        oss = OSSClient(mContext, OssConfig.END_POINT, provider, conf)
    }

    override suspend fun uploadMedia(endPoint: String?, uri: Uri?, @MediaType type: Int): String {
        return withContext(coroutineContext) {
            if (uri == null) return@withContext ""
            val request = PutObjectRequest(
                OssConfig.BUCKET,
                getObjectKey(getExtension(mContext, uri)),
                uri
            )
            uploadMediaCancelable(request, type)
        }
    }

    override suspend fun uploadMedia(endPoint: String?, path: String?, @MediaType type: Int): String {
        return withContext(coroutineContext) {
            if (path.isNullOrEmpty()) return@withContext ""
            val request = PutObjectRequest(OssConfig.BUCKET, getObjectKey(getExtension(path)), path)
            uploadMediaCancelable(request, type)
        }
    }

    private suspend fun uploadMediaCancelable(
        request: PutObjectRequest,
        @MediaType type: Int
    ): String {
        return suspendCancellableCoroutine { cont ->
            folder = when (type) {
                PICTURE -> "chatList/picture/"
                AUDIO -> "chatList/audio/"
                FILE -> "chatList/file/"
                VIDEO -> "chatList/video/"
                else -> "chatList/picture/"
            }
            val task = oss.asyncPutObject(
                request,
                object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                    override fun onSuccess(request: PutObjectRequest, result: PutObjectResult) {
                        val url =
                            oss.presignPublicObjectURL(OssConfig.BUCKET, request.objectKey)
                        cont.resume(url)
                    }

                    override fun onFailure(
                        request: PutObjectRequest,
                        clientException: ClientException?,
                        serviceException: ServiceException?
                    ) {
                        when {
                            clientException != null -> cont.resumeWithException(clientException)
                            serviceException != null -> cont.resumeWithException(serviceException)
                            else -> cont.resumeWithException(Exception("fail to upload to oss."))
                        }
                    }
                })
            cont.invokeOnCancellation {
                task.cancel()
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getObjectKey(suffix: String): String {
        val timeL = System.currentTimeMillis()
        val format = SimpleDateFormat("yyyyMMdd")
        val time = format.format(timeL)
        val format1 = SimpleDateFormat("HHmm")
        val time1 = format1.format(timeL)
        val format2 = SimpleDateFormat("ssSSS")
        val time2 = format2.format(timeL)
        val objectKey = StringBuilder("")
        try {
            objectKey.append(folder)
            objectKey.append(time)
            objectKey.append("/")
            if (ChatConfig.FILE_ENCRYPT) {
                objectKey.append(ChatConst.ENC_PREFIX)
                objectKey.append("_")
            }
            objectKey.append(time)
            objectKey.append(time1)
            objectKey.append(time2)
            objectKey.append("_")
            objectKey.append(delegate.getAddress())
            objectKey.append("_")
            objectKey.append(generateRandomString(5))
            if (suffix.isNotEmpty()) {
                objectKey.append(".")
                objectKey.append(suffix)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return objectKey.toString()
    }

    private fun getExtension(context: Context, uri: Uri?): String {
        if (uri == null) return ""
        val ext = context.contentResolver.getType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(ext) ?: ""
    }

    private fun getExtension(path: String): String = path.split(".").lastOrNull() ?: ""

    private fun generateRandomString(length: Int): String {
        val str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = Random()
        val sb = StringBuilder()
        for (i in 0 until length) {
            val number = random.nextInt(62)
            sb.append(str[number])
        }
        return sb.toString()
    }
}