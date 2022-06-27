package com.fzm.chat.router.oss

import android.net.Uri
import com.alibaba.android.arouter.facade.template.IProvider

/**
 * @author zhengjy
 * @since 2021/01/28
 * Description:
 */
interface OssService : IProvider {

    /**
     * 上传文件，返回url
     */
    suspend fun uploadMedia(endPoint: String? = null, uri: Uri?, @MediaType type: Int): String

    /**
     * 上传文件，返回url
     */
    suspend fun uploadMedia(endPoint: String? = null, path: String?, @MediaType type: Int): String
}