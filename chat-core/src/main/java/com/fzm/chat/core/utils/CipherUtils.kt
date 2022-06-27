package com.fzm.chat.core.utils

import com.zjy.architecture.ext.bytes2Hex
import com.zjy.architecture.ext.hex2Bytes
import com.zjy.architecture.ext.tryWith
import walletapi.HDWallet
import walletapi.Walletapi
import javax.crypto.KeyGenerator

/**
 * @author zhengjy
 * @since 2020/12/07
 * Description:
 */
class CipherUtils {

    companion object {
        /**
         * 默认密码：chat33Cipher-{md5("chat33Cipher")}
         */
        const val DEFAULT_PASSWORD = "chat33Cipher-7F67A95639B92CDD52C224DB7202DB07"

        /**
         * 创建助记词
         * 中文:     lang:1 bitSize:160
         * English:  lang:0 bitSize:128
         *
         * 0：英文   1：中文
         * bitSize=128 返回12个单词或者汉子，bitSize+32=160  返回15个单词或者汉子，bitSize=256 返回24个单词或者汉子
         */
        @JvmStatic
        fun createMnemonicString(lang: Long, bitSize: Long): String? {
            try {
                return Walletapi.newMnemonicString(lang, bitSize)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }


        /**
         * 通过主链类型和助记词获取HDWallet对象
         */
        @JvmStatic
        fun getHDWallet(coinType: String, mnem: String): HDWallet? {
            return try {
                Walletapi.newWalletFromMnemonic_v2(coinType, mnem)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        @JvmStatic
        fun pubToAddress(publicKey: String): String {
            return Walletapi.pubToAddress_v2(Walletapi.TypeBtyString, publicKey.hex2Bytes())
        }

        @JvmStatic
        fun pubToAddress(publicKey: ByteArray): String {
            return Walletapi.pubToAddress_v2(Walletapi.TypeBtyString, publicKey)
        }

        @JvmStatic
        fun generateDHSessionKey(privateKey: String?, publicKey: String?): ByteArray? {
            return tryWith { Walletapi.generateDHSessionKey(privateKey, publicKey) }
        }

        /**
         * 加密
         *
         * @param data          待加密数据
         * @param publicKey     对方公钥
         * @param privateKey    己方私钥
         * @return
         * @throws Exception
         */
        @JvmStatic
        @Throws(Exception::class)
        fun encrypt(data: ByteArray, publicKey: String?, privateKey: String?): ByteArray {
            return Walletapi.encryptWithDHKeyPair(privateKey, publicKey, data)
        }

        /**
         * 解密
         *
         * @param data          待解密数据
         * @param publicKey     对方公钥
         * @param privateKey    己方私钥
         * @return
         * @throws Exception
         */
        @JvmStatic
        @Throws(Exception::class)
        fun decrypt(data: ByteArray, publicKey: String?, privateKey: String?): ByteArray {
            return try {
                Walletapi.decryptWithDHKeyPair(privateKey, publicKey, data)
            } catch (e: Exception) {
                data
            }
        }

        /**
         * 对称加密，用于群聊加密
         */
        @JvmStatic
        fun encryptSymmetric(data: String, key: String): String {
            return Walletapi.encryptSymmetric(key, data.toByteArray()).bytes2Hex()
        }

        /**
         * 对称解密，用于群聊解密
         */
        @JvmStatic
        fun decryptSymmetric(data: String, key: String): String {
            return try {
                return String(Walletapi.decryptSymmetric(key, data.hex2Bytes()))
            } catch (e: Exception) {
                data
            }
        }

        @JvmStatic
        fun generateAESKey(length: Int): ByteArray {
            return try {
                val gen = KeyGenerator.getInstance("AES")
                gen.init(length)
                val key = gen.generateKey()
                return key.encoded
            } catch (e: Exception) {
                e.printStackTrace()
                ByteArray(0)
            }
        }

        /**
         * 数字签名接口
         */
        @JvmStatic
        fun sign(data: ByteArray, privateKey: String): ByteArray {
            return if (privateKey.length == 64) {
                try {
                    return Walletapi.chatSign(data, privateKey.hex2Bytes())
                } catch (e: Exception) {
                    e.printStackTrace()
                    ByteArray(0)
                }
            } else {
                ByteArray(0)
            }
        }

        /**
         * 加密密聊密码
         */
        @JvmStatic
        fun encryptPassword(password: String): ByteArray? {
            return try {
                Walletapi.encPasswd(password)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * 密聊密码Hash
         */
        @JvmStatic
        fun passwordHash(password: ByteArray?): String? {
            return try {
                Walletapi.passwdHash(password)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * 校验密聊密码
         */
        @JvmStatic
        fun checkPassword(password: String, passwordHash: String): Boolean {
            var checked = false
            try {
                checked = Walletapi.checkPasswd(password, passwordHash)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return checked
        }

        /**
         * 对助记词进行加密
         */
        @JvmStatic
        fun seedEncKey(encPassword: ByteArray?, seed: String): String? {
            return try {
                val bSeed = Walletapi.stringTobyte(seed)
                val seedEncKey = Walletapi.seedEncKey(encPassword, bSeed)
                Walletapi.bytes2Hex(seedEncKey)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * 对助记词进行解密
         */
        @JvmStatic
        fun seedDecKey(encPassword: ByteArray?, seed: String): String? {
            return try {
                val bSeed = Walletapi.hexTobyte(seed)
                val seedDecKey = Walletapi.seedDecKey(encPassword, bSeed)
                Walletapi.byteTostring(seedDecKey)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * 解密外部传入的助记词
         */
        @JvmStatic
        fun decryptMnemonicString(encMnem: String, password: String): String? {
            val encPassword = encryptPassword(password)
            return seedDecKey(encPassword, encMnem)
        }

        /**
         * 加密外部传入的助记词
         */
        @JvmStatic
        fun encryptMnemonicString(mnem: String, password: String): String? {
            val encPassword = encryptPassword(password)
            return seedEncKey(encPassword, mnem)
        }
    }
}
