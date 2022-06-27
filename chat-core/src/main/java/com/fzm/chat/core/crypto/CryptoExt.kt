package com.fzm.chat.core.crypto

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import com.fzm.chat.core.data.bean.getPubKey
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.AddressValidationUtils
import com.fzm.chat.core.utils.CipherUtils
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.ext.hex2Bytes
import com.zjy.architecture.util.FileUtils
import com.zjy.architecture.util.isContent
import com.zjy.architecture.util.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * @author zhengjy
 * @since 2021/09/22
 * Description:
 */
private const val ALGORITHM = "AES"
private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"

private const val IV_LENGTH = 16

suspend fun InputStream.encrypt(target: String?): InputStream {
    if (target == null) return this
    val aesKey = target.contactKey() ?: return this
    val spec = SecretKeySpec(aesKey, ALGORITHM)
    val cipher = Cipher.getInstance(TRANSFORMATION)
    val iv = generateIV()
    cipher.init(Cipher.ENCRYPT_MODE, spec, IvParameterSpec(iv))
    // 将iv放在流最前面
    return SequenceInputStream(ByteArrayInputStream(iv), CipherInputStream(this, cipher))
}

suspend fun InputStream.decrypt(target: String?): InputStream {
    if (target == null) return this
    return try {
        val aesKey = target.contactKey() ?: return this
        val iv = ByteArray(IV_LENGTH)
        // 读取最开始的16字节作为iv
        read(iv)
        val spec = SecretKeySpec(aesKey, ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, spec, IvParameterSpec(iv))
        CipherInputStream(this, cipher)
    } catch (e: Exception) {
        this
    }
}

private fun generateIV(): ByteArray {
    val rnd = SecureRandom()
    val iv = ByteArray(IV_LENGTH)
    rnd.nextBytes(iv)
    return iv
}

private suspend fun String.contactKey(): ByteArray? {
    val delegate by rootScope.inject<LoginDelegate>()
    val manager by rootScope.inject<ContactManager>()
    return try {
        if (AddressValidationUtils.bitCoinAddressValidate(this)) {
            val contact = manager.getUserInfo(this, true)
            CipherUtils.generateDHSessionKey(delegate.preference.PRI_KEY, contact.getPubKey())
        } else {
            val group = manager.getGroupInfo(this)
            group.key?.hex2Bytes()
        }
    } catch (e: Exception) {
        null
    }
}

private fun getExtension(context: Context, uri: Uri?): String {
    if (uri == null) return ""
    val ext = context.contentResolver.getType(uri)
    return MimeTypeMap.getSingleton().getExtensionFromMimeType(ext) ?: ""
}

/**
 * 将给定文件路径转为加密文件，放在应用缓存目录中
 */
suspend fun String.toEncryptFile(context: Context, target: String): File? {
    return withContext(Dispatchers.IO) {
        try {
            val path = this@toEncryptFile
            val ext: String
            val inputStream: InputStream?
            if (path.isContent()) {
                val uri = Uri.parse(path)
                ext = getExtension(context, uri)
                inputStream = context.contentResolver.openInputStream(uri)?.encrypt(target)
            } else {
                ext = FileUtils.getExtension(path)
                inputStream = FileInputStream(File(path)).encrypt(target)
            }
            val timeStamp = System.currentTimeMillis()
            val cacheDir = context.externalCacheDir ?: context.cacheDir
            val temp = File(cacheDir, "TMP_ENC_${timeStamp}_${generateRandomString(5)}.$ext")
            inputStream?.use { input ->
                BufferedOutputStream(FileOutputStream(temp)).use {
                    input.copyTo(it)
                    it.flush()
                }
            }
            temp
        } catch (e: Exception) {
            logE(Log.getStackTraceString(e))
            null
        }
    }
}

private fun generateRandomString(length: Int): String {
    val str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val random = Random()
    val sb = StringBuilder()
    for (i in 0 until length) {
        val number = random.nextInt(62)
        sb.append(str[number])
    }
    return sb.toString()
}

//***************************群聊加密相关***************************//
fun String?.encrypt(aesKey: String?): String {
    if (this.isNullOrEmpty()) return ""
    return try {
        if (aesKey.isNullOrEmpty()) return this
        val spec = SecretKeySpec(aesKey.hex2Bytes(), ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = generateIV()
        cipher.init(Cipher.ENCRYPT_MODE, spec, IvParameterSpec(iv))
        val encode = iv + cipher.doFinal(toByteArray())
        String(Base64.encode(encode, Base64.NO_WRAP), StandardCharsets.US_ASCII)
    } catch (e: Exception) {
        this
    }
}

fun String?.decrypt(aesKey: String?): String {
    if (this.isNullOrEmpty()) return ""
    if (aesKey.isNullOrEmpty()) return this
    return try {
        val decode = Base64.decode(toByteArray(StandardCharsets.US_ASCII), Base64.NO_WRAP)
        val iv = ByteArray(IV_LENGTH)
        val enc = ByteArray(decode.size - IV_LENGTH)
        // 读取最开始的16字节作为iv
        System.arraycopy(decode, 0, iv, 0, IV_LENGTH)
        System.arraycopy(decode, IV_LENGTH, enc, 0, enc.size)
        val spec = SecretKeySpec(aesKey.hex2Bytes(), ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, spec, IvParameterSpec(iv))
        String(cipher.doFinal(enc))
    } catch (e: Exception) {
        this
    }
}

fun ByteArray.encrypt(aesKey: String?): ByteArray {
    if (aesKey.isNullOrEmpty()) return this
    return try {
        val spec = SecretKeySpec(aesKey.hex2Bytes(), ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val iv = generateIV()
        cipher.init(Cipher.ENCRYPT_MODE, spec, IvParameterSpec(iv))
        return iv + cipher.doFinal(this)
    } catch (e: Exception) {
        this
    }
}

fun ByteArray.decrypt(aesKey: String?): ByteArray {
    if (aesKey.isNullOrEmpty()) return this
    return try {
        val iv = ByteArray(IV_LENGTH)
        val enc = ByteArray(size - IV_LENGTH)
        // 读取最开始的16字节作为iv
        System.arraycopy(this, 0, iv, 0, IV_LENGTH)
        System.arraycopy(this, IV_LENGTH, enc, 0, enc.size)
        val spec = SecretKeySpec(aesKey.hex2Bytes(), ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, spec, IvParameterSpec(iv))
        cipher.doFinal(enc)
    } catch (e: Exception) {
        this
    }
}