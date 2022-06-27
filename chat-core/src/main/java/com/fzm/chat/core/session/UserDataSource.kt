package com.fzm.chat.core.session

import com.fzm.chat.core.data.bean.Field
import com.fzm.chat.core.data.bean.ChatQuery
import com.fzm.chat.core.data.bean.ServerGroupInfo
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2020/12/04
 * Description:
 */
interface UserDataSource {

    /**
     * 获取用户自己的信息
     */
    suspend fun getUserInfo(address: String, publicKey: String, query: ChatQuery): Result<UserInfo>

    /**
     * 退出登录清空用户信息
     */
    suspend fun logout(address: String)

    /**
     * 设置用户信息
     */
    suspend fun setUserInfo(fields: List<Field>): Result<String>

    /**
     * 修改分组列表信息
     *
     * @param groupInfo   分组信息
     */
    suspend fun modifyServerGroup(groupInfo: List<ServerGroupInfo>): Result<String>
}