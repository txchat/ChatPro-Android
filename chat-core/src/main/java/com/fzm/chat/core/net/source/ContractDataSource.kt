package com.fzm.chat.core.net.source

import com.fzm.chat.core.data.bean.ServerGroupInfo
import com.fzm.chat.core.data.bean.UserAddress
import com.fzm.chat.core.data.bean.UserResult
import com.fzm.chat.core.data.bean.ChatQuery
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2020/02/06
 * Description:合约数据源
 */
interface ContractDataSource {

    /**
     * 获取用户信息
     *
     * @param query  查询参数
     */
    suspend fun getUser(query: ChatQuery): Result<UserResult>

    /**
     * 获取好友列表
     *
     * @param query  查询参数
     */
    suspend fun getFriendList(query: ChatQuery): Result<UserAddress.Wrapper>

    /**
     * 获取黑名单列表
     *
     * @param query  查询参数
     */
    suspend fun getBlockList(query: ChatQuery): Result<UserAddress.Wrapper>

    /**
     * 获取分组列表信息
     *
     * @param query  查询参数
     */
    suspend fun getServerGroup(query: ChatQuery): Result<ServerGroupInfo.Wrapper>

    /**
     * 修改好友
     *
     * @param address   好友地址
     * @param type      操作类型，1：添加好友 2：删除好友
     * @param groups    好友分组，空数组则表示默认分组
     */
    suspend fun modifyFriend(address: List<String?>, type: Int, groups: List<String>): Result<String>

    /**
     * 修改黑名单
     *
     * @param address   目标地址
     * @param type      操作类型，1：添加黑名单 2：删除黑名单
     */
    suspend fun modifyBlock(address: List<String?>, type: Int): Result<String>

    /**
     * 修改分组列表信息
     *
     * @param groupInfo   分组信息
     */
    suspend fun modifyServerGroup(groupInfo: List<ServerGroupInfo>): Result<String>
}