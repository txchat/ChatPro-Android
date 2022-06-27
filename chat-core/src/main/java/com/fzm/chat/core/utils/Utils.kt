package com.fzm.chat.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.net.toUri
import com.zjy.architecture.util.isContent
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat

/**
 * @author zhengjy
 * @since 2021/01/08
 * Description:
 */
object Utils {

    fun getImageSize(context: Context, uri: Uri): IntArray {
        val options = BitmapFactory.Options()
        /**
         * 关键options.inJustDecodeBounds = true;
         * 这里再decodeFileDescriptor()，返回的bitmap为空，但此时调用options.outHeight时，已经包含了图片的高了
         */
        options.inJustDecodeBounds = true
        val fd = context.contentResolver.openFileDescriptor(uri, "r")
        if (fd != null) {
            BitmapFactory.decodeFileDescriptor(fd.fileDescriptor, null, options)
        }

        /**
         * options.outHeight为原始图片的高
         */
        val result = IntArray(2)
        result[0] = options.outHeight
        result[1] = options.outWidth
        return result
    }

    fun getImageSize(path: String): IntArray {
        val options = BitmapFactory.Options()
        /**
         * 关键options.inJustDecodeBounds = true;
         * 这里再decodeFileDescriptor()，返回的bitmap为空，但此时调用options.outHeight时，已经包含了图片的高了
         */
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)

        /**
         * options.outHeight为原始图片的高
         */
        val result = IntArray(2)
        result[0] = options.outHeight
        result[1] = options.outWidth
        return result
    }

    fun getImageCacheDir(context: Context): String {
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        val folder = File(cacheDir, "/Pictures")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder.absolutePath
    }

    fun getImageDir(context: Context): String {
        val folder = context.getExternalFilesDir("Pictures") ?: File(context.filesDir.absolutePath, "Pictures")
        if (!folder.exists()) {
            folder.mkdirs()
        }
        return folder.absolutePath
    }

    private fun getMediaMetadataRetriever(context: Context, path: String): MediaMetadataRetriever {
        val media = MediaMetadataRetriever()
        if (path.isContent()) {
            media.setDataSource(context, path.toUri())
        } else if (path.startsWith("http://")
            || path.startsWith("https://")
            || path.startsWith("widevine://")) {
            media.setDataSource(path, HashMap())
        } else {
            media.setDataSource(path)
        }
        return media
    }

    /**
     * 获取视频总时长
     *
     * @param path
     * @return
     */
    fun getVideoDuration(context: Context, path: String?): Long {
        if (path.isNullOrEmpty()) return 0L
        val media = getMediaMetadataRetriever(context, path)
        val duration = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        return (duration?.toLong() ?: 0L / 1000 + 0.5f).toLong()
    }

    /**
     * 根据路径得到视频尺寸
     *
     * @return
     */
    fun getVideoSize(context: Context, path: String?): IntArray {
        val result = IntArray(2)
        if (path.isNullOrEmpty()) return result
        val media = getMediaMetadataRetriever(context, path)

        result[0] = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
            ?: 0
        result[1] = media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
            ?: 0
        return result
    }

    fun getVideoSize2(context: Context, path: String?): IntArray {
        val result = IntArray(2)
        if (path.isNullOrEmpty()) return result
        val bitmap = getVideoPhoto(context, path)

        result[0] = bitmap?.height ?: 0
        result[1] = bitmap?.width ?: 0
        bitmap?.recycle()
        return result
    }

    /**
     * 根据路径得到视频缩略图
     *
     * @param videoPath
     * @return
     */
    fun getVideoPhoto(context: Context, videoPath: String?): Bitmap? {
        if (videoPath.isNullOrEmpty()) return null
        val media = getMediaMetadataRetriever(context, videoPath)
        return media.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    }

    fun formatVideoDuration(time: Int): String {
        val sb = StringBuilder()
        val minutes = time / 60
        val second = time % 60
        if (minutes < 10) {
            sb.append(0)
        }
        sb.append(minutes).append(":")
        if (second < 10) {
            sb.append(0)
        }
        sb.append(second)
        return sb.toString()
    }

    fun byteToSize(size: Long): String {
        val bytes = StringBuffer()
        val format = DecimalFormat("###.0")
        if (size >= 1024 * 1024 * 1024) {
            val i = size / (1024.0 * 1024.0 * 1024.0)
            bytes.append(format.format(i)).append("GB")
        } else if (size >= 1024 * 1024) {
            val i = size / (1024.0 * 1024.0)
            bytes.append(format.format(i)).append("MB")
        } else if (size >= 1024) {
            val i = size / 1024.0
            bytes.append(format.format(i)).append("KB")
        } else if (size < 1024) {
            if (size <= 0) {
                bytes.append("0B")
            } else {
                bytes.append(size.toInt()).append("B")
            }
        }
        return bytes.toString()
    }

    fun formatDay(datetime: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd")
        return sdf.format(datetime)
    }
}