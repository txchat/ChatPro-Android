package com.fzm.chat.core.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.zjy.architecture.di.rootScope

/**
 * @author zhengjy
 * @since 2020/12/04
 * Description:
 */
class ChatDatabaseProvider {

    companion object {

        @Volatile
        private var sInstance: ChatDatabase? = null

        init {
            // 保证在主线程执行
            Handler(Looper.getMainLooper()).post {
                AppPreference.address.observeForever {
                    if (it.isNullOrEmpty()) {
                        // address不同时重置数据库
                        sInstance?.close()
                        sInstance = null
                    }
                }
            }
        }

        private val context = rootScope.get<Context>()

        fun provide(): ChatDatabase {
            if (sInstance == null || ChatDatabase.dbName.isEmpty()) {
                synchronized(this) {
                    if (sInstance == null) {
                        sInstance = ChatDatabase.build(context, AppPreference.ADDRESS)
                    }
                }
            }
            return sInstance!!
        }
    }
}