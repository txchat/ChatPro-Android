package com.fzm.chat.core.data.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author zhengjy
 * @since 2021/01/13
 * Description:
 */
@Parcelize
data class BackupMnemonic(
    val address: String?,
    val phone: String?,
    val email: String?,
    val mnemonic: String?
) : Parcelable