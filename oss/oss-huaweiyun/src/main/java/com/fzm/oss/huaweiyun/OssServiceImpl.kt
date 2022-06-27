package com.fzm.oss.huaweiyun

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.oss.MediaType
import com.fzm.chat.router.oss.OssModule
import com.fzm.chat.router.oss.OssService
import com.obs.services.ObsClient
import com.obs.services.ObsConfiguration
import com.obs.services.model.HttpMethodEnum
import com.obs.services.model.PutObjectRequest
import com.obs.services.model.TemporarySignatureRequest
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.util.FileUtils
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Route(path = OssModule.HUAWEI_OSS)
class OssServiceImpl : OssService, CoroutineScope {

    private lateinit var obs: ObsClient
    private lateinit var mContext: Context
    private lateinit var clientDeferred: Deferred<ObsClient>
    private val delegate by rootScope.inject<LoginDelegate>()

    private var folder = "chatList/picture/"
    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.IO


    @SuppressLint("MissingPermission")
    override suspend fun uploadMedia(endPoint: String?, uri: Uri?, @MediaType type: Int): String {
        return withContext(coroutineContext) {
            if (uri == null) return@withContext ""
            waitObsInit()
            val request = PutObjectRequest().apply {
                file = FileUtils.copyToCacheFile(mContext, uri)
                bucketName = OssConfig.BUCKET
                objectKey = getObjectKey(getExtension(mContext, uri))
            }
            uploadMediaCancelable(request, type)
        }
    }

    private suspend fun waitObsInit() {
        if (!this::obs.isInitialized) obs = clientDeferred.await()
    }

    override suspend fun uploadMedia(endPoint: String?, path: String?, @MediaType type: Int): String {
        return withContext(coroutineContext) {
            if (path.isNullOrEmpty()) return@withContext ""
            waitObsInit()
            val request = PutObjectRequest().apply {
                file = File(path)
                bucketName = OssConfig.BUCKET
                objectKey = getObjectKey(getExtension(path))
            }
            uploadMediaCancelable(request, type)
        }
    }

    override fun init(context: Context) {
        mContext = context
        val config = ObsConfiguration().apply {
            endPoint = OssConfig.END_POINT
            authType = OssConfig.AUTH_TYPE
            connectionTimeout = 15 * 1000
            socketTimeout = 15 * 1000
            maxConnections = 5
            maxErrorRetry = 2
        }
        clientDeferred = async {
            ObsClient(OssCustomCredentialsProvider(OssConfig.AUTH_SERVER), config)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun uploadMediaCancelable(request: PutObjectRequest, @MediaType type: Int) =
        suspendCancellableCoroutine<String> {
            folder = when (type) {
                MediaType.PICTURE -> "chatList/picture/"
                MediaType.AUDIO -> "chatList/audio/"
                MediaType.FILE -> "chatList/file/"
                MediaType.VIDEO -> "chatList/video/"
                else -> "chatList/picture/"
            }
            try {
                val putObjectResult = obs.putObject(request)
                val signRequest = TemporarySignatureRequest(
                    HttpMethodEnum.GET,
                    TimeUnit.DAYS.toSeconds(365)
                ).apply {
                    bucketName = OssConfig.BUCKET
                    objectKey = putObjectResult.objectKey
                }
                val response = obs.createTemporarySignature(signRequest)
                it.resume(Uri.parse(response.signedUrl).buildUpon().clearQuery().build().toString())
            } catch (e: Throwable) {
                it.resumeWithException(e)
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

    private fun getExtension(context: Context, uri: Uri?): String {
        if (uri == null) return ""
        val ext = context.contentResolver.getType(uri)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(ext) ?: ""
    }

    private fun getExtension(path: String): String = path.split(".").lastOrNull() ?: ""
}