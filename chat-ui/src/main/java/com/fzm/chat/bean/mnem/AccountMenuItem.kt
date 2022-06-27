package com.fzm.chat.bean.mnem

import android.os.Bundle
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/15
 * Description:
 */
data class AccountMenuItem(
    val icon: Int,
    val name: String,
    var subname: String,
    var mark: Boolean,
    val route: Pair<String, Bundle?>? = null,
    val action: (() -> Unit)? = null
) : Serializable