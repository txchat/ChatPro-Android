package com.fzm.chat.core.converter

import androidx.room.TypeConverter
import com.fzm.chat.core.data.bean.Server
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zjy.architecture.di.rootScope

/**
 * @author zhengjy
 * @since 2018/12/25
 * Description:
 */
class ServerListToStringConverter {

    private val gson by rootScope.inject<Gson>()

    @TypeConverter
    fun revertList(server: String?): MutableList<Server> {
        if (server.isNullOrEmpty()) return mutableListOf()
        return try {
            gson.fromJson(server, object : TypeToken<List<Server>?>() {}.type)
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    @TypeConverter
    fun convertString(list: List<Server>?): String {
        if (list.isNullOrEmpty()) return ""
        return gson.toJson(list)
    }
}