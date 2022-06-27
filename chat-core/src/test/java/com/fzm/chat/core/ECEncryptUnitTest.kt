package com.fzm.chat.core

import com.fzm.chat.core.crypto.ECCUtils
import com.fzm.chat.core.crypto.ECCUtils.toBytes
import com.fzm.chat.core.crypto.ECCUtils.toECPrivateKey
import com.zjy.architecture.ext.hex2Bytes
import org.junit.Test

import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ECEncryptUnitTest {

    private val content = "哈哈哈，我是需要加密的内容".toByteArray()

    /**
     * bitcoinj和普通椭圆曲线混合
     */
    @Test
    fun encryptCompat() {
        val pair = ECCUtils.genECKeyPair()

        val btcSign = ECCUtils.btcCoinSign(content, (pair.private as BCECPrivateKey).toBytes())
        assert(ECCUtils.verify(content, btcSign, pair.public))
        assert(ECCUtils.btcCoinVerify(content, btcSign, (pair.public as BCECPublicKey).toBytes()))

        val normalSign = ECCUtils.sign(content, pair.private)
        assert(ECCUtils.verify(content, normalSign, pair.public))
        assert(ECCUtils.btcCoinVerify(content, normalSign, (pair.public as BCECPublicKey).toBytes()))
    }

    /**
     * 解密
     */
    @Test
    fun decrypt() {
        val pair = ECCUtils.genECKeyPair()

        val enc = ECCUtils.encrypt(content, pair.public)

        assert(ECCUtils.decrypt(enc, pair.private).contentEquals(content))

        // 私钥
        val pri = "58e09864a6ec21f07577a6a8a78a39d0ee4d6a1a6bb0dfb3c1a3b22245f9db1c".hex2Bytes()

        // 用公钥加密过后的私钥
        val encPri = "046e455b20fb2845fd59ba9d341421984e0ecc28d69df9d494b099ca378ecce1294b945f8d71" +
            "45bfb04428884d834521d7ed41bed4a3cf1f00138be03a8df5433180db1dbcb3de96e191febcf20be4905" +
            "2cb327e4db36f5d80db0281e0b1603f4adf449d329c1644d337ec3d0fcfa318ebed83b292"

        assert(ECCUtils.decrypt(encPri.hex2Bytes(), pri.toECPrivateKey()).contentEquals(pri))
    }
}