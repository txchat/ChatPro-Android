package com.fzm.chat.core.session

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fzm.chat.core.data.bean.Server
import com.fzm.chat.core.utils.getSearchKey
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/08/06
 * Description:
 */
@Entity(tableName = "user_info")
class UserInfo(
    /**
     * 用户地址，唯一标识
     */
    @PrimaryKey
    val address: String,
    /**
     * 用户昵称
     */
    var nickname: String,
    /**
     * 用户头衔
     */
    var avatar: String,
    /**
     * 用户公钥
     */
    var publicKey: String,
    /**
     * 绑定手机号
     */
    var phone: String?,
    /**
     * 绑定邮箱
     */
    var email: String?,
    /**
     * 设置的服务器
     */
    val servers: MutableList<Server>,
    /**
     * 主链地址
     */
    val chainAddress: MutableMap<String, String>,
) : Serializable {

    var searchKey: String = ""
        get() {
            if (field.isNotEmpty()) return field
            val name = nickname.ifEmpty { address }
            return getSearchKey(name).also {
                searchKey = it
            }
        }

    companion object {

        const val EMPTY_ADDRESS = "empty"

        val EMPTY_USER = UserInfo(EMPTY_ADDRESS, "", "", "", "", "", mutableListOf(), mutableMapOf())
    }

    fun isLogin(): Boolean {
        return address.isNotEmpty() && address != EMPTY_ADDRESS
    }

    fun newBuilder(): Builder {
        return Builder(address)
            .setNickname(nickname)
            .setAvatar(avatar)
            .setPublicKey(publicKey)
            .setPhone(phone)
            .setEmail(email)
            .setServers(servers)
            .setChainAddress(chainAddress)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserInfo

        if (address != other.address) return false
        if (nickname != other.nickname) return false
        if (avatar != other.avatar) return false
        if (publicKey != other.publicKey) return false
        if (phone != other.phone) return false
        if (email != other.email) return false
        if (servers != other.servers) return false
        if (chainAddress != other.chainAddress) return false
        if (searchKey != other.searchKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + nickname.hashCode()
        result = 31 * result + avatar.hashCode()
        result = 31 * result + publicKey.hashCode()
        result = 31 * result + (phone?.hashCode() ?: 0)
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + servers.hashCode()
        result = 31 * result + chainAddress.hashCode()
        result = 31 * result + searchKey.hashCode()
        return result
    }

    class Builder(private val address: String) : Serializable {

        private var nickname: String = ""
        private var avatar: String = ""
        private var publicKey: String = ""
        private var phone: String? = null
        private var email: String? = null
        private var servers: MutableList<Server> = mutableListOf()
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

        fun setPhone(phone: String?): Builder {
            this.phone = phone
            return this
        }

        fun setEmail(email: String?): Builder {
            this.email = email
            return this
        }

        fun setServers(servers: MutableList<Server>): Builder {
            this.servers = servers
            return this
        }

        fun setChainAddress(address: MutableMap<String, String>): Builder {
            this.chainAddress = address
            return this
        }

        fun build(): UserInfo {
            return UserInfo(address, nickname, avatar, publicKey, phone, email, servers, chainAddress)
        }
    }
}
