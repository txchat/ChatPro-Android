package com.fzm.chat.core.converter

import androidx.room.TypeConverter
import com.fzm.chat.core.data.bean.Server
import com.google.gson.Gson
import com.zjy.architecture.di.rootScope

/**
 * @author zhengjy
 * @since 2021/05/12
 * Description:
 */
class ServerToStringConverter {

    private val gson by rootScope.inject<Gson>()

    @TypeConverter
    fun revertServer(server: String?): Server? {
        if (server == null) return null
        return gson.fromJson(server, Server::class.java)
    }

    @TypeConverter
    fun convertString(server: Server?): String? {
        if (server == null) return null
        return gson.toJson(server)
    }
}