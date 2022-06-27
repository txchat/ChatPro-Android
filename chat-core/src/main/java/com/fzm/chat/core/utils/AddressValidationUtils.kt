package com.fzm.chat.core.utils

import java.lang.IllegalStateException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * @author zhengjy
 * @since 2021/09/15
 * Description:
 */
object AddressValidationUtils {

    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    /**
     * btc(bch,usdt)地址是否有效
     *
     * return: true有效,false无效
     */
    fun bitCoinAddressValidate(address: String): Boolean {
        if (address.length < 26 || address.length > 35) return false
        val decoded = decodeBase58To25Bytes(address) ?: return false
        val hash1 = sha256(decoded.copyOfRange(0, 21))
        val hash2 = sha256(hash1)
        return hash2.copyOfRange(0, 4).contentEquals(decoded.copyOfRange(21, 25))
    }

    private fun decodeBase58To25Bytes(input: String): ByteArray? {
        var num: BigInteger = BigInteger.ZERO
        for (t in input.toCharArray()) {
            val p = ALPHABET.indexOf(t)
            if (p == -1) return null
            num = num.multiply(BigInteger.valueOf(58)).add(BigInteger.valueOf(p.toLong()))
        }
        val result = ByteArray(25)
        val numBytes: ByteArray = num.toByteArray()
        System.arraycopy(numBytes, 0, result, result.size - numBytes.size, numBytes.size)
        return result
    }

    private fun sha256(data: ByteArray): ByteArray {
        return try {
            val md: MessageDigest = MessageDigest.getInstance("SHA-256")
            md.update(data)
            md.digest()
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException(e)
        }
    }
}

fun String?.isBTYAddress(): Boolean {
    if (this == null) return false
    return AddressValidationUtils.bitCoinAddressValidate(this)
}