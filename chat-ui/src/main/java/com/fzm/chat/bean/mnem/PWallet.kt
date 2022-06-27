package com.fzm.chat.bean.mnem

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/09
 * Description:
 */
class PWallet : Serializable {

    var password: String? = null

    var mnem: String? = null

    //0：英文  1：中文
    var mnemType = 0

    companion object {
        const val TYPE_CHINESE = 1
        const val TYPE_ENGLISH = 0
    }
}