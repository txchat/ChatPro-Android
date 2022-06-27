package com.fzm.chat.core.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.fzm.chat.core.data.bean.Server
import com.fzm.chat.core.session.UserInfo

/**
 * @author zhengjy
 * @since 2020/08/06
 * Description:
 */
@Dao
interface UserInfoDao {

    @Query("SELECT * FROM user_info WHERE address=:address")
    fun getUserInfo(address: String): LiveData<UserInfo>

    @Query("SELECT count(1) FROM user_info WHERE address=:address")
    suspend fun hasUserInfo(address: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: UserInfo)

    @Query("INSERT INTO user_info (address, nickname, avatar, publicKey, servers, searchKey, chainAddress) VALUES (:address, :nickname, :avatar, :publicKey, :servers, :searchKey, :chainAddress)")
    suspend fun _insertWithoutPhone(address: String, nickname: String, avatar: String, publicKey: String?, servers: List<Server>, searchKey: String, chainAddress: Map<String, String>)

    @Query("UPDATE user_info SET nickname=:nickname, avatar=:avatar, publicKey=:publicKey, servers=:servers, searchKey=:searchKey, chainAddress=:chainAddress WHERE address=:address")
    suspend fun _updateWithoutPhone(address: String, nickname: String, avatar: String, publicKey: String?, servers: List<Server>, searchKey: String, chainAddress: Map<String, String>)

    @Transaction
    suspend fun insertWithoutPhone(address: String, nickname: String, avatar: String, publicKey: String?, servers: List<Server>, searchKey: String, chainAddress: Map<String, String>) {
        val user = hasUserInfo(address)
        if (user > 0) {
            _updateWithoutPhone(address, nickname, avatar, publicKey, servers, searchKey, chainAddress)
        } else {
            _insertWithoutPhone(address, nickname, avatar, publicKey, servers, searchKey, chainAddress)
        }
    }

    @Query("DELETE FROM user_info WHERE address=:address")
    suspend fun deleteByAddress(address: String): Int
}