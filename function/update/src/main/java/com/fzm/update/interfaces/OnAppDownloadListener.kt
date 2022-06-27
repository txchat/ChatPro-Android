package com.fzm.update.interfaces

import java.io.File

/**
 * @author zhengjy
 * @since 2018/08/02
 * Description:
 */
interface OnAppDownloadListener {
    /**
     * 开始下载
     */
    fun onStart()

    /**
     * 下载进度
     */
    fun onProgress(progress: Float)

    /**
     * 下载成功
     *
     * @param force 强制更新
     * @param file App安装文件
     */
    fun onSuccess(force: Boolean, file: File)

    /**
     * 下载失败
     *
     * @param force 强制更新
     * @param e     错误，异常
     */
    fun onFail(force: Boolean, e: Throwable?)
}