package com.fzm.chat.core.data.bean

import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.contact
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/04/20
 * Description:
 */
data class MessageOption(
    val option: Option,
    val payload: Any
) : Serializable {

    init {
        when (option) {
            Option.ADD_MSG -> check(payload is ChatMessage) { "payload must be ChatMessage" }
            Option.REMOVE_MSG -> check(payload is ChatMessage) { "payload must be ChatMessage" }
            Option.UPDATE_STATE -> check(payload is ChatMessage) { "payload must be ChatMessage" }
            Option.UPDATE_CONTENT -> check(payload is ChatMessage) { "payload must be ChatMessage" }
            Option.REVOKE_MSG -> check(payload is ChatMessage) { "payload must be ChatMessage" }
            Option.UPDATE_FOCUS -> check(payload is MsgFocus) { "payload must be MsgFocus" }
            Option.UPDATE_CONTACT -> check(payload is Contact) { "payload must be Contact" }
        }
    }
}

data class MsgFocus(
    val logId: Long,
    val num: Int,
    val contact: String?,
    val hasFocused: Boolean
): Serializable

val MessageOption.contact: String?
    get() = if (option == Option.UPDATE_FOCUS) focus.contact else message.contact

val MessageOption.message: ChatMessage get() = payload as ChatMessage

val MessageOption.focus: MsgFocus get() = payload as MsgFocus

val MessageOption.sender: Contact get() = payload as Contact

enum class Option(option: Int) {
    ADD_MSG(1),
    REMOVE_MSG(2),
    UPDATE_STATE(3),
    UPDATE_CONTENT(4),
    REVOKE_MSG(5),
    UPDATE_FOCUS(6),
    UPDATE_CONTACT(7),
}

