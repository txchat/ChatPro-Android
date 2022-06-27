package com.fzm.chat.core.data.model

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.room.ColumnInfo
import androidx.room.Ignore
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.bean.Server
import com.fzm.chat.core.data.bean.Sortable
import com.fzm.chat.core.utils.PinyinUtils
import java.io.Serializable
import java.util.*

/**
 * @author zhengjy
 * @since 2021/05/12
 * Description:
 */
data class GroupUser(
    val gid: String,
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
    var muteTime: Long,
    /**
     * 用户昵称
     */
    val name: String?,
    /**
     * 用户头像
     */
    val avatar: String?,
    /**
     * 好友的备注，可能为空
     */
    val remark: String?,
    /**
     * 团队姓名，可能为空
     */
    val teamName: String?,
) : Contact, Sortable, Serializable {

    companion object {
        const val LEVEL_USER = 0
        const val LEVEL_ADMIN = 1
        const val LEVEL_OWNER = 2

        fun empty(gid: String, address: String): GroupUser =
            GroupUser(gid, address, 0, 0, "", 0, null, null, null, null)
    }

    override fun getId(): String {
        return address
    }

    override fun getDisplayName(): String {
        return (remark ?: "").ifEmpty {
            nickname.ifEmpty {
                (teamName ?: "").ifEmpty {
                    (name ?: "").ifEmpty {
                        address.toDisplay()
                    }
                }
            }
        }
    }

    override fun getScopeName(): String {
        return nickname.ifEmpty {
            (teamName ?: "").ifEmpty {
                (name ?: "").ifEmpty {
                    address.toDisplay()
                }
            }
        }
    }

    override fun getRawName(): String {
        return nickname.ifEmpty {
            (name ?: "").ifEmpty {
                address.toDisplay()
            }
        }
    }

    override fun getDisplayImage(): String {
        return avatar ?: ""
    }

    override fun getType(): Int {
        return -1
    }

    override fun getServerList(): List<Server> {
        return emptyList()
    }

    override fun getExtra(): Bundle? {
        return bundleOf(
            Contact.GROUP_ROLE to role,
            Contact.GROUP_MUTE_TIME to muteTime
        )
    }

    @Ignore
    private var letter: String = ""

    override fun getFirstChar(): String {
        return when {
            !remark.isNullOrEmpty() -> remark.substring(0, 1)
            nickname.isNotEmpty() -> nickname.substring(0, 1)
            !teamName.isNullOrEmpty() -> teamName.substring(0, 1)
            !name.isNullOrEmpty() -> name.substring(0, 1)
            address.isNotEmpty() -> address.substring(0, 1)
            else -> "#"
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
        val pinyin = when {
            !remark.isNullOrEmpty() -> PinyinUtils.getPingYin(remark)
            nickname.isNotEmpty() -> PinyinUtils.getPingYin(nickname)
            !teamName.isNullOrEmpty() -> PinyinUtils.getPingYin(teamName)
            !name.isNullOrEmpty() -> PinyinUtils.getPingYin(name)
            address.isNotEmpty() -> PinyinUtils.getPingYin(address)
            else -> "#"
        }
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
        return role
    }
}

fun GroupUser.addFlag(flag: Int) {
    this.flag = this.flag or flag
}

fun GroupUser.deleteFlag(flag: Int) {
    this.flag = this.flag and flag.inv()
}