package com.fzm.chat.conversation

import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.session.LoginDelegate
import com.zjy.architecture.mvvm.LoadingViewModel
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2020/12/28
 * Description:
 */
class SessionViewModel(
    delegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by delegate {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    val personSession by lazy { database.recentSessionDao().getPrivateSessions() }

    val groupSession by lazy { database.recentSessionDao().getGroupSessions() }

    fun changeStickTop(id: String, channelType: Int, stickTop: Boolean) {
        launch {
            if (channelType == ChatConst.PRIVATE_CHANNEL) {
                if (stickTop) {
                    database.friendUserDao().addFlag(id, Contact.STICK_TOP)
                } else {
                    database.friendUserDao().deleteFlag(id, Contact.STICK_TOP)
                }
            } else if (channelType == ChatConst.GROUP_CHANNEL) {
                if (stickTop) {
                    database.groupDao().addFlag(id.toLong(), Contact.STICK_TOP)
                } else {
                    database.groupDao().deleteFlag(id.toLong(), Contact.STICK_TOP)
                }
            }
        }
    }

    fun changeNoDisturb(id: String, channelType: Int, noDisturb: Boolean) {
        launch {
            if (channelType == ChatConst.PRIVATE_CHANNEL) {
                if (noDisturb) {
                    database.friendUserDao().addFlag(id, Contact.NO_DISTURB)
                } else {
                    database.friendUserDao().deleteFlag(id, Contact.NO_DISTURB)
                }
            } else if (channelType == ChatConst.GROUP_CHANNEL) {
                if (noDisturb) {
                    database.groupDao().addFlag(id.toLong(), Contact.NO_DISTURB)
                } else {
                    database.groupDao().deleteFlag(id.toLong(), Contact.NO_DISTURB)
                }
            }
        }
    }

    fun deleteSession(id: String, channelType: Int) {
        launch {
            database.recentSessionDao().deleteSession(id, channelType)
        }
    }
}