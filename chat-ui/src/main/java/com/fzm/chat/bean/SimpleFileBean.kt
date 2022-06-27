package com.fzm.chat.bean

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/03/16
 * Description:
 */
data class SimpleFileBean(
    val name: String?,
    val size: Long,
    val md5: String?,
    var path: String?,
) : Serializable