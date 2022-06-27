package com.fzm.chat.core.data.po

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fzm.chat.core.data.bean.Server
import com.fzm.chat.core.utils.getSearchKey
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/10/15
 * Description:
 */
@Entity(tableName = "friend_user")
data class FriendUserPO(
    /**
     * 用户地址
     */
    @PrimaryKey
    val address: String,
    /**
     * 用户昵称
     */
    var nickname: String,
    /**
     * 用户头像
     */
    var avatar: String,
    /**
     * 用户公钥
     */
    var publicKey: String,
    /**
     * 是否是好友黑名单等标志位
     */
    var flag: Int,
    /**
     * 我在对方的服务器分组（我发送的消息的目的地）
     */
    var servers: MutableList<Server>,
    /**
     * 对方在我的服务器分组id（对方发来的消息的接收地）
     */
    var groups: MutableList<String>,
    /**
     * 对方主链地址
     */
    var chainAddress: MutableMap<String, String>,
    /**
     * 用户昵称
     */
    var remark: String = ""
) : Serializable {

    var searchKey: String = ""
        get() {
            if (field.isNotEmpty()) return field
            val name = remark.ifEmpty { nickname.ifEmpty { address } }
            return getSearchKey(name).also {
                searchKey = it
            }
        }
}