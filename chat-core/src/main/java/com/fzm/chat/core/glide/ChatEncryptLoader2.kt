package com.fzm.chat.core.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DataSource.REMOTE
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.fzm.chat.core.crypto.decrypt
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.model.ForwardMsg
import com.fzm.chat.core.data.model.localExists
import com.fzm.chat.core.di.CoreInjector
import com.zjy.architecture.di.rootScope
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*

/**
 * @author zhengjy
 * @since 2021/09/24
 * Description:
 */
data class ForwardModel(
    val forward: ForwardMsg,
    val target: String
) : Serializable

class ChatEncryptLoader2 : ModelLoader<ForwardModel, InputStream> {

    override fun buildLoadData(model: ForwardModel, width: Int, height: Int, options: Options): ModelLoader.LoadData<InputStream>? {
        return ModelLoader.LoadData(ObjectKey(model.forward.msg.mediaUrl ?: model.forward.msg.localUrl ?: ""), ChatDataFetcher(model))
    }

    override fun handles(s: ForwardModel): Boolean {
        return true
    }

    class ChatDataFetcher(private val model: ForwardModel) : DataFetcher<InputStream> {

        private val okHttpClient by rootScope.inject<OkHttpClient>(CoreInjector.crypto)

        @Volatile
        private var isCanceled: Boolean = false
        private var mInputStream: InputStream? = null

        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) = runBlocking {
            try {
                if (!isCanceled) {
                    val message = model.forward
                    if (!message.localExists()) {
                        val url = message.msg.mediaUrl ?: ""
                        val enc = url.contains(ChatConst.ENC_PREFIX)
                        if (!url.startsWith("http")) {
                            callback.onLoadFailed(Exception("url?????????http??????"))
                            return@runBlocking
                        }
                        val call = okHttpClient.newCall(Request.Builder().url(url).build())
                        val response = call.execute()
                        if (response.isSuccessful) {
                            val input = response.body()?.byteStream()
                            if (input == null) {
                                callback.onLoadFailed(Exception(response.message()))
                                return@runBlocking
                            }
                            mInputStream = if (enc) input.decrypt(model.target) else input
                        } else {
                            callback.onLoadFailed(Exception(response.message()))
                            return@runBlocking
                        }
                    } else {
                        mInputStream = FileInputStream(message.msg.localUrl)
                    }
                }
                if (isCanceled) {
                    callback.onDataReady(null)
                    return@runBlocking
                }
                callback.onDataReady(mInputStream)
            } catch (e: Exception) {
                callback.onLoadFailed(e)
            }
        }

        override fun cleanup() {
            try {
                mInputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun cancel() {
            isCanceled = true
        }

        override fun getDataClass(): Class<InputStream> {
            return InputStream::class.java
        }

        override fun getDataSource(): DataSource {
            return REMOTE
        }
    }

    class LoaderFactory : ModelLoaderFactory<ForwardModel, InputStream> {

        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ForwardModel, InputStream> {
            return ChatEncryptLoader2()
        }

        override fun teardown() {

        }
    }
}
