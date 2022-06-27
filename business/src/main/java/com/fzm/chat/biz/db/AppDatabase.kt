package com.fzm.chat.biz.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fzm.chat.biz.db.dao.LocalTxNoteDao
import com.fzm.chat.biz.db.dao.ServerHistoryDao
import com.fzm.chat.biz.db.po.LocalNote
import com.fzm.chat.biz.db.po.ServerHistory

/**
 * @author zhengjy
 * @since 2021/02/23
 * Description:
 */
@Database(
    entities = [
        ServerHistory::class,
        LocalNote::class
    ],
    version = AppDatabase.APP_DB_VERSION
)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        /**
         * 数据库的版本号
         */
        const val APP_DB_VERSION = 2

        fun build(context: Context): AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, "app_local.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    abstract fun serverHistoryDao(): ServerHistoryDao

    abstract fun localTxNoteDao(): LocalTxNoteDao
}