package com.fzm.chat.core.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fzm.chat.core.data.po.GroupInfo

/**
 * @author zhengjy
 * @since 2021/05/11
 * Description:
 */
@Dao
interface GroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: List<GroupInfo>)

    @Query("SELECT * FROM group_info WHERE gid=:gid")
    fun getGroupInfoLive(gid: Long): LiveData<GroupInfo>

    @Query("SELECT * FROM group_info WHERE gid=:gid")
    suspend fun getGroupInfo(gid: Long): GroupInfo?

    @Query("SELECT * FROM group_info WHERE serverUrlKey=:server AND flag & :flag == :flag")
    fun getGroupListByServer(server: String, flag: Int): LiveData<List<GroupInfo>>

    @Query("SELECT * FROM group_info WHERE flag & :flag == :flag")
    suspend fun getGroupList(flag: Int): List<GroupInfo>

    @Query("UPDATE group_info SET flag=flag & ~:flag WHERE gid=:gid")
    suspend fun deleteFlag(gid: Long, flag: Int)

    @Query("UPDATE group_info SET flag=flag | :flag WHERE gid=:gid")
    suspend fun addFlag(gid: Long, flag: Int)

    @Query("DELETE FROM group_info WHERE gid=:gid")
    suspend fun deleteGroup(gid: Long)

    @Query("UPDATE group_info SET name=:name, publicName=:publicName WHERE gid=:gid")
    suspend fun editGroupNames(gid: Long, name: String, publicName: String)

    @Query("UPDATE group_info SET name=:name WHERE gid=:gid")
    suspend fun editGroupName(gid: Long, name: String)

    @Query("UPDATE group_info SET publicName=:publicName WHERE gid=:gid")
    suspend fun editGroupPublicName(gid: Long, publicName: String)

    @Query("UPDATE group_info SET avatar=:avatar WHERE gid=:gid")
    suspend fun editGroupAvatar(gid: Long, avatar: String)

    @Query("SELECT * FROM group_info WHERE flag & :flag = :flag AND (name LIKE '%'||:keywords||'%' OR publicName LIKE '%'||:keywords||'%' OR searchKey LIKE '%'||:keywords||'%')")
    suspend fun searchGroups(keywords: String, flag: Int): List<GroupInfo>

    suspend fun changeMyRole(gid: Long, role: Int) {
        val groupInfo = getGroupInfo(gid)
        if (groupInfo != null) {
            groupInfo.person?.role = role
            insert(groupInfo)
        }
    }

    suspend fun changeMyMuteTime(gid: Long, muteTime: Long) {
        val groupInfo = getGroupInfo(gid)
        if (groupInfo != null) {
            groupInfo.person?.muteTime = muteTime
            insert(groupInfo)
        }
    }

    @Query("UPDATE group_info SET friendType=:friendType WHERE gid=:gid")
    suspend fun changeFriendType(gid: Long, friendType: Int)

    @Query("UPDATE group_info SET joinType=:joinType WHERE gid=:gid")
    suspend fun changeJoinType(gid: Long, joinType: Int)

    @Query("UPDATE group_info SET muteType=:muteType WHERE gid=:gid")
    suspend fun changeMuteType(gid: Long, muteType: Int)

    @Query("UPDATE group_info SET memberNum=memberNum+:num WHERE gid=:gid")
    suspend fun changeGroupUserNumBy(gid: Long, num: Int)
}