package com.fzm.update.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import com.fzm.update.interfaces.IUpdateInfo
import com.zjy.architecture.ext.Hash
import com.zjy.architecture.ext.hash
import java.io.File

/**
 * @author zhengjy
 * @since 2020/08/12
 * Description:
 */
/**
 * 获取apk下载目录
 */
fun getDestFileDir(context: Context): File {
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
    return File(dir, "apk")
}

/**
 * 获取apk文件名
 */
fun getDestFileName(info: IUpdateInfo): String {
    return "app-${info.getVersionName()}.apk"
}

/**
 * 判断本地已下载的文件是否是最新版本
 */
private fun isLatestApkFile(context: Context, file: File, info: IUpdateInfo): Boolean {
    if (!file.exists()) {
        return false
    }
    val fileVersion = context.packageManager.getPackageArchiveInfo(file.path, PackageManager.GET_ACTIVITIES)?.versionCode
    val latestFile = fileVersion == info.getVersionCode()
    val sameFile = file.hash(Hash.MD5) == info.getFileMd5()
    return latestFile && sameFile
}

/**
 * 是否已经下载好安装包
 */
fun alreadyDownloaded(context: Context, info: IUpdateInfo): Boolean {
    val dir = getDestFileDir(context)
    val name = getDestFileName(info)
    if (dir.exists()) {
        val file = File(dir, name)
        if (isLatestApkFile(context, file, info)) {
            return true
        }
    }
    return false
}