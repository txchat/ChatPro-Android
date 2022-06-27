package com.fzm.chat.bean.model

import com.fzm.chat.R
import com.fzm.chat.core.data.bean.Contact
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/06/18
 * Description:
 */
data class ForwardContact(
    /**
     * 联系人选择方式
     *
     * [RECENT_SESSION] 从最近会话列表选择
     * [FRIEND]         从好友列表选择
     * [GROUP]          从群列表选择
     */
    val channel: Int,
    val contact: Contact
) : Serializable {

    companion object {
        const val RECENT_SESSION = 1
        const val FRIEND = 2
        const val GROUP = 3
    }

    fun getListTag(): Int {
        return when (channel) {
            RECENT_SESSION -> R.string.chat_tips_forward_contact_tag1
            FRIEND -> R.string.chat_tips_forward_contact_tag2
            else -> R.string.chat_tips_forward_contact_tag3
        }
    }
}