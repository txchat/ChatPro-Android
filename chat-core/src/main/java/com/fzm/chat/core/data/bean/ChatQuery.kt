package com.fzm.chat.core.data.bean

import com.fzm.chat.core.data.ChatConfig
import com.fzm.chat.core.data.contract.ContractQuery
import com.fzm.chat.core.data.contract.sign
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/02/05
 * Description:合约查询好友参数
 */
class ChatQuery private constructor(
    funcName: String,
    mainAddress: String?,
    targetAddress: String?,
    count: Int,
    index: String,
    keyPair: KeyPair
) : ContractQuery() {

    companion object {

        fun friendsQuery(mainAddress: String?, index: String?, keyPair: KeyPair): ChatQuery {
            return ChatQuery(
                "GetFriends",
                mainAddress ?: "",
                null,
                ChatConfig.PAGE_SIZE * 2,
                index ?: "",
                keyPair
            )
        }

        fun blockQuery(mainAddress: String?, index: String?, keyPair: KeyPair): ChatQuery {
            return ChatQuery(
                "GetBlackList",
                mainAddress ?: "",
                null,
                ChatConfig.PAGE_SIZE * 2,
                index ?: "",
                keyPair
            )
        }

        fun userQuery(
            mainAddress: String?,
            targetAddress: String?,
            index: String?,
            keyPair: KeyPair
        ): ChatQuery {
            return ChatQuery(
                "GetUser",
                mainAddress ?: "",
                targetAddress ?: "",
                ChatConfig.PAGE_SIZE * 2,
                index ?: "",
                keyPair
            )
        }

        fun serverGroupQuery(
            mainAddress: String?,
            index: String?,
            keyPair: KeyPair
        ): ChatQuery {
            return ChatQuery(
                "GetServerGroup",
                mainAddress ?: "",
                null,
                ChatConfig.PAGE_SIZE * 2,
                index ?: "",
                keyPair
            )
        }
    }

    init {
        execer = "chat"
        this.funcName = funcName
        val time = System.currentTimeMillis()
        val signature = mutableMapOf<String, Any>().apply {
            if (mainAddress != null) {
                put("mainAddress", mainAddress)
            }
            if (targetAddress != null) {
                put("targetAddress", targetAddress)
            }
            put("count", count)
            put("index", index)
            put("time", time)
        }.sign(keyPair.privateKey)
        payload = Params(
            mainAddress,
            targetAddress,
            count,
            index,
            time,
            Signature.create(keyPair.publicKey, signature)
        )
    }

    data class Params(
        var mainAddress: String?,
        var targetAddress: String?,
        var count: Int,
        var index: String,
        var time: Long,
        var sign: Signature
    ) : Serializable

    data class Signature(
        var publicKey: String,
        var signature: String
    ) : Serializable {

        companion object {
            fun create(publicKey: String, signature: String): Signature {
                return Signature(publicKey, signature)
            }
        }
    }
}