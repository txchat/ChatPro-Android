package com.fzm.chat.core.data.bean

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/05/27
 * Description:
 */
data class StringList(
    @SerializedName(value = "users", alternate = ["memberIds"])
    val strings: List<String>
) : Serializable