package com.fzm.chat.bean.mnem

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/10
 * Description:
 */
class WalletBackup(
    val mnem: String,
    var selected: Boolean
) : Serializable