package com.fzm.chat.core.converter

import androidx.room.TypeConverter
import com.fzm.chat.core.data.bean.GroupUserTO
import com.google.gson.Gson
import com.zjy.architecture.di.rootScope

/**
 * @author zhengjy
 * @since 2021/05/19
 * Description:
 */
class GroupUserToString {
    private val gson by rootScope.inject<Gson>()

    @TypeConverter
    fun revertContent(msg: String): GroupUserTO? {
        return gson.fromJson(msg, GroupUserTO::class.java)
    }

    @TypeConverter
    fun convertString(content: GroupUserTO?): String {
        return gson.toJson(content)
    }
}