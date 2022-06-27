package com.fzm.chat.bean

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/06/07
 * Description:
 */
data class MuteParams(
    val gid: Long,
    val address: String,
    var muteTime: Long = 0L,
    var name: String? = null
) : Serializable