package com.fzm.chat.redpacket.data.bean

import java.io.Serializable

data class AccountAsset(
    /**
     * 帐号的地址
     */
    val addr: String,
    /**
     * 帐号总额
     */
    val currency: Long,
    /**
     * 帐号的可用余额
     */
    val balance: String,
    /**
     * 帐号中冻结余额
     */
    val frozen: String,
) : Serializable {

    /**
     * 币种精度，小数点后面精确位数
     */
    var accuracy: Int = 0

    var name: String? = null

    data class Wrapper(
        /**
         * token标记符
         */
        val symbol: String,
        val account: AccountAsset
    ) : Serializable

    data class TokenAsset(
        val tokenAssets: List<Wrapper>
    ) : Serializable
}

data class Token(
    val name: String?,
    val symbol: String?
) : Serializable {

    data class Wrapper(
        val tokens: List<Token>
    ) : Serializable
}
