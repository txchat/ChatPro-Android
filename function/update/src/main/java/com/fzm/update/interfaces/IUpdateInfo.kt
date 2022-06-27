package com.fzm.update.interfaces

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2018/08/03
 * Description:更新信息类需要实现的接口
 */
interface IUpdateInfo : Serializable {
    /**
     * 获取版本号
     */
    fun getVersionCode(): Int

    /**
     * 获取版本名
     */
    fun getVersionName(): String

    /**
     * 获取更新描述信息
     */
    fun getDescription(): List<String>?

    /**
     * 获取App下载链接
     */
    fun getDownloadUrl(): String

    /**
     * 是否为强制更新
     */
    fun isForceUpdate(): Boolean

    /**
     * 获取安装包大小
     */
    fun getApkSize(): Long

    /**
     * 安装文件的md5
     */
    fun getFileMd5(): String?
}