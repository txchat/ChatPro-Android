package com.fzm.chat.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.fzm.chat.R
import com.zjy.architecture.ext.toBitmap
import java.security.MessageDigest

/**
 * @author zhengjy
 * @since 2021/12/21
 * Description:
 */
class VideoIconTransformation(
    private val context: Context,
    private val isVideo: Boolean
) : BitmapTransformation() {

    private val ID = "com.fzm.chat.widget.VideoIconTransformation"
    private val ID_BYTES = ID.toByteArray(CHARSET)

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        if (!isVideo) {
            return toTransform
        }
        return addVideoIcon(toTransform)
    }

    private fun addVideoIcon(oldBitmap: Bitmap): Bitmap {
        val width = oldBitmap.width
        val height = oldBitmap.height

        val video =
            ResourcesCompat.getDrawable(context.resources, R.drawable.ic_msg_video_play_small, null)
                ?.toBitmap() ?: return oldBitmap

        Canvas(oldBitmap).apply {
            drawBitmap(oldBitmap, 0f, 0f, null)
            drawBitmap(video, (width - video.width) / 2f, (height - video.height) / 2f, null)
            video.recycle()
        }
        return oldBitmap
    }
}