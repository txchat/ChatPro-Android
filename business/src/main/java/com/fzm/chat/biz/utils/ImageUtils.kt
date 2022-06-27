package com.fzm.chat.biz.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import com.zjy.architecture.Arch
import com.zjy.architecture.util.isAndroidQ
import java.io.File
import java.io.IOException

/**
 * @author zhengjy
 * @since 2021/01/26
 * Description:
 */
object ImageUtils {

    fun saveImageToGallery(context: Context, drawable: Drawable?, folder: String): Uri? {
        if (drawable == null) return null
        val bmp = drawable2Bitmap(drawable)
        return saveBitmapToGallery(context, bmp, folder)
    }

    @SuppressLint("InlinedApi")
    fun saveBitmapToGallery(context: Context, bmp: Bitmap?, folder: String): Uri? {
        if (folder.isEmpty() || bmp == null) {
            return null
        }
        val fileName = "IMG_" + System.currentTimeMillis() + ".jpg"
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            .absolutePath + "/" + folder
        val resolver = context.contentResolver
        //设置文件参数到ContentValues中
        val values = ContentValues()
        //设置文件名
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        //设置文件类型为image/*
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
        //注意：MediaStore.Images.Media.RELATIVE_PATH需要targetSdkVersion=29,
        //故该方法只可在Android10的手机上执行
        if (isAndroidQ) {
            values.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/" + folder
            )
            values.put(MediaStore.MediaColumns.IS_PENDING, 1)
        } else {
            values.put(MediaStore.MediaColumns.DATA, "$path/$fileName")
        }
        //EXTERNAL_CONTENT_URI代表外部存储器
        val external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        //insertUri表示文件保存的uri路径
        val insertUri = resolver.insert(external, values)
        if (insertUri != null) {
            try {
                if (!isAndroidQ) {
                    // android10以下可能需要手动创建文件夹
                    val file = File(path)
                    if (!file.exists()) {
                        file.mkdirs()
                    }
                }
                val output = resolver.openOutputStream(insertUri)
                // 将Bitmap写入insertUri
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, output)
                output?.flush()
                output?.close()
                if (isAndroidQ) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(insertUri, values, null, null)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return insertUri
    }

    fun drawable2Bitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)
        //canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }

//    fun save(src: Bitmap, file: File, format: Bitmap.CompressFormat, recycle: Boolean):Boolean {
//
//    }

    fun Bitmap.blur(radius: Float = 10f): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.flags = Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG
        val filter = PorterDuffColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_ATOP)
        paint.colorFilter = filter
        canvas.drawBitmap(this, 0f, 0f, paint)
        val rs = RenderScript.create(Arch.context)
        val input = Allocation.createFromBitmap(
            rs, bitmap, Allocation.MipmapControl.MIPMAP_NONE,
            Allocation.USAGE_SCRIPT
        )
        val output = Allocation.createTyped(rs, input.type)
        val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blur.setInput(input)
        blur.setRadius(radius)
        blur.forEach(output)
        output.copyTo(bitmap)
        rs.destroy()
        return bitmap
    }
}