package com.fzm.update

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.fzm.update.interfaces.IUpdateInfo
import com.fzm.update.interfaces.OnAppDownloadListener
import com.fzm.update.service.DownloadService

/**
 * @author zhengjy
 * @since 2020/08/11
 * Description:
 */
object UpdateManager {

    private lateinit var updateInfo: IUpdateInfo
    private lateinit var title: String
    private var icon: Int = 0

    private var service: DownloadService? = null

    fun with(updateInfo: IUpdateInfo): UpdateManager {
        UpdateManager.updateInfo = updateInfo
        return this
    }

    fun setTitle(title: String): UpdateManager {
        UpdateManager.title = title
        return this
    }

    fun setIcon(icon: Int): UpdateManager {
        UpdateManager.icon = icon
        return this
    }

    fun start(activity: Activity, listener: OnAppDownloadListener) {
        val intent = Intent(activity, DownloadService::class.java)
            .putExtra("title", title)
            .putExtra("icon", icon)
            .putExtra("updateInfo", updateInfo)
        activity.bindService(intent, object : ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName?) {

            }

            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = (binder as DownloadService.DownloadBinder).service
                // 设置下载进度监听
                service?.setOnAppDownloadListener(listener)
                service?.setOnFinishListener { activity.unbindService(this) }
                // 开始下载安装包
                service?.startDownload()
            }
        }, Context.BIND_AUTO_CREATE)
    }
}