package com.fzm.chat.core.data.po

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.fzm.arch.connection.utils.urlKey
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.*
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.utils.PinyinUtils
import com.fzm.chat.core.utils.getSearchKey
import java.io.Serializable
import java.util.*

/**
 * @author zhengjy
 * @since 2021/05/07
 * Description:
 */
@Entity(tableName = "group_info")
data class GroupInfo(
    @PrimaryKey
    val gid: Long,
    /**
     * 群头像
     */
    val avatar: String,
    /**
     * 内部群名
     */
    val name: String,
    /**
     * 公开群名
     */
    val publicName: String,
    /**
     * 群密钥
     */
    var key: String?,
    /**
     * 群类型：
     * 0：普通群
     * 1：全员群
     * 2：部门群
     */
    val groupType: Int,
    /**
     * 群所在服务器地址
     */
    var server: Server,
    /**
     * 群标志位
     */
    var flag: Int,
    /**
     * 群短 id(仅供展示, 后面可能可以供搜索加群使用)
     */
    val markId: String,
    /**
     * 群主
     */
    val owner: GroupUserTO,
    /**
     * 自己在群里的信息
     */
    val person: GroupUserTO?,
    /**
     * 群成员信息
     */
    val members: List<GroupUserTO>?,
    /**
     * 群人数
     */
    val memberNum: Int,
    /**
     * 群内管理员人数
     */
    var adminNum: Int,
    /**
     * 群内禁言人数
     */
    var muteNum: Int,
    /**
     * 群人数上限
     */
    val maximum: Int,
    /**
     * 群状态，0=正常 1=封禁 2=解散
     */
    val status: Int,
    /**
     * 是否允许群内加好友，0=允许加好友，1=禁止加好友
     */
    val friendType: Int,
    /**
     * 加群方式，0=允许任何方式加群，1=群主和管理员邀请加群
     */
    val joinType: Int,
    /**
     * 禁言， 0=所有人可以发言， 1=群主和管理员可以发言
     */
    val muteType: Int,
) : Contact, Sortable, Serializable {

    var serverUrlKey: String = ""
        get() {
            if (field.isNotEmpty()) return field
            return server.address.urlKey().also { field = it }
        }

    var searchKey: String = ""
        get() {
            if (field.isNotEmpty()) return field
            return getSearchKey(name.ifEmpty { publicName }).also {
                searchKey = it
            }
        }

    companion object {

        const val CAN_ADD_FRIEND = 0
        const val FORBID_ADD_FRIEND = 1

        const val CAN_JOIN_GROUP = 0
        const val ONLY_INVITE_GROUP = 1

        const val NOT_MUTE_ALL = 0
        const val MUTE_ALL = 1

        const val TYPE_NORMAL = 0
        const val TYPE_TEAM = 1
        const val TYPE_DEPART = 2

        fun empty(gid: Long, key: String? = null): GroupInfo {
            return GroupInfo(gid, "", "", "", key, 0, Server("", "", ""), 0, "",
                GroupUserTO("", 0, "", 0L), null, null, 0, 0,
                0, 0, 0, 0, 0, 0)
        }
    }

    override fun getId(): String {
        return gid.toString()
    }

    override fun getDisplayName(): String {
        return name.ifEmpty { publicName }
    }

    override fun getScopeName(): String {
        return name.ifEmpty { publicName }
    }

    override fun getRawName(): String {
        return publicName
    }

    override fun getDisplayImage(): String {
        return avatar
    }

    override fun getType(): Int {
        return ChatConst.GROUP_CHANNEL
    }

    override fun getServerList(): List<Server> {
        return listOf(server)
    }

    override fun getExtra(): Bundle {
        return bundleOf(
            Contact.GROUP_TYPE to groupType,
            Contact.FLAG to flag
        )
    }

    @Ignore
    private var letter: String = ""

    override fun getFirstChar(): String {
        return if (name.isNotEmpty()) {
            name.substring(0, 1)
        } else {
            "#"
        }
    }

    override fun getFirstLetter(): String {
        val letter = getLetters().substring(0, 1)
        return if (letter.matches("[A-Z]".toRegex())) {
            letter
        } else {
            "#"
        }
    }

    override fun getLetters(): String {
        if (letter.isNotEmpty()) {
            return letter
        }
        val pinyin = if (name.isNotEmpty()) PinyinUtils.getPingYin(name) else "#"
        val sortString = pinyin.substring(0, 1).toUpperCase(Locale.US)
        return if (sortString.matches("[A-Z]".toRegex())) {
            letter = pinyin.toUpperCase(Locale.US)
            letter
        } else {
            letter = getDisplayName()
            letter
        }
    }

    override fun priority(): Int {
        return 0
    }
}

/**
 * 用户是否在群中
 */
val GroupInfo.isInGroup: Boolean
    get() = flag and Contact.RELATION == Contact.RELATION

/**
 * 该群是否全员禁言
 */
val GroupInfo.isMuteAll: Boolean
    get() = muteType == GroupInfo.MUTE_ALL

/**
 * 该群是否可以互相添加好友
 */
val GroupInfo.canAddFriend: Boolean
    get() = friendType == GroupInfo.CAN_ADD_FRIEND

/**
 * 成员在群中的角色，默认为普通成员
 */
val GroupInfo?.groupRole: Int
    get() = this?.person?.role ?: GroupUser.LEVEL_USER

/**
 * 全员群、部门群或者普通群
 */
val GroupInfo.typeDesc: String
    get() = if (groupType == GroupInfo.TYPE_TEAM) "全员" else if (groupType == GroupInfo.TYPE_DEPART) "部门" else ""