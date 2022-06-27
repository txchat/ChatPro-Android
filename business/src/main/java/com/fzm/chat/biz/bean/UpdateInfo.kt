package com.fzm.chat.biz.bean

import com.fzm.update.interfaces.IUpdateInfo
import com.google.gson.annotations.SerializedName

/**
 * @author zhengjy
 * @since 2021/04/01
 * Description:
 */
class UpdateInfo(
    @SerializedName("versionCode")
    val code: Int,
    @SerializedName("versionName")
    val name: String,
    @SerializedName("description")
    val desc: List<String>?,
    val url: String,
    val force: Boolean,
    val size: Long,
    val md5: String?
) : IUpdateInfo {

    override fun getVersionCode(): Int {
        return code
    }

    override fun getVersionName(): String {
        return name
    }

    override fun getDescription(): List<String>? {
        return desc
    }

    override fun getDownloadUrl(): String {
        return url
    }

    override fun isForceUpdate(): Boolean {
        return force
    }

    override fun getApkSize(): Long {
        return size
    }

    override fun getFileMd5(): String? {
        return md5
    }
}