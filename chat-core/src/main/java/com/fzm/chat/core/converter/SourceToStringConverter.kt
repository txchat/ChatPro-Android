package com.fzm.chat.core.converter

import androidx.room.TypeConverter
import com.fzm.chat.core.data.po.MessageSource
import com.google.gson.Gson
import com.zjy.architecture.di.rootScope

/**
 * @author zhengjy
 * @since 2021/06/17
 * Description:
 */
class SourceToStringConverter {

    private val gson by rootScope.inject<Gson>()

    @TypeConverter
    fun revertSource(source: String?): MessageSource? {
        if (source == null) return null
        return gson.fromJson(source, MessageSource::class.java)
    }

    @TypeConverter
    fun convertString(source: MessageSource?): String? {
        if (source == null) return null
        return gson.toJson(source)
    }
}