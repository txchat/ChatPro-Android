package com.fzm.chat.core.data.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.room.*
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.data.po.FriendUserPO
import com.fzm.chat.core.data.po.isBlock
import com.fzm.chat.core.data.po.toPO

/**
 * @author zhengjy
 * @since 2020/12/22
 * Description:
 */
@Dao
interface FriendUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: FriendUserPO)

    suspend fun insert(user: FriendUser) = insert(user.toPO())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: List<FriendUserPO>)

    @Query("""
        SELECT f.*, c.name as teamName
        FROM friend_user f
        LEFT JOIN company_user c
        ON f.address=c.id
    """)
    fun getAllUsers(): LiveData<List<FriendUser>>

    @Query("SELECT * FROM friend_user")
    suspend fun getAllUsersSuspend(): List<FriendUserPO>

    @Query("""
        SELECT f.*, c.name as teamName
        FROM friend_user f
        LEFT JOIN company_user c
        ON f.address=c.id
        WHERE address=:address
    """)
    suspend fun getFriendUser(address: String): FriendUser?

    @Query("""
        SELECT f.*, c.name as teamName
        FROM friend_user f
        LEFT JOIN company_user c
        ON f.address=c.id
        WHERE address=:address
    """)
    fun getFriendLive(address: String): LiveData<FriendUser>

    @Query("DELETE FROM friend_user WHERE address=:address")
    suspend fun deleteFriendUser(address: String)

    @Query("""
        SELECT f.*, c.name as teamName
        FROM friend_user f
        LEFT JOIN company_user c
        ON f.address=c.id
        WHERE flag & :flag=:flag
    """)
    fun getFriendUserList(flag: Int): LiveData<List<FriendUser>>

    @Query("""
        SELECT f.*, c.name as teamName
        FROM friend_user f
        LEFT JOIN company_user c
        ON f.address=c.id
        WHERE flag & :flag=:flag
    """)
    suspend fun getFriendListSuspend(flag: Int): List<FriendUser>

    suspend fun getFriendList(): List<FriendUser> {
        return getFriendListSuspend(Contact.RELATION).filter { !it.isBlock }
    }

    @Query("UPDATE friend_user SET flag=flag & ~:flag WHERE address=:address")
    suspend fun deleteFlag(address: String, flag: Int)

    @Query("UPDATE friend_user SET flag=flag | :flag WHERE address=:address")
    suspend fun addFlag(address: String, flag: Int)

    @Query("UPDATE friend_user SET remark=:remark WHERE address=:address")
    suspend fun updateRemark(address: String, remark: String)

    @Query("""
        SELECT f.*, c.name as teamName
        FROM friend_user f 
        LEFT JOIN company_user c
        ON f.address=c.id
        WHERE flag & :flag=:flag AND (teamName LIKE '%'||:keywords||'%' OR nickname LIKE '%'||:keywords||'%' 
        OR remark LIKE '%'||:keywords||'%' OR address LIKE '%'||:keywords||'%' 
        OR searchKey LIKE '% '||:keywords||'%' OR teamName LIKE '% '||:keywords||'%' ESCAPE '/')
    """)
    suspend fun searchFriendsByFlag(keywords: String?, flag: Int): List<FriendUser>

    suspend fun searchUsers(keywords: String?, flag: Int): List<FriendUser> {
        return if (keywords.isNullOrEmpty()) {
            if (flag == Contact.RELATION) {
                getFriendListSuspend(flag).filter { !it.isBlock }
            } else {
                getFriendListSuspend(flag)
            }
        } else {
            if (flag == Contact.RELATION) {
                searchFriendsByFlag(keywords, flag).filter { !it.isBlock }
            } else {
                searchFriendsByFlag(keywords, flag)
            }
        }
    }

    @Query("""
        SELECT f.*, c.name as teamName
        FROM friend_user f 
        LEFT JOIN company_user c
        ON f.address=c.id
        WHERE flag & :flag=:flag AND (teamName LIKE '%'||:keywords||'%' OR nickname LIKE '%'||:keywords||'%' 
        OR remark LIKE '%'||:keywords||'%' OR address LIKE '%'||:keywords||'%' 
        OR searchKey LIKE '% '||:keywords||'%' OR teamName LIKE '% '||:keywords||'%' ESCAPE '/')
    """)
    fun searchFriendsByFlagLive(keywords: String?, flag: Int): LiveData<List<FriendUser>>

    fun searchUsersLive(keywords: String?, flag: Int): LiveData<List<FriendUser>> {
        return if (keywords.isNullOrEmpty()) {
            if (flag == Contact.RELATION) {
                getFriendUserList(flag).map { list ->
                    list.filter { !it.isBlock }
                }
            } else {
                getFriendUserList(flag)
            }
        } else {
            if (flag == Contact.RELATION) {
                searchFriendsByFlagLive(keywords, flag).map { list ->
                    list.filter { !it.isBlock }
                }
            } else {
                searchFriendsByFlagLive(keywords, flag)
            }
        }
    }
}