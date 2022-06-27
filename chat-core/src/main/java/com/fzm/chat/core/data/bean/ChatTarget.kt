package com.fzm.chat.core.data.bean

import com.fzm.chat.core.data.ChatConst
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2019/09/17
 * Description:聊天对象
 */
data class ChatTarget(
    var channelType: Int,
    var targetId: String
) : Serializable

val ChatTarget.isGroup: Boolean get() = channelType == ChatConst.GROUP_CHANNEL