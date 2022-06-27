package com.fzm.chat.core.data.po

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.room.Ignore
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.bean.Server
import com.fzm.chat.core.data.bean.Sortable
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.PinyinUtils
import java.io.Serializable
import java.util.*

/**
 * @author zhengjy
 * @since 2020/12/22
 * Description:
 */
data class FriendUser(
    /**
     * 用户地址
     */
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
    var remark: String = "",
    /**
     * 团队姓名
     */
    val teamName: String? = null
) : Serializable, Contact, Sortable {

    @Ignore
    var preSelected = false

    override fun getId(): String {
        return address
    }

    override fun getDisplayName(): String {
        return remark.ifEmpty {
            (teamName ?: "").ifEmpty {
                nickname.ifEmpty { address.toDisplay() }
            }
        }
    }

    override fun getScopeName(): String {
        return (teamName ?: "").ifEmpty {
            nickname.ifEmpty { address.toDisplay() }
        }
    }

    override fun getRawName(): String {
        return nickname.ifEmpty { address.toDisplay() }
    }

    override fun getDisplayImage(): String {
        return avatar
    }

    override fun getType(): Int {
        return ChatConst.PRIVATE_CHANNEL
    }

    override fun getServerList(): List<Server> {
        return servers
    }

    override fun getExtra(): Bundle? {
        return bundleOf(
            Contact.PUB_KEY to publicKey,
            Contact.FLAG to flag,
            Contact.CHAIN_ADDRESS to chainAddress
        )
    }

    companion object {
        fun empty(address: String?): FriendUser {
            return FriendUser(address ?: "", "", "", "", 0, mutableListOf(), mutableListOf(), mutableMapOf())
        }
    }

    class Builder(private val address: String) : Serializable {
        private var nickname: String = ""
        private var avatar: String = ""
        private var publicKey: String = ""
        private var flag: Int = 0
        private var servers: MutableList<Server> = mutableListOf()
        private var groups: MutableList<String> = mutableListOf()
        private var chainAddress: MutableMap<String, String> = mutableMapOf()

        fun setNickname(nickname: String): Builder {
            this.nickname = nickname
            return this
        }

        fun setAvatar(avatar: String): Builder {
            this.avatar = avatar
            return this
        }

        fun setPublicKey(publicKey: String): Builder {
            this.publicKey = publicKey
            return this
        }

        fun setFlag(flag: Int): Builder {
            this.flag = flag
            return this
        }

        fun setServers(servers: MutableList<Server>): Builder {
            this.servers = servers
            return this
        }

        fun setGroups(groups: MutableList<String>): Builder {
            this.groups = groups
            return this
        }

        fun setChainAddress(address: MutableMap<String, String>): Builder {
            this.chainAddress = address
            return this
        }

        fun build(): FriendUser {
            return FriendUser(address, nickname, avatar, publicKey, flag, servers, groups, chainAddress)
        }
    }

    @Ignore
    private var letter: String = ""

    override fun getFirstChar(): String {
        return when {
            remark.isNotEmpty() -> remark.substring(0, 1)
            !teamName.isNullOrEmpty() -> teamName.substring(0, 1)
            nickname.isNotEmpty() -> nickname.substring(0, 1)
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
            remark.isNotEmpty() -> PinyinUtils.getPingYin(remark)
            !teamName.isNullOrEmpty() -> PinyinUtils.getPingYin(teamName)
            nickname.isNotEmpty() -> PinyinUtils.getPingYin(nickname)
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
        return 0
    }
}

fun LoginDelegate.toFriendUser(): FriendUser {
    val user = this.current.value ?: return FriendUser.empty(getAddress())
    val companyUser = this.companyUser.value
    return FriendUser(
        user.address,
        user.nickname,
        user.avatar,
        user.publicKey,
        0,
        user.servers,
        mutableListOf(),
        user.chainAddress,
        teamName = companyUser?.name
    )
}

fun FriendUser.toPO() = FriendUserPO(address, nickname, avatar, publicKey, flag, servers, groups, chainAddress, remark)

val FriendUser.isFriend: Boolean
    get() = flag and Contact.RELATION == Contact.RELATION

val FriendUser.isBlock: Boolean
    get() = flag and Contact.BLOCK == Contact.BLOCK

fun FriendUser.addFriend() = addFlag(Contact.RELATION)

fun FriendUser.deleteFriend() = deleteFlag(Contact.RELATION)

fun FriendUser.blockUser() = addFlag(Contact.BLOCK)

fun FriendUser.unBlockUser() = deleteFlag(Contact.BLOCK)

fun FriendUser.addFlag(flag: Int) {
    this.flag = this.flag or flag
}

fun FriendUser.deleteFlag(flag: Int) {
    this.flag = this.flag and flag.inv()
}