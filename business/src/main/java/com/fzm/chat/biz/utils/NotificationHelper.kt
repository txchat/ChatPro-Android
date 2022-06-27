package com.fzm.chat.biz.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fzm.chat.biz.R
import com.fzm.chat.biz.ui.DeepLinkHelper
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.contact
import com.fzm.chat.core.data.model.getContent
import com.fzm.chat.core.data.po.RecentSession
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.main.MainService
import com.fzm.chat.router.route
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.notificationManager
import com.zjy.architecture.util.immutableFlag
import com.zjy.architecture.util.other.BadgeUtil
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/10/08
 * Description:
 */
object NotificationHelper {

    private const val REQUEST_BUBBLE = 1
    private const val REQUEST_NOTIFICATION = 2

    private val context by rootScope.inject<Context>()
    private val contactManager by rootScope.inject<ContactManager>()
    private val mainService by route<MainService>(MainModule.SERVICE)

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    suspend fun showNotification(message: ChatMessage) {
        val builder = NotificationCompat.Builder(context, "chatMessage")
        builder.setSmallIcon(R.drawable.ic_notification)
        // 会话对象信息
        val target = if (message.channelType == ChatConst.PRIVATE_CHANNEL) {
            contactManager.getUserInfo(message.from)
        } else {
            contactManager.getGroupInfo(message.target)
        }
        // 消息实际发送者信息
        val senderTarget = if (message.channelType == ChatConst.PRIVATE_CHANNEL) {
            contactManager.getUserInfo(message.from)
        } else {
            contactManager.getGroupUserInfo(message.target, message.from)
        }
        builder.setAutoCancel(true)

        val contentIntent = createIntent(REQUEST_NOTIFICATION, message, target)
        // 指定点击跳转页面
        builder.setContentIntent(contentIntent)
        builder.setCategory(Notification.CATEGORY_MESSAGE)
        builder.setShowWhen(true)
        val session = database.recentSessionDao().getRecentSession(message.contact, message.channelType)
        val count = database.recentSessionDao().getUnreadCount()

        Glide.with(context).asBitmap().load(target.getDisplayImage()).into(object : CustomTarget<Bitmap>(65.dp, 65.dp) {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                try {
                    val icon = IconCompat.createWithAdaptiveBitmap(resource)
//                    builder.setBubbleMetadata(createBubbleMetadata(icon, message, target))
//                    builder.setStyle(createStyle(icon, message, target, senderTarget, session?.unread))
                    builder.setContentTitle(target.getDisplayName())
                    builder.setContentText(getContentText(session, message, senderTarget))
                    builder.setLargeIcon(resource)

                    target.apply {
                        val id = mainService?.addContactShortcut(getId(), getType(), getDisplayName(), icon) ?: ""
                        builder.setShortcutId(id)
                    }
                    if (Build.MANUFACTURER.toLowerCase().contains("xiaomi")) {
                        BadgeUtil.setBadgeOfMIUI(context, count, message.contact.hashCode(), builder.build())
                    } else {
                        BadgeUtil.setBadgeCount(context, count)
                        context.notificationManager?.notify(message.contact.hashCode(), builder.build())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                try {
                    val icon = if (message.channelType == ChatConst.PRIVATE_CHANNEL) {
                        IconCompat.createWithResource(context, R.mipmap.default_avatar_round)
                    } else {
                        IconCompat.createWithResource(context, R.mipmap.default_avatar_room)
                    }
//                    builder.setBubbleMetadata(createBubbleMetadata(icon, message, target))
//                    builder.setStyle(createStyle(null, message, target, senderTarget, session?.unread))
                    builder.setContentTitle(target.getDisplayName())
                    builder.setContentText(getContentText(session, message, senderTarget))

                    target.apply {
                        val id = mainService?.addContactShortcut(getId(), getType(), getDisplayName(), icon) ?: ""
                        builder.setShortcutId(id)
                    }
                    if (Build.MANUFACTURER.toLowerCase().contains("xiaomi")) {
                        BadgeUtil.setBadgeOfMIUI(context, count, message.contact.hashCode(), builder.build())
                    } else {
                        BadgeUtil.setBadgeCount(context, count)
                        context.notificationManager?.notify(message.contact.hashCode(), builder.build())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {

            }
        })
    }

    private fun getContentText(session: RecentSession?, message: ChatMessage, senderTarget: Contact) : String {
        var num = ""
        if (session != null && session.unread > 1) {
            num = context.getString(R.string.core_notification_unread_number, session.unread)
        }
        var sender = ""
        if (message.channelType == ChatConst.GROUP_CHANNEL && message.msgType != Biz.MsgType.Notification_VALUE) {
            sender = "${senderTarget.getDisplayName()}:"
        }
        return "$num$sender${message.getContent(context)}"
    }

    private fun createStyle(icon: IconCompat?, message: ChatMessage, sessionTarget: Contact?,
                            senderTarget: Contact?, unread: Int?): NotificationCompat.MessagingStyle {
        val user = createPerson(icon, sessionTarget)
        return NotificationCompat.MessagingStyle(user)
            .also {
                var num = ""
                if (unread != null && unread > 1) {
                    num = context.getString(R.string.core_notification_unread_number, unread)
                }
                var sender = ""
                if (message.channelType == ChatConst.GROUP_CHANNEL && message.msgType != Biz.MsgType.Notification_VALUE) {
                    sender = "${senderTarget?.getDisplayName()}:"
                }
                it.addMessage("$num$sender${message.getContent(context)}", message.datetime, user)
            }
            .setGroupConversation(false)
    }

    private fun createPerson(icon: IconCompat?, target: Contact?): Person {
        return Person.Builder()
            .setIcon(icon)
            .setName(target?.getDisplayName())
            .build()
    }

    private fun createBubbleMetadata(icon: IconCompat, message: ChatMessage, target: Contact?): NotificationCompat.BubbleMetadata {
        return NotificationCompat.BubbleMetadata.Builder()
            .setDesiredHeight(400)
            .setIcon(icon)
            .setIntent(createIntent(REQUEST_BUBBLE, message, target))
            .build()
    }

    private fun createIntent(
        requestCode: Int,
        message: ChatMessage,
        target: Contact?
    ): PendingIntent {
        val intent = Intent().apply {
            val uri =
                Uri.parse("${DeepLinkHelper.APP_LINK}?type=chatNotification&address=${message.contact}&channelType=${target?.getType() ?: ChatConst.PRIVATE_CHANNEL}")
            component = ComponentName(
                context.packageName,
                "com.fzm.chat.app.MainActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("route", uri)
        }

        return PendingIntent.getActivity(
            context, message.contact.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT.immutableFlag
        )
    }
}