package com.fzm.chat.core.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.model.*
import com.fzm.chat.core.data.po.*

/**
 * @author zhengjy
 * @since 2020/12/28
 * Description:
 */
@Dao
interface RecentSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sessions: List<RecentSession>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: RecentSession)

    @Query("DELETE FROM recent_session WHERE id=:id AND channelType=:channelType")
    suspend fun deleteSessionInner(id: String, channelType: Int)

    @Query(
        """
        SELECT r.*, f.nickname, f.avatar, f.remark, f.servers, f.flag, c.name as teamName 
        FROM recent_session r 
        LEFT JOIN friend_user f 
        ON r.id=f.address 
        LEFT JOIN company_user c 
        ON r.id=c.id 
        WHERE r.channelType=0
    """
    )
    fun getPrivateSessions(): LiveData<List<RecentFriendMsg>>

    @Query(
        """
        SELECT r.*, g.name, g.publicName, g.avatar, g.server, g.flag, g.groupType 
        FROM recent_session r 
        LEFT JOIN group_info g 
        ON r.id=g.gid 
        WHERE r.channelType=1
    """
    )
    fun getGroupSessions(): LiveData<List<RecentGroupMsg>>

    @Query(
        """
        SELECT r.*, f.nickname, f.avatar, f.remark, f.servers, f.flag, c.name as teamName 
        FROM recent_session r 
        LEFT JOIN friend_user f 
        ON r.id=f.address 
        LEFT JOIN company_user c 
        ON r.id=c.id 
        WHERE r.channelType=0
    """
    )
    suspend fun getPrivateSessionList(): List<RecentFriendMsg>

    @Query(
        """
        SELECT r.*, g.name, g.publicName, g.avatar, g.server, g.flag, g.groupType 
        FROM recent_session r 
        LEFT JOIN group_info g 
        ON r.id=g.gid 
        WHERE r.channelType=1
    """
    )
    suspend fun getGroupSessionList(): List<RecentGroupMsg>

    @Query("SELECT * FROM recent_session WHERE id=:id AND channelType=:channelType")
    suspend fun getRecentSession(id: String, channelType: Int): RecentSession?

    @Query("UPDATE recent_session SET unread=0 WHERE id=:id AND channelType=:channelType")
    suspend fun clearUnread(id: String, channelType: Int)

    @Query("SELECT unread FROM recent_session WHERE id=:id AND channelType=:channelType")
    suspend fun getUnreadMsgCount(id: String, channelType: Int): Int?

    @Query("""
        SELECT sum(unread)
        FROM recent_session r 
        LEFT JOIN group_info g 
        ON r.id=g.gid 
        WHERE channelType=:channelType AND g.flag & :flag=0
    """
    )
    suspend fun getGroupUnreadCount(channelType: Int, flag: Int): Int?

    @Query(
        """
        SELECT sum(unread)
        FROM recent_session r 
        LEFT JOIN friend_user f 
        ON r.id=f.address 
        WHERE channelType=:channelType AND f.flag & :flag=0
    """
    )
    suspend fun getFriendUnreadCount(channelType: Int, flag: Int): Int?

    @Transaction
    suspend fun getUnreadCount(): Int {
        val count1 = getGroupUnreadCount(ChatConst.GROUP_CHANNEL, Contact.NO_DISTURB) ?: 0
        val count2 = getFriendUnreadCount(ChatConst.PRIVATE_CHANNEL, Contact.NO_DISTURB) ?: 0
        return count1 + count2
    }

    @Query("UPDATE recent_session SET beAt=0 AND atMessages=null WHERE id=:id AND channelType=:channelType")
    suspend fun clearAtMsg(id: String, channelType: Int)

    @Query("UPDATE recent_session SET msg_logId=:logId WHERE id=:id AND channelType=:channelType")
    suspend fun updateRecentLogId(logId: Long, id: String, channelType: Int)

    @Query("UPDATE recent_session SET draft=:draft WHERE id=:id AND channelType=:channelType")
    suspend fun updateDraft(draft: Draft?, id: String, channelType: Int)

    @Query("SELECT draft FROM recent_session WHERE id=:id AND channelType=:channelType")
    suspend fun getDraft(id: String, channelType: Int): Draft?

    @Transaction
    suspend fun createOrUpdateDraft(draft: Draft?, id: String, channelType: Int) {
        if (draft == null) {
            // ??????????????????????????????????????????????????????
            updateDraft(null, id, channelType)
            // 2021???11???18??? 17:30:12 ?????????????????????
            // updateRecentLog(id, channelType)
            return
        }
        val session = getRecentSession(id, channelType)
        if (session != null) {
            val oldDraft = getDraft(id, channelType)
            if (oldDraft?.text != draft.text || oldDraft.atInfo != draft.atInfo) {
                updateDraft(draft, id, channelType)
            }
        } else {
            val message = getLatestMessage(id, channelType)
            if (message != null) {
                insert(RecentSession(id, channelType, 0, false, emptyList(), draft, message.toRecentLog()))
            } else {
                insert(RecentSession(id, channelType, 0, false, emptyList(), draft, RecentLog.EMPTY_LOG))
            }
        }
    }

    /**
     * ???????????????????????????
     *
     * @param message   ??????????????????
     * @param unread    ??????????????????
     * @param beAt      ?????????@
     *
     * @return  ???????????????id???????????????-1??????????????????????????????
     */
    @Transaction
    suspend fun insertMessage(message: MessagePO, unread: Int, beAt: Boolean): Long {
        val row = insert(message)
        if (row != -1L) {
            updateRecent(message, unread, beAt)
        }
        return row
    }

    @Transaction
    suspend fun insertMessageReplace(message: MessagePO, unread: Int, beAt: Boolean): Long {
        val row = insertReplace(message)
        if (row != -1L) {
            updateRecent(message, unread, beAt)
        }
        return row
    }

    private suspend fun updateRecent(message: MessagePO, unread: Int, beAt: Boolean) {
        val id = message.contact
        val recent = getRecentSession(id, message.channelType)
        if (recent != null) {
            if (message.logId >= recent.message.logId || message.logId == 0L) {
                recent.message = message.toRecentLog()
            }
            recent.unread += unread
            recent.beAt = recent.beAt || beAt
            if (beAt) {
                recent.atMessages = recent.atMessages.toMutableList().apply {
                    add(0, message.logId.toString())
                }
            }
            insert(recent)
        } else {
            val atMessages = if (beAt) listOf(message.logId.toString()) else emptyList()
            insert(RecentSession(id, message.channelType, unread, beAt, atMessages, null, message.toRecentLog()))
        }
    }

    /**
     * ?????????????????????????????????????????????????????????????????????
     *
     * @param address       ??????????????????id
     * @param channelType   ????????????
     */
    @Transaction
    suspend fun updateRecentLog(address: String, channelType: Int) {
        val message = getLatestMessage(address, channelType)
        if (message != null) {
            val id = message.contact
            val recent = getRecentSession(id, message.channelType)
            if (recent != null) {
                recent.message = message.toRecentLog()
                insert(recent)
            } else {
                insert(RecentSession(id, message.channelType, 0, false, emptyList(), null, message.toRecentLog()))
            }
        } else {
            deleteSession(address, channelType)
        }
    }

    /**
     * ????????????????????????????????????????????????????????????
     *
     * @param address       ??????????????????id
     * @param channelType   ????????????
     * @param msgId         ??????msgId
     * @param logId         ??????logId
     */
    @Transaction
    suspend fun deleteAndUpdate(address: String, channelType: Int, msgId: String, logId: Long) {
        val msg = getLatestMessage(address, channelType)
        deleteMessage(msgId, logId, channelType)
        if (msg != null && msg.logId == logId && msg.msgId == msgId) {
            updateRecentLog(address, channelType)
        }
    }

    @Transaction
    suspend fun deleteAndUpdate(address: String, channelType: Int, logs: List<Pair<String, Long>>) {
        val msg = getLatestMessage(address, channelType)
        logs.forEach {
            deleteMessage(it.first, it.second, channelType)
        }
        val msgIds = logs.map { it.first }
        if (msg != null && msgIds.contains(msg.msgId)) {
            updateRecentLog(address, channelType)
        }
    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param id            ?????????????????????id
     * @param channelType   ????????????
     */
    @Transaction
    suspend fun deleteSession(id: String, channelType: Int) {
        deleteSessionInner(id, channelType)
        if (channelType == ChatConst.GROUP_CHANNEL) {
            val group = getGroupInfo(id.toLong())
            if (group?.isInGroup != true) {
                deleteGroup(id.toLong())
                deleteGroupUsersByGroup(id.toLong())
            }
        }
    }

    /*------------------------------??????????????????------------------------------*/
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessagePO): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplace(message: MessagePO): Long

    @Query("DELETE FROM chat_message WHERE msgId=:msgId AND logId=:logId AND channelType=:channelType")
    suspend fun deleteMsg(msgId: String, logId: Long, channelType: Int)

    @Query("DELETE FROM message_focus_user WHERE logId=:logId")
    suspend fun deleteFocusInfo(logId: Long)

    @Transaction
    suspend fun deleteMessage(msgId: String, logId: Long, channelType: Int) {
        deleteMsg(msgId, logId, channelType)
        if (logId != 0L) {
            deleteFocusInfo(logId)
        }
    }

    @Query(
        """
        SELECT * FROM chat_message 
        WHERE channelType=:channelType AND (`from`=:address OR target=:address) 
        ORDER BY datetime DESC LIMIT 1
    """
    )
    suspend fun getLatestMessage(address: String, channelType: Int): MessagePO?

    /*------------------------------??????????????????------------------------------*/
    @Query("SELECT * FROM group_info WHERE gid=:gid")
    suspend fun getGroupInfo(gid: Long): GroupInfo?

    @Query("DELETE FROM group_info WHERE gid=:gid")
    suspend fun deleteGroup(gid: Long)

    @Query("DELETE FROM group_user WHERE gid=:gid")
    suspend fun deleteGroupUsersByGroup(gid: Long)

    @Query("SELECT * FROM friend_user WHERE address=:address")
    suspend fun getFriendUser(address: String): FriendUser?

    @Query("DELETE FROM friend_user WHERE address=:address")
    suspend fun deleteFriendUser(address: String)
}