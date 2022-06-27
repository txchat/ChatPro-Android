package com.fzm.chat.core.converter

import androidx.room.TypeConverter
import com.fzm.chat.core.data.model.Draft
import com.google.gson.Gson
import com.zjy.architecture.di.rootScope

/**
 * @author zhengjy
 * @since 2021/11/16
 * Description:
 */
class DraftToStringConverter {

    private val gson by rootScope.inject<Gson>()

    @TypeConverter
    fun revertDraft(draft: String?): Draft? {
        if (draft == null) return null
        return gson.fromJson(draft, Draft::class.java)
    }

    @TypeConverter
    fun convertString(draft: Draft?): String? {
        if (draft == null) return null
        return gson.toJson(draft)
    }
}