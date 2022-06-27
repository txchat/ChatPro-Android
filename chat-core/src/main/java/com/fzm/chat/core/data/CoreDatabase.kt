package com.fzm.chat.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fzm.chat.core.data.dao.BackupKeypairDao
import com.fzm.chat.core.data.dao.LocalAccountDao
import com.fzm.chat.core.data.po.BackupKeypair
import com.fzm.chat.core.data.po.LocalAccount

/**
 * @author zhengjy
 * @since 2021/11/29
 * Description:
 */
@Database(
    entities = [
        LocalAccount::class,
        BackupKeypair::class
    ],
    version = CoreDatabase.CORE_DB_VERSION
)
abstract class CoreDatabase : RoomDatabase() {
    companion object {
        /**
         * 数据库的版本号
         */
        const val CORE_DB_VERSION = 1

        fun build(context: Context): CoreDatabase {
            return Room.databaseBuilder(context, CoreDatabase::class.java, "core.db")
                .build()
        }
    }

    abstract fun localAccountDao(): LocalAccountDao

    abstract fun backupKeypair(): BackupKeypairDao
}