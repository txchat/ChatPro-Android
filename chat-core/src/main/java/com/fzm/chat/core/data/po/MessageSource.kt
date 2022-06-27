package com.fzm.chat.core.data.po

import com.fzm.chat.core.data.model.ChatMessage
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/06/17
 * Description:
 */
data class MessageSource(
    val channelType: Int,
    val from: SourceUser,
    val target: SourceUser
) : Serializable, Cloneable {

    companion object {
        fun create(message: ChatMessage, fromName: String, targetName: String): MessageSource {
            return create(message.channelType, message.from, fromName, message.target, targetName)
        }

        fun create(
            channelType: Int,
            from: String,
            fromName: String,
            target: String,
            targetName: String
        ): MessageSource {
            return MessageSource(
                channelType,
                SourceUser(from, fromName),
                SourceUser(target, targetName)
            )
        }
    }

    public override fun clone(): MessageSource {
        return MessageSource(
            channelType,
            SourceUser(from.id, from.name),
            SourceUser(target.id, target.name)
        )
    }
}

data class SourceUser(
    val id: String,
    val name: String
) : Serializable