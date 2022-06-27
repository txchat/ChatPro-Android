package com.fzm.chat.core.data.po

import android.content.Context
import android.util.Base64
import com.fzm.chat.core.data.model.ForwardMsg
import com.fzm.chat.core.data.model.getSnapShot
import com.zjy.architecture.util.FileUtils
import dtalk.biz.msg.Msg
import walletapi.Walletapi
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/28
 * Description:
 */
open class MessageContent(
    var content: String? = null,
    var atList: List<String>? = null,

    /*===================图片、语音、视频消息===================*/
    /**
     * 媒体文件相关
     */
    var mediaUrl: String? = null,
    var localUrl: String? = null,
    var height: Int = 0,
    var width: Int = 0,
    var duration: Int = 0,
    /*=======================================================*/

    /*========================文件消息========================*/
    /**
     * 文件相关
     */
    var fileName: String? = null,
    var size: Long = 0L,
    var md5: String? = null,
    /*=======================================================*/

    /**
     * 合并转发相关
     */
    var forwardLogs: List<ForwardMsg> = emptyList(),

    /**
     * 通知子类型
     */
    var notificationType: Int = -1,
    /**
     * 重新编辑
     */
    var reedit: String? = null,

    /*========================音视频消息========================*/
    /**
     * 音视频通话类型
     */
    var rtcType: Int = 0,
    /**
     * 音视频通话状态
     * 1：正常结束
     * 2：拒绝
     * 3：取消
     * 4：忙线
     * 5：未响应
     * 6：通话失败
     */
    var rtcStatus: Int = 0,
    /*=======================================================*/

    /*========================转账消息========================*/
    /**
     * 主链
     */
    var chain: String? = null,
    /**
     * 平台标识，用来区分平行连
     */
    var platform: String? = null,
    /**
     * 转账交易hash
     */
    var txHash: String? = null,
    /**
     * 币种名称
     */
    var txSymbol: String? = null,

    /**
     * 交易金额
     */
    var txAmount: String? = null,
    /**
     * 交易无效（转账和收款地址不是聊天双方）
     */
    var txInvalid: Boolean = false,
    /**
     * 交易状态
     */
    var txStatus: String? = null,
    /*=======================================================*/

    /*========================红包消息========================*/
    /**
     * 红包id
     */
    var packetId: String? = null,
    /**
     * 币种所属链
     */
    var exec: String? = null,
    /**
     * 币种类型：coins，token（只对BTY系列币种有意义）
     */
    var coinType: Int = 0,
    /**
     * 币种名
     */
    var symbol: String? = null,
    /**
     * 红包备注
     */
    var remark: String? = null,
    /**
     * 0：随机金额
     * 1：固定金额
     */
    var packetType: Int = 0,
    /**
     * 红包私钥
     */
    var privateKey: String? = null,
    /**
     * 红包过期时间
     */
    var expire: Long = 0,
    /**
     * 红包状态
     * 1:待领 2:已领完 3:已退回 4:过期以过期时间为准 5:红包已领取
     *
     * 如果是服务端返回[3：已退回]，则红包已经过期
     */
    var state: Int = 0,
    /*=======================================================*/

    /*========================红包消息========================*/
    /**
     * 名片类型
     * 1：个人名片
     * 2：群名片
     * 3：团队名片
     */
    var contactType: Int = 0,
    /**
     * 名片id
     */
    var contactId: String? = null,
    /**
     * 名片名字
     */
    var contactName: String? = null,
    /**
     * 名片头像
     */
    var contactAvatar: String? = null,
    /**
     * 联系人服务器
     */
    var contactServer: String? = null,
    /**
     * 邀请人
     */
    var contactInviter: String? = null,
    /*=======================================================*/

    var isRead: Boolean = false,
    /**
     * 未知消息类型，保存源字节数组
     */
    var proto: String? = null,
) : Serializable, Cloneable {

    /**
     * 获取媒体本地或网络地址
     */
    @Deprecated("使用getDisplayUrl(Context)方法")
    fun getUrl(): String = localUrl?.ifEmpty { mediaUrl } ?: mediaUrl ?: ""

    companion object {

        private val EMPTY = MessageContent()

        fun empty() = EMPTY

        fun text(content: String?, atList: List<String>? = null) = MessageContent().apply {
            this.content = content
            this.atList = atList
        }

        fun audio(mediaUrl: String? = null, localUrl: String? = null, duration: Int) = MessageContent().apply {
            this.mediaUrl = mediaUrl
            this.localUrl = localUrl
            this.duration = duration
        }

        fun image(mediaUrl: String? = null, localUrl: String? = null, size: IntArray) = MessageContent().apply {
            this.mediaUrl = mediaUrl
            this.localUrl = localUrl
            this.height = size[0]
            this.width = size[1]
        }

        fun video(mediaUrl: String? = null, localUrl: String? = null, size: IntArray, duration: Int) = MessageContent().apply {
            this.mediaUrl = mediaUrl
            this.localUrl = localUrl
            this.height = size[0]
            this.width = size[1]
            this.duration = duration
        }

        fun file(mediaUrl: String? = null, localUrl: String? = null, name: String, size: Long, md5: String) = MessageContent().apply {
            this.mediaUrl = mediaUrl
            this.localUrl = localUrl
            this.fileName = name
            this.size = size
            this.md5 = md5
        }

        fun notification(content: String, notificationType: Int) = MessageContent().apply {
            this.content = content
            this.notificationType = notificationType
        }

        fun notification(content: String, reedit: String?, notificationType: Int) = MessageContent().apply {
            this.content = content
            this.reedit = reedit
            this.notificationType = notificationType
        }

        fun forward(context: Context, messages: List<ForwardMsg>) = MessageContent().apply {
            this.forwardLogs = messages
            this.content = this.forwardLogs.getSnapShot(context)
        }

        fun rtcCall(rtcType: Int, status: Int, duration: Int = 0) = MessageContent().apply {
            this.rtcType = rtcType
            this.rtcStatus = status
            this.duration = duration
        }

        fun transfer(chain: String, platform: String?, txHash: String, coinName: String, coinType: Int) = MessageContent().apply {
            this.chain = chain
            this.platform = platform
            this.txHash = txHash
            this.txSymbol = coinName
            this.txStatus = "0"
            this.coinType = coinType
        }

        fun redPacket(
            packetId: String,
            exec: String,
            coinType: Int,
            symbol: String,
            remark: String,
            packetType: Int,
            privateKey: String,
            expire: Long
        ) = MessageContent().apply {
            this.packetId = packetId
            this.exec = exec
            this.coinType = coinType
            this.symbol = symbol
            this.remark = remark
            this.packetType = packetType
            this.privateKey = privateKey
            this.expire = expire
            this.state = 1
        }

        fun contactCard(contactType: Int, id: String, name: String, avatar: String, server: String? = null, inviterId: String? = null) = MessageContent().apply {
            this.contactType = contactType
            this.contactId = id
            this.contactName = name
            this.contactAvatar = avatar
            this.contactServer = server
            this.contactInviter = inviterId
        }

        fun unknown(bytes: ByteArray): MessageContent {
            return MessageContent().apply {
                this.proto = Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        }
    }

    public override fun clone(): MessageContent {
        return MessageContent(
            content, atList, mediaUrl, localUrl, height, width, duration, fileName, size, md5, forwardLogs, notificationType, reedit, rtcType, rtcStatus, chain, platform, txHash, txSymbol, txAmount, txInvalid, txStatus, packetId, exec, coinType, symbol, remark, packetType, privateKey, expire, state, contactType, contactId, contactName, contactAvatar, contactServer, contactInviter, isRead, proto
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageContent) return false

        if (content != other.content) return false
        if (atList != other.atList) return false
        if (mediaUrl != other.mediaUrl) return false
        if (localUrl != other.localUrl) return false
        if (height != other.height) return false
        if (width != other.width) return false
        if (duration != other.duration) return false
        if (fileName != other.fileName) return false
        if (size != other.size) return false
        if (md5 != other.md5) return false
        if (forwardLogs != other.forwardLogs) return false
        if (notificationType != other.notificationType) return false
        if (reedit != other.reedit) return false
        if (rtcType != other.rtcType) return false
        if (rtcStatus != other.rtcStatus) return false
        if (chain != other.chain) return false
        if (platform != other.platform) return false
        if (txHash != other.txHash) return false
        if (txSymbol != other.txSymbol) return false
        if (txAmount != other.txAmount) return false
        if (txInvalid != other.txInvalid) return false
        if (txStatus != other.txStatus) return false
        if (packetId != other.packetId) return false
        if (exec != other.exec) return false
        if (coinType != other.coinType) return false
        if (symbol != other.symbol) return false
        if (remark != other.remark) return false
        if (packetType != other.packetType) return false
        if (privateKey != other.privateKey) return false
        if (expire != other.expire) return false
        if (state != other.state) return false
        if (contactType != other.contactType) return false
        if (contactId != other.contactId) return false
        if (contactName != other.contactName) return false
        if (contactAvatar != other.contactAvatar) return false
        if (contactServer != other.contactServer) return false
        if (isRead != other.isRead) return false
        if (proto != other.proto) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content?.hashCode() ?: 0
        result = 31 * result + (atList?.hashCode() ?: 0)
        result = 31 * result + (mediaUrl?.hashCode() ?: 0)
        result = 31 * result + (localUrl?.hashCode() ?: 0)
        result = 31 * result + height
        result = 31 * result + width
        result = 31 * result + duration
        result = 31 * result + (fileName?.hashCode() ?: 0)
        result = 31 * result + size.hashCode()
        result = 31 * result + (md5?.hashCode() ?: 0)
        result = 31 * result + forwardLogs.hashCode()
        result = 31 * result + notificationType
        result = 31 * result + (reedit?.hashCode() ?: 0)
        result = 31 * result + rtcType
        result = 31 * result + rtcStatus
        result = 31 * result + (chain?.hashCode() ?: 0)
        result = 31 * result + (platform?.hashCode() ?: 0)
        result = 31 * result + (txHash?.hashCode() ?: 0)
        result = 31 * result + (txSymbol?.hashCode() ?: 0)
        result = 31 * result + (txAmount?.hashCode() ?: 0)
        result = 31 * result + txInvalid.hashCode()
        result = 31 * result + (txStatus?.hashCode() ?: 0)
        result = 31 * result + (packetId?.hashCode() ?: 0)
        result = 31 * result + (exec?.hashCode() ?: 0)
        result = 31 * result + coinType.hashCode()
        result = 31 * result + (symbol?.hashCode() ?: 0)
        result = 31 * result + (remark?.hashCode() ?: 0)
        result = 31 * result + packetType
        result = 31 * result + (privateKey?.hashCode() ?: 0)
        result = 31 * result + expire.hashCode()
        result = 31 * result + state
        result = 31 * result + contactType
        result = 31 * result + (contactId?.hashCode() ?: 0)
        result = 31 * result + (contactName?.hashCode() ?: 0)
        result = 31 * result + (contactAvatar?.hashCode() ?: 0)
        result = 31 * result + (contactServer?.hashCode() ?: 0)
        result = 31 * result + isRead.hashCode()
        result = 31 * result + (proto?.hashCode() ?: 0)
        return result
    }
}

/**
 * 获取媒体本地或网络地址
 */
fun MessageContent.getDisplayUrl(context: Context): String {
    return if (FileUtils.fileExists(context, localUrl)) localUrl ?: "" else mediaUrl ?: ""
}

/**
 * 用于判断红包币种是否是coins
 */
private inline val MessageContent.isCoins: Boolean get() = coinType == Msg.CoinType.Coins_VALUE

/**
 * 红包币种的币种合约
 */
val MessageContent.fullExec: String get() {
    return if (exec?.startsWith("user.p.") == true) {
        if (isCoins) "${exec}.coins" else "${exec}.token"
    } else {
        if (isCoins) "coins" else "token"
    }
}

/**
 * 用于查询交易信息的symbol
 */
val MessageContent.tokenSymbol: String get() {
    return if (chain == Walletapi.TypeBtyString && platform != "bty") {
        if (coinType == Msg.CoinType.Coins_VALUE) {
            "${platform}.coins"
        } else {
            "${platform}.${txSymbol}"
        }
    } else {
        "$txSymbol"
    }
}