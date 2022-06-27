package com.fzm.chat.core.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2019/09/17
 * Description:
 */
@Entity(tableName = "search_history")
data class SearchHistory constructor(
        @PrimaryKey
        var keywords: String,
        var searchTime: Long
) : Serializable