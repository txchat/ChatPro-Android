package com.fzm.chat.core.data.dao

import androidx.room.*
import com.fzm.chat.core.data.po.LocalAccount

/**
 * @author zhengjy
 * @since 2021/11/29
 * Description:
 */
@Dao
interface LocalAccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: LocalAccount)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(account: LocalAccount)

    @Query("SELECT * FROM local_account")
    suspend fun getAllAccounts(): List<LocalAccount>?

    @Query("SELECT * FROM local_account WHERE phone=:phone ORDER BY id DESC")
    suspend fun getAccountByPhone(phone: String?): LocalAccount?

    @Query("SELECT * FROM local_account WHERE email=:email ORDER BY id DESC")
    suspend fun getAccountByEmail(email: String?): LocalAccount?

    @Query("SELECT * FROM local_account WHERE address=:address")
    suspend fun getAccountByAddress(address: String?): LocalAccount?

    @Query("UPDATE local_account SET phone=null WHERE address=:address")
    suspend fun clearAccountPhone(address: String)

    @Query("UPDATE local_account SET email=null WHERE address=:address")
    suspend fun clearAccountEmail(address: String)

}