package com.fzm.chat.core.data.bean

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/22
 * Description:
 */
@Parcelize
data class ServerGroupInfo(
    val id: String,
    /**
     * 1->新增；2->删除；3->修改
     */
    val type: Int,
    val name: String,
    val value: String,
    var selected: Boolean = false,
) : Parcelable {

    /**
     * 服务器连接状态
     */
    @IgnoredOnParcel
    var state = 0

    data class Wrapper(
        var groups: List<ServerGroupInfo>
    ) : Serializable
}