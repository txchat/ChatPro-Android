package com.fzm.chat.core.data.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/16
 * Description:服务器地址
 */
@Parcelize
data class Server(
    val id: String,
    val name: String,
    val address: String,
) : Parcelable, Serializable