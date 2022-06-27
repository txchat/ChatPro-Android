package com.fzm.chat.core.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zjy.architecture.di.rootScope

/**
 * @author zhengjy
 * @since 2018/12/25
 * Description:
 */
class ListToStringConverter {

    private val gson by rootScope.inject<Gson>()

    @TypeConverter
    fun revertList(string: String?): MutableList<String> {
        if (string.isNullOrEmpty()) return mutableListOf()
        return gson.fromJson(string, object : TypeToken<List<String?>?>() {}.type)
    }

    @TypeConverter
    fun convertString(list: List<String?>?): String {
        return gson.toJson(list)
    }
}