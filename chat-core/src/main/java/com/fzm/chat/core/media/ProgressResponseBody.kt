package com.fzm.chat.core.media

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*
import java.io.IOException

class ProgressResponseBody(
    private val tag: Any?,
    private val mResponseBody: ResponseBody?,
    private val callback: Callback? = null
) : ResponseBody() {

    private var mBufferedSource: BufferedSource? = null

    override fun contentType(): MediaType? {
        return mResponseBody?.contentType()
    }

    override fun contentLength(): Long {
        return mResponseBody?.contentLength() ?: 0L
    }

    override fun source(): BufferedSource? {
        if (mResponseBody == null) return null
        if (mBufferedSource == null) {
            mBufferedSource = Okio.buffer(source(mResponseBody.source()))
        }
        return mBufferedSource
    }

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            private var totalBytesRead = 0L

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                if (bytesRead != -1L && totalBytesRead != 0L) {
                    callback?.onProgress(tag, totalBytesRead.toFloat() / contentLength())
                }

                return bytesRead
            }
        }
    }

    interface Callback {
        fun onProgress(tag: Any?, progress: Float)
    }
}