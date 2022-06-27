package com.fzm.chat.core.converter

import androidx.room.TypeConverter
import com.fzm.chat.core.data.po.MessageContent
import com.google.gson.Gson
import com.zjy.architecture.di.rootScope

/**
 * @author zhengjy
 * @since 2020/12/28
 * Description:
 */
class ContentToStringConverter {

    private val gson by rootScope.inject<Gson>()

    @TypeConverter
    fun revertContent(msg: String): MessageContent {
        return gson.fromJson(msg, MessageContent::class.java)
    }

    @TypeConverter
    fun convertString(content: MessageContent): String {
        return gson.toJson(content)
    }
}