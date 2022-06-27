package com.fzm.chat.wallet.data.bean

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/06
 * Description:钱包中的资产
 */
data class WalletAsset(
    val name: String?,
    @SerializedName(value = "address", alternate = ["addr"])
    val address: String,
    var balance: String,
    val cointype: String? = null,
    @SerializedName("tokensymbol")
    var symbol: String = ""
) : Serializable {

    /**
     * 币种精度，小数点后面精确位数
     */
    var accuracy: Int = 0
}

data class Token(
    val name: String?,
    val symbol: String?
) : Serializable {

    data class Wrapper(
        val tokens: List<Token>
    ) : Serializable
}