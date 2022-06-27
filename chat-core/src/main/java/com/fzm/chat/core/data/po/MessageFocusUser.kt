package com.fzm.chat.core.data.po

import androidx.room.Entity
import androidx.room.Ignore
import com.fzm.chat.core.data.bean.Contact
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/12/27
 * Description:
 */
@Entity(tableName = "message_focus_user", primaryKeys = ["logId", "uid"],)
data class MessageFocusUser(
    /**
     * 消息id
     */
    val logId: Long,
    /**
     * 关注者id
     */
    val uid: String,
    /**
     * 关注时间
     */
    val datetime: Long
) : Serializable {

    /**
     * 关注者信息
     */
    @Ignore
    var contact: Contact? = null
}