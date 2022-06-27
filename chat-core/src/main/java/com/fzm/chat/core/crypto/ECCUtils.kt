package com.fzm.chat.core.crypto

import com.zjy.architecture.ext.hex2Bytes
import com.zjy.architecture.ext.sha256Bytes
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Sha256Hash
import org.spongycastle.asn1.sec.SECNamedCurves
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.spongycastle.jcajce.provider.asymmetric.ec.IESCipher
import org.spongycastle.jce.provider.BouncyCastleProvider
import org.spongycastle.jce.spec.ECParameterSpec
import org.spongycastle.jce.spec.ECPrivateKeySpec
import java.math.BigInteger
import java.security.*
import javax.crypto.Cipher

/**
 * @author zhengjy
 * @since 2021/09/01
 * Description:
 */
object ECCUtils {

    /**
     * 椭圆曲线
     */
    const val SecP256K1 = "secp256k1"

    /**
     * 密钥生成算法
     */
    const val KEY_ALGORITHM = "EC"

    /**
     * 非对称加密算法
     */
    const val ECIES = "ECIES"

    /**
     * 签名算法
     */
    const val SHA256_ECDSA = "SHA256withECDSA"

    private val SecP256K1_SPEC by lazy {
        val params = SECNamedCurves.getByName(SecP256K1)
        ECParameterSpec(params.curve, params.g, params.n, params.h, params.seed)
    }

    init {
        try {
            Security.addProvider(BouncyCastleProvider())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun BCECPublicKey.toBytes(compressed: Boolean = true): ByteArray {
        return q.getEncoded(compressed)
    }

    fun BCECPrivateKey.toBytes(): ByteArray {
        return d.toByteArray()
    }

    fun PublicKey.toBytes(compressed: Boolean = true): ByteArray {
        return (this as BCECPublicKey).q.getEncoded(compressed)
    }

    fun PrivateKey.toBytes(): ByteArray {
        return (this as BCECPrivateKey).d.toByteArray()
    }

    fun ByteArray.toECPrivateKey(): BCECPrivateKey {
        return BCECPrivateKey(
            KEY_ALGORITHM,
            ECPrivateKeySpec(BigInteger(1, this), SecP256K1_SPEC),
            BouncyCastleProvider.CONFIGURATION
        )
    }

    fun String.toECPrivateKey(): BCECPrivateKey {
        return BCECPrivateKey(
            KEY_ALGORITHM,
            ECPrivateKeySpec(BigInteger(1, this.hex2Bytes()), SecP256K1_SPEC),
            BouncyCastleProvider.CONFIGURATION
        )
    }

    /**
     * 生成EC公私钥对
     */
    fun genECKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance(KEY_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME)
        gen.initialize(SecP256K1_SPEC)
        return gen.genKeyPair()
    }

    /**
     * 用EC私钥进行签名
     */
    fun sign(data: ByteArray, privateKey: PrivateKey): ByteArray {
        val s = Signature.getInstance(SHA256_ECDSA)
        s.initSign(privateKey)
        s.update(data)
        return s.sign()
    }

    /**
     * 用EC公钥进行验签
     */
    fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey): Boolean {
        val s = Signature.getInstance(SHA256_ECDSA)
        s.initVerify(publicKey)
        s.update(data)
        return s.verify(signature)
    }

    fun encrypt(data: ByteArray, publicKey: PublicKey): ByteArray {
        val cipher = IESCipher.ECIES()
        cipher.engineInit(Cipher.ENCRYPT_MODE, publicKey, SecureRandom())
        return cipher.engineDoFinal(data, 0, data.size)
    }

    fun decrypt(data: ByteArray, privateKey: PrivateKey): ByteArray {
        val cipher = IESCipher.ECIES()
        cipher.engineInit(Cipher.DECRYPT_MODE, privateKey, SecureRandom())
        return cipher.engineDoFinal(data, 0, data.size)
    }

    /*==================================bitcoinj实现==================================*/

    /**
     * 使用bitcoinj方法签名
     */
    fun btcCoinSign(data: ByteArray, privateKey: ByteArray): ByteArray {
        val sha256Hash = Sha256Hash.wrap(data.sha256Bytes())
        val ecKey = ECKey.fromPrivate(privateKey)
        return ecKey.sign(sha256Hash).encodeToDER()
    }

    /**
     * 使用bitcoinj方法验签
     */
    fun btcCoinVerify(data: ByteArray, signature: ByteArray, publicKey: ByteArray): Boolean {
        val ecKey = ECKey.fromPublicOnly(publicKey)
        return ecKey.verify(data.sha256Bytes(), signature)
    }
}