package com.fzm.chat.core.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fzm.chat.core.data.po.CompanyInfoPO
import com.fzm.chat.router.oa.CompanyInfo

/**
 * @author zhengjy
 * @since 2021/09/05
 * Description:
 */
@Dao
interface CompanyDao {

    @Query("SELECT * FROM company WHERE id=:id")
    fun getCompanyInfo(id: String): LiveData<CompanyInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: CompanyInfoPO)

    @Query("DELETE FROM company WHERE id=:id")
    suspend fun delete(id: String): Int
}