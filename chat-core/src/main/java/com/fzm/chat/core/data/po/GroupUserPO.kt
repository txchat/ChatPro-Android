package com.fzm.chat.core.data.po

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.fzm.chat.core.utils.getSearchKey
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/05/12
 * Description:
 */
@Entity(tableName = "group_user", primaryKeys = ["gid", "address"])
data class GroupUserPO(
    val gid: Long,
    val address: String,
    val role: Int,
    @ColumnInfo(name = "groupUserFlag")
    var flag: Int,
    /**
     * 群昵称
     */
    val nickname: String,
    /**
     * 禁言时间
     */
    val muteTime: Long,
) : Serializable {

    var searchKey: String = ""
        get() {
            if (field.isNotEmpty()) return field
            val name = nickname.ifEmpty { nickname.ifEmpty { address } }
            return getSearchKey(name).also {
                searchKey = it
            }
        }
}