package com.fzm.chat.core.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.fzm.chat.core.data.model.ChatMessage
import java.io.InputStream

/**
 * @author zhengjy
 * @since 2021/09/22
 * Description:
 */
@GlideModule
class EncryptGlideModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        glide.registry.append(ChatMessage::class.java, InputStream::class.java, ChatEncryptLoader.LoaderFactory())
        glide.registry.append(ForwardModel::class.java, InputStream::class.java, ChatEncryptLoader2.LoaderFactory())
    }
}