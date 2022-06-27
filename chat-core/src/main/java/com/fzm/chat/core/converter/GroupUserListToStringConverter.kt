package com.fzm.chat.core.converter

import androidx.room.TypeConverter
import com.fzm.chat.core.data.bean.GroupUserTO
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zjy.architecture.di.rootScope

/**
 * @author zhengjy
 * @since 2018/12/25
 * Description:
 */
class GroupUserListToStringConverter {

    private val gson by rootScope.inject<Gson>()

    @TypeConverter
    fun revertList(users: String?): MutableList<GroupUserTO>? {
        if (users == null) return null
        return gson.fromJson(users, object : TypeToken<List<GroupUserTO?>?>() {}.type)
    }

    @TypeConverter
    fun convertString(list: List<GroupUserTO?>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }
}