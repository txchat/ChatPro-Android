package com.fzm.chat.core.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fzm.chat.core.data.bean.CompanyUserBO
import com.fzm.chat.core.data.po.CompanyUserPO
import com.fzm.chat.router.oa.CompanyUser

/**
 * @author zhengjy
 * @since 2021/09/05
 * Description:
 */
@Dao
interface CompanyUserDao {

    @Query("SELECT * FROM company_user WHERE id=:id")
    fun getCompanyUserLive(id: String): LiveData<CompanyUser>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: CompanyUserPO)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: List<CompanyUserPO>)

    @Query("DELETE FROM company_user WHERE id=:id")
    suspend fun deleteByAddress(id: String): Int

    @Query("""
        SELECT u.*, c.id as team_id, c.avatar as team_avatar, c.name as team_name, c.avatar as team_avatar, 
            c.description as team_description, c.imServer as team_imServer, c.nodeServer as team_nodeServer,
            c.oaServer as team_oaServer, c.rootDepId as team_rootDepId
        FROM company_user u 
        INNER JOIN company c 
        ON u.entId=c.id 
        WHERE u.id=:id
    """)
    fun getCompanyUserInfo(id: String): LiveData<CompanyUserBO?>
}