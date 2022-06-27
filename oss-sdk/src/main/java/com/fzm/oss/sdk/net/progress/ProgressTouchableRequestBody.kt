package com.fzm.oss.sdk.net.progress

import android.os.Handler
import android.os.Looper
import com.fzm.oss.sdk.model.OSSRequest
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

/**
 * @author zhengjy
 * @since 2021/08/23
 * Description:
 */
class ProgressTouchableRequestBody<T : OSSRequest> : RequestBody {

    private var bytes: ByteArray? = null
    private var inputStream: InputStream? = null
    private var contentType: String? = null
    private var contentLength: Long = 0L
    private var callback: OSSProgressCallback<T>?
    private var request: T

    private var lastProgressTime = 0L

    private val handler = Handler(Looper.getMainLooper())

    constructor(
        input: InputStream?,
        contentLength: Long,
        contentType: String?,
        request: T,
        callback: OSSProgressCallback<T>?
    ) {
        inputStream = input
        this.contentType = contentType
        this.contentLength = contentLength
        this.callback = callback
        this.request = request
    }

    constructor(
        bytes: ByteArray,
        contentLength: Long,
        contentType: String?,
        request: T,
        callback: OSSProgressCallback<T>?
    ) {
        this.bytes = bytes
        this.contentType = contentType
        this.contentLength = contentLength
        this.callback = callback
        this.request = request
    }

    override fun contentType(): MediaType? {
        return contentType?.let { MediaType.parse(it) }
    }

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return contentLength
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val source = if (inputStream != null) {
            Okio.source(inputStream!!)
        } else {
            Okio.source(ByteArrayInputStream(bytes))
        }
        var total: Long = 0
        var read: Long
        var toRead: Long
        var remain: Long
        while (total < contentLength) {
            remain = contentLength - total
            toRead = min(remain, SEGMENT_SIZE.toLong())
            read = source.read(sink.buffer(), toRead)
            if (read == -1L) {
                break
            }
            total += read
            sink.flush()
            callback?.also {
                // 最少100ms刷新一次
                if (total != 0L && (System.currentTimeMillis() - lastProgressTime > 100 || total == contentLength)) {
                    handler.post { it.onProgress(request, total, contentLength) }
                    lastProgressTime = System.currentTimeMillis()
                }
            }
        }
        source.close()
    }

    companion object {
        private const val SEGMENT_SIZE = 8192 // okio.Segment.SIZE
    }
}