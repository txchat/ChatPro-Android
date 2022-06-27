package com.fzm.update.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import androidx.core.app.NotificationCompat
import com.zjy.architecture.ext.*
import com.zjy.architecture.util.InstallUtil
import com.fzm.update.interfaces.IUpdateInfo
import com.fzm.update.interfaces.OnAppDownloadListener
import com.fzm.update.R
import com.fzm.update.utils.alreadyDownloaded
import com.fzm.update.utils.getDestFileDir
import com.fzm.update.utils.getDestFileName
import com.fzm.update.provider.UpdateProvider
import com.zjy.architecture.util.ActivityUtils
import com.zjy.architecture.util.immutableFlag
import okhttp3.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.Throws

/**
 * @author zhengjy
 * @since 2020/08/11
 * Description:
 */
class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "notification"
        const val INSTALL_ACTION = "com.zjy.update.service.INSTALL_ACTION"
    }

    // 通知标题
    private var title: String = ""
    // 通知图标
    private var icon = 0
    private lateinit var updateInfo: IUpdateInfo

    private var destFileDir: File? = null
    private var destFileName: String = ""
    private var oldProgress: Int = 0

    private val manager by lazy(LazyThreadSafetyMode.NONE) { notificationManager }
    private val mBuilder by lazy { NotificationCompat.Builder(this, CHANNEL_ID) }
    private val notificationId = 1000

    private var listener: OnAppDownloadListener? = null
    private var onFinishListener: (() -> Unit)? = null
    private var apkFile: File? = null
    private val handler = Handler(Looper.getMainLooper())
    private val receiver = InstallBroadCaseReceiver()

    private val binder = DownloadBinder()

    // 是否正在下载中
    private var isRunning = false

    inner class DownloadBinder : Binder() {
        val service: DownloadService
            get() = this@DownloadService
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(receiver, IntentFilter().apply { addAction(INSTALL_ACTION) })
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent != null) {
            prepareParams(intent)
        }
        // 适配Android8.0通知栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.update_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.update_notification_channel_name_desc)
            manager?.createNotificationChannel(channel)
        }
        return binder
    }

    private fun prepareParams(intent: Intent) {
        title = intent.getStringExtra("title") ?: ""
        icon = intent.getIntExtra("icon", 0)
        updateInfo = intent.getSerializableExtra("updateInfo") as IUpdateInfo
        destFileDir = getDestFileDir(this)
        destFileName = getDestFileName(updateInfo)
    }

    fun startDownload() {
        if (isRunning) {
            return
        }
        isRunning = true
        listener?.onStart()
        if (alreadyDownloaded(this, updateInfo)) {
            val file = File(destFileDir, destFileName)
            apkFile = file
            // 如果本地已经包含了指定版本号的安装包，则不需要下载
            listener?.onSuccess(updateInfo.isForceUpdate(), file)
            finishDownload(true)
            return
        }

        if (updateInfo.getDownloadUrl().isNotEmpty()) {
            startForeground(notificationId, showNotification())
            val client = OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()
            val request: Request = Request.Builder().get().url(updateInfo.getDownloadUrl()).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            val file = saveFile(response)
                            if (file != null) {
                                apkFile = file
                                handler.post { listener?.onSuccess(updateInfo.isForceUpdate(), file) }
                                finishDownload(true)
                            } else {
                                onFailure(call, IOException(getString(R.string.update_error_request)))
                            }
                        } catch (e: Exception) {
                            onFailure(call, IOException(getString(R.string.update_error_request)))
                        }
                    } else {
                        onFailure(call, IOException(getString(R.string.update_error_request)))
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    finishDownload(false)
                    handler.post { listener?.onFail(updateInfo.isForceUpdate(), e) }
                }
            })
        }
    }

    private fun updateProgress(progress: Int) {
        // 更新通知栏
        mBuilder.setProgress(100, progress, false)
            .setSubText("$progress%")
            .setContentText(getString(R.string.update_downloading))
        manager?.notify(notificationId, mBuilder.build())
    }

    private fun finishDownload(success: Boolean) {
        isRunning = false
        // 下载结束，成功或失败都要停止服务，取消通知
        stopForeground(true)
        if (success) {
            // 发送一个新的下载完成的通知
            val builder = NotificationCompat.Builder(this, CHANNEL_ID).apply {
                setContentTitle(title)
                setSmallIcon(icon)
                setContentText(getString(R.string.update_download_success))
                setAutoCancel(true)
                val clickIntent = Intent().setAction(INSTALL_ACTION)
                val contentIntent = PendingIntent.getBroadcast(
                    this@DownloadService, 0, clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT.immutableFlag
                )
                // 指定点击跳转页面
                setContentIntent(contentIntent)
            }
            manager?.notify(notificationId, builder.build())
        } else {
            onFinishListener?.invoke()
        }
    }

    /**
     * 下载的长度/文件的长度
     */
    private fun showNotification(): Notification {
        mBuilder.setContentTitle(title)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setSmallIcon(icon)
            .setContentText(getString(R.string.update_begin_download))
            .setSubText("0%")
        return mBuilder.build()
    }

    /**
     * 将Response解析转换成File
     *
     * @param response  网络请求响应
     * @return          对应文件
     * @throws IOException
     */
    @Throws(IOException::class)
    fun saveFile(response: Response): File? {
        if (response.body() == null) {
            return null
        }
        if (destFileDir?.exists() != true) {
            destFileDir?.mkdirs()
        }
        val file = File(destFileDir, destFileName)
        if (file.exists()) {
            file.delete()
        }
        val buf = ByteArray(4 * 1024)
        val total = response.body()?.contentLength() ?: 1
        response.body()?.byteStream()?.use { input ->
            FileOutputStream(file).use { output ->
                var sum: Long = 0
                var len = 0
                while (input.read(buf).also { len = it } != -1) {
                    sum += len.toLong()
                    output.write(buf, 0, len)
                    val progress = (sum * 1.0f / total * 100).toInt()
                    if (progress != oldProgress) {
                        oldProgress = progress
                        updateProgress(progress)
                        handler.post { listener?.onProgress(sum * 1.0f / total) }
                    }
                }
                output.flush()
            }
        }
        response.body()?.close()
        return file
    }

    fun setOnAppDownloadListener(listener: OnAppDownloadListener) {
        this.listener = listener
    }

    fun setOnFinishListener(action: () -> Unit) {
        this.onFinishListener = action
    }

    inner class InstallBroadCaseReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == INSTALL_ACTION) {
                if (context != null && apkFile != null) {
                    val activity = ActivityUtils.getTopActivity()
                    if (activity != null) {
                        InstallUtil.install(activity, apkFile!!, UpdateProvider.authority(this@DownloadService))
                    }
                }
                onFinishListener?.invoke()
            }
        }
    }
}