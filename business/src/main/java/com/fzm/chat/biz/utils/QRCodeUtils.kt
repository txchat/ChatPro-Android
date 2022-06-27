package com.fzm.chat.biz.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.king.zxing.DecodeFormatManager
import com.king.zxing.util.CodeUtils
import com.king.zxing.util.LogUtils
import com.zjy.architecture.util.isContent
import java.io.FileInputStream
import java.util.*

/**
 * @author zhengjy
 * @since 2021/01/26
 * Description:扩展[CodeUtils]中的图片二维码解析方法，支持uri和bitmap解析
 */
object QRCodeUtils {

    fun parseCode(context: Context, path: String?): String? {
        if (path == null) return null
        if (path.isContent()) {
            return parseCode(context, Uri.parse(path))
        }
        val bitmap = FileInputStream(path).use {
            BitmapFactory.decodeStream(FileInputStream(path))
        }
        return parseCode(bitmap)
    }

    fun parseCode(context: Context, uri: Uri?): String? {
        if (uri == null) return null
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        }
        return parseCode(bitmap)
    }

    fun parseCode(bitmap: Bitmap?): String? {
        if (bitmap == null) return null
        val hints: MutableMap<DecodeHintType, Any> = EnumMap(DecodeHintType::class.java)
        //添加可以解析的编码类型
        val decodeFormats = Vector<BarcodeFormat>()
        decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS)
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS)
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS)
        decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS)
        decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS)
        hints[DecodeHintType.CHARACTER_SET] = "utf-8"
        hints[DecodeHintType.TRY_HARDER] = true
        hints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats
        return parseCode(bitmap, hints)
    }

    fun parseCode(bitmap: Bitmap?, hints: Map<DecodeHintType, Any>?): String? {
        if (bitmap == null) return null
        val result: Result? = parseCodeResult(bitmap, hints)
        return result?.text
    }

    fun parseCodeResult(image: Bitmap?, hints: Map<DecodeHintType, Any>?): Result? {
        if (image == null) return null
        var result: Result? = null
        try {
            val reader = MultiFormatReader()
            reader.setHints(hints)
            val source = getRGBLuminanceSource(image)
            var isReDecode: Boolean
            try {
                val bitmap = BinaryBitmap(HybridBinarizer(source))
                result = reader.decodeWithState(bitmap)
                isReDecode = false
            } catch (e: Exception) {
                isReDecode = true
            }
            if (isReDecode) {
                try {
                    val bitmap = BinaryBitmap(HybridBinarizer(source.invert()))
                    result = reader.decodeWithState(bitmap)
                    isReDecode = false
                } catch (e: Exception) {
                    isReDecode = true
                }
            }
            if (isReDecode) {
                try {
                    val bitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
                    result = reader.decodeWithState(bitmap)
                    isReDecode = false
                } catch (e: Exception) {
                    isReDecode = true
                }
            }
            if (isReDecode && source.isRotateSupported) {
                try {
                    val bitmap = BinaryBitmap(HybridBinarizer(source.rotateCounterClockwise()))
                    result = reader.decodeWithState(bitmap)
                } catch (e: Exception) {
                }
            }
            reader.reset()
        } catch (e: Exception) {
            LogUtils.w(e.message)
        }
        return result
    }

    private fun getRGBLuminanceSource(bitmap: Bitmap): RGBLuminanceSource {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return RGBLuminanceSource(width, height, pixels)
    }
}