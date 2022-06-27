package com.fzm.chat.core.data.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author zhengjy
 * @since 2020/12/16
 * Description:
 */
@Parcelize
class UserResult(
    val chatServers: List<Server>,
    val groups: List<String>,
    val fields: List<Field>
) : Parcelable