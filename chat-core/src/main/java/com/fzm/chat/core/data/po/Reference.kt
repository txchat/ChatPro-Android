package com.fzm.chat.core.data.po

import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.fzm.chat.core.data.model.ChatMessage
import com.google.gson.annotations.Expose
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/12/14
 * Description:
 */
class Reference(
    /**
     * 主题消息id
     */
    val topic: Long,
    /**
     * 引用消息id
     */
    @ColumnInfo(name = "refId")
    val ref: Long,
) : Serializable, Cloneable {

    @Ignore
    @Expose(serialize = false, deserialize = false)
    var refMsg: ChatMessage? = null

    public override fun clone(): Reference {
        val ref = refMsg?.clone()
        return (super.clone() as Reference).also { it.refMsg = ref }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Reference

        if (topic != other.topic) return false
        if (ref != other.ref) return false
        if (refMsg != other.refMsg) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topic.hashCode()
        result = 31 * result + ref.hashCode()
        result = 31 * result + (refMsg?.hashCode() ?: 0)
        return result
    }
}