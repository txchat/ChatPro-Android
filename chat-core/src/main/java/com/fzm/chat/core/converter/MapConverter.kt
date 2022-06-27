package com.fzm.chat.core.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zjy.architecture.di.rootScope

/**
 * @author zhengjy
 * @since 2022/02/22
 * Description:
 */
class MapConverter {

    private val gson by rootScope.inject<Gson>()

    @TypeConverter
    fun revertMap(str: String?): Map<String, String> {
        if (str.isNullOrEmpty()) return mutableMapOf()
        return gson.fromJson(str, object : TypeToken<Map<String, String>>() {}.type)
    }

    @TypeConverter
    fun convertString(server: Map<String, String>): String {
        return gson.toJson(server) ?: ""
    }
}