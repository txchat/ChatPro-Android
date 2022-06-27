package com.fzm.chat.core.data.msg

import android.os.Bundle
import com.fzm.arch.connection.type.ConfirmType
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/03/12
 * Description:
 */
data class MsgConfirmResult(
    val identifier: String,
    val success: ConfirmType,
    val extra: Bundle?
) : Serializable