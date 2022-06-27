package com.fzm.chat.core.data.contract

import com.fzm.chat.core.utils.CipherUtils
import com.zjy.architecture.ext.bytes2Hex
import com.zjy.architecture.ext.sha256Bytes

/**
 * @author zhengjy
 * @since 2020/02/12
 * Description:
 */
/**
 * 合约接口签名验证方法
 *
 * @param map           需要参加签名的参数
 * @param privateKey    私钥
 */
fun Map<String, Any>.sign(privateKey: String): String {
    val sb = StringBuilder()
    val sorted = keys.toList().sorted()
    for (key in sorted) {
        if (sorted.indexOf(key) != 0) {
            sb.append("&")
        }
        sb.append("${key}=${get(key)}")
    }
    return CipherUtils.sign(sb.toString().toByteArray().sha256Bytes(), privateKey).bytes2Hex()
}
