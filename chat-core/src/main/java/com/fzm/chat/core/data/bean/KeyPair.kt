package com.fzm.chat.core.data.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author zhengjy
 * @since 2020/12/15
 * Description:公私钥对
 */
@Parcelize
class KeyPair(
    val privateKey: String,
    val publicKey: String
) : Parcelable