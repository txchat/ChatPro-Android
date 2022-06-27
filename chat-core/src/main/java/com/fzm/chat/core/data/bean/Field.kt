package com.fzm.chat.core.data.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author zhengjy
 * @since 2020/12/16
 * Description:合约存储字段
 */
@Parcelize
class Field(
    val name: String,
    val value: String,
    val type: Int,
    val level: String = "public"
) : Parcelable