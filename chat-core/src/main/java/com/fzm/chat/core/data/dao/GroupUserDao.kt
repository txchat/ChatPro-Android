package com.fzm.chat.core.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.data.po.GroupUserPO

/**
 * @author zhengjy
 * @since 2021/05/13
 * Description:
 */
@Dao
interface GroupUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: GroupUserPO)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: List<GroupUserPO>)

    @Query(
        """
            SELECT g.*, f.nickname as name, f.avatar, f.remark, c.name as teamName
            FROM group_user g 
            LEFT JOIN friend_user f 
            ON g.address=f.address 
            LEFT JOIN company_user c
            ON g.address=c.id
            WHERE gid=:gid AND g.address=:address
            """
    )
    suspend fun getGroupUser(gid: Long, address: String): GroupUser?

    @Query(
        """
            SELECT g.*, f.nickname as name, f.avatar, f.remark, c.name as teamName
            FROM group_user g 
            LEFT JOIN friend_user f 
            ON g.address=f.address 
            LEFT JOIN company_user c
            ON g.address=c.id
            WHERE gid=:gid AND g.groupUserFlag & :flag=:flag
            """
    )
    fun getGroupUserList(gid: Long, flag: Int = Contact.RELATION): LiveData<List<GroupUser>>

    @Query(
        """
            SELECT g.*, f.nickname as name, f.avatar, f.remark, c.name as teamName
            FROM group_user g 
            LEFT JOIN friend_user f 
            ON g.address=f.address 
            LEFT JOIN company_user c
            ON g.address=c.id
            WHERE gid=:gid AND g.muteTime > :time AND g.groupUserFlag & :flag=:flag
            """
    )
    fun getGroupUserListByMuteTime(gid: Long, time: Long, flag: Int = Contact.RELATION): LiveData<List<GroupUser>>

    @Query(
        """
            SELECT g.*, f.nickname as name, f.avatar, f.remark, c.name as teamName
            FROM group_user g 
            LEFT JOIN friend_user f 
            ON g.address=f.address 
            LEFT JOIN company_user c
            ON g.address=c.id
            WHERE gid=:gid AND g.role >= :role AND g.groupUserFlag & :flag=:flag
            """
    )
    fun getGroupUserListByRole(gid: Long, role: Int, flag: Int = Contact.RELATION): LiveData<List<GroupUser>>

    @Query(
        """
            SELECT g.*, f.nickname as name, f.avatar, f.remark, c.name as teamName
            FROM group_user g 
            LEFT JOIN friend_user f 
            ON g.address=f.address 
            LEFT JOIN company_user c
            ON g.address=c.id
            WHERE gid=:gid AND g.groupUserFlag & :flag=:flag AND (g.nickname LIKE '%'||:keywords||'%' 
            OR g.searchKey LIKE '%'||:keywords||'%' OR f.remark LIKE '%'||:keywords||'%' 
            OR f.nickname LIKE '%'||:keywords||'%' OR f.searchKey LIKE '%'||:keywords||'%' 
            OR teamName LIKE '%'||:keywords||'%' ESCAPE '/')
            """
    )
    fun getGroupUserListByKeywords(gid: Long, keywords: String, flag: Int = Contact.RELATION): LiveData<List<GroupUser>>

    @Query("DELETE FROM group_user WHERE gid=:gid")
    suspend fun deleteGroupUsersByGroup(gid: Long)

    @Deprecated(
        message = "本地群成员信息不建议删除",
        replaceWith = ReplaceWith("this.disableGroupUsers(Long, String)")
    )
    @Query("DELETE FROM group_user WHERE gid=:gid AND address=:address")
    suspend fun deleteGroupUsers(gid: Long, address: String)

    suspend fun disableGroupUsers(gid: Long, address: String) {
        deleteFlag(gid, address, Contact.RELATION)
    }

    @Query("UPDATE group_user SET groupUserFlag=groupUserFlag & ~:flag WHERE gid=:gid AND address=:address")
    suspend fun deleteFlag(gid: Long, address: String, flag: Int)

    @Query("UPDATE group_user SET nickname=:nickname WHERE gid=:gid AND address=:address")
    suspend fun editNickname(gid: Long, address: String, nickname: String)

    @Query("UPDATE group_user SET role=:role WHERE gid=:gid AND address=:address")
    suspend fun changeGroupUserRole(gid: Long, address: String, role: Int)

    @Query("UPDATE group_user SET muteTime=:muteTime WHERE gid=:gid AND address=:address")
    suspend fun changeMuteTime(gid: Long, address: String, muteTime: Long)
}