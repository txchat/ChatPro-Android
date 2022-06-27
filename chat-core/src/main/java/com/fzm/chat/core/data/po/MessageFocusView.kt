package com.fzm.chat.core.data.po

import androidx.room.DatabaseView
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/12/30
 * Description:
 */
@DatabaseView(
    value = "SELECT logId, count(1) as num FROM message_focus_user GROUP BY logId",
    viewName = "view_message_focus"
)
data class MessageFocusView(
    val logId: String,
    val num: Int
) : Serializable