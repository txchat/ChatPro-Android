package com.fzm.chat.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fzm.chat.R
import com.fzm.chat.conversation.ChatActivity
import com.zjy.architecture.ext.dp

/**
 * @author zhengjy
 * @since 2021/02/04
 * Description:
 */
object ShortcutHelper {

    fun addContactShortcut(context: Context, address: String, channelType: Int, name: String, avatar: String) {
        loadIcon(context, avatar) {
            addContactShortcut(context, address, channelType, name, it)
        }
    }

    fun addContactShortcut(context: Context, address: String, channelType: Int, name: String, icon: IconCompat) {
        val person = Person.Builder().setIcon(icon).setName(name).build()
        addDynamicShortcut(
            context,
            address,
            name,
            "",
            icon,
            Intent(context, ChatActivity::class.java).apply {
                putExtra("channelType", channelType)
                putExtra("address", address)
                putExtra("name", name)
                action = Intent.ACTION_DEFAULT
            },
            arrayOf(person)
        )
    }

    fun addDynamicShortcut(
        context: Context,
        id: String,
        label: String,
        longLabel: String = "",
        icon: IconCompat,
        intent: Intent,
        persons: Array<Person>? = null
    ) {
        val shortcut = ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(label)
            .setLongLabel(longLabel)
            .setIcon(icon)
            .setIntent(intent)
            .apply {
                persons?.also { setPersons(it) }
            }
            .build()
        ShortcutManagerCompat.addDynamicShortcuts(context, listOf(shortcut))
    }

    fun removeAllDynamicShortcuts(context: Context) {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
    }

    private fun loadIcon(context: Context, url: String, callback: (IconCompat) -> Unit) {
        Glide.with(context).asBitmap().load(url).into(object : CustomTarget<Bitmap>(65.dp, 65.dp) {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                callback(IconCompat.createWithAdaptiveBitmap(resource))
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                callback(IconCompat.createWithResource(context, R.mipmap.default_avatar_round))
            }

            override fun onLoadCleared(placeholder: Drawable?) {

            }
        })
    }
}