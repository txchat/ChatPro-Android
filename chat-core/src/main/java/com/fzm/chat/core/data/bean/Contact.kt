package com.fzm.chat.core.data.bean

import android.os.Bundle
import com.fzm.chat.core.data.model.RecentContactMsg
import com.fzm.chat.core.data.po.GroupInfo
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/22
 * Description:
 */
interface Contact : Sortable, Serializable {

    companion object {
        /**
         * 在好友中或在群聊之中
         */
        const val RELATION = 1

        /**
         * 在黑名单中
         */
        const val BLOCK = 1 shl 1

        /**
         * 置顶
         */
        const val STICK_TOP = 1 shl 2

        /**
         * 免打扰
         */
        const val NO_DISTURB = 1 shl 3

        /*-------------------------额外信息字段名-------------------------*/
        /**
         * 群成员等级
         */
        const val GROUP_ROLE = "GROUP_ROLE"
        /**
         * 群成员禁言时间
         */
        const val GROUP_MUTE_TIME = "GROUP_MUTE_TIME"
        /**
         * 群类型
         */
        const val GROUP_TYPE = "GROUP_TYPE"

        /**
         * 好友公钥
         */
        const val PUB_KEY = "PUB_KEY"

        /**
         * 用户flag
         */
        const val FLAG = "FLAG"

        /**
         * 好友主链地址
         */
        const val CHAIN_ADDRESS = "CHAIN_ADDRESS"
    }

    /**
     * 获取id
     */
    fun getId(): String

    /**
     * 获取显示名，如会话和消息列表中，优先显示备注
     */
    fun getDisplayName(): String

    /**
     * 获取限定范围内的公开昵称，如团队姓名
     */
    fun getScopeName(): String

    /**
     * 获取公开昵称，如转发时只显示用户昵称或地址
     */
    fun getRawName(): String

    /**
     * 获取显示头像
     */
    fun getDisplayImage(): String

    /**
     * 获取联系人类型
     */
    fun getType(): Int

    /**
     * 获取联系人所在服务器
     */
    fun getServerList(): List<Server>

    /**
     * 获取额外信息，如：群角色
     */
    fun getExtra(): Bundle?

    fun String.toDisplay(): String {
        if (length <= 8) {
            return this
        }
        val start: String = substring(0, 4)
        val end: String = substring(length - 4)
        return "$start****$end"
    }

    override fun getFirstChar(): String {
        return ""
    }

    override fun getFirstLetter(): String {
        return ""
    }

    override fun getLetters(): String {
        return ""
    }

    override fun priority(): Int {
        return 0
    }
}

fun Contact.getPubKey() = getExtra()?.getString(Contact.PUB_KEY) ?: ""

fun Contact.getGroupRole(default: Int) = getExtra()?.getInt(Contact.GROUP_ROLE) ?: default

fun Contact.getMuteTime() = getExtra()?.getLong(Contact.GROUP_MUTE_TIME) ?: 0L

fun Contact.getGroupType(): Int = getExtra()?.getInt(Contact.GROUP_TYPE) ?: GroupInfo.TYPE_NORMAL

val Contact.isFriend: Boolean
    get() = (getExtra()?.getInt(Contact.FLAG) ?: 0) and Contact.RELATION != 0

val Contact.isBlock: Boolean
    get() = (getExtra()?.getInt(Contact.FLAG) ?: 0) and Contact.BLOCK != 0

val Contact.noDisturb: Boolean
    get() = (getExtra()?.getInt(Contact.FLAG) ?: 0) and Contact.NO_DISTURB != 0

val Contact.stickTop: Boolean
    get() = (getExtra()?.getInt(Contact.FLAG) ?: 0) and Contact.STICK_TOP != 0

val Contact.chains: Map<String, String>
    get() = (getExtra()?.getSerializable(Contact.CHAIN_ADDRESS)
        ?: emptyMap<String, String>()) as Map<String, String>