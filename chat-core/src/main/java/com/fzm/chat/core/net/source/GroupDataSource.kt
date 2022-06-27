package com.fzm.chat.core.net.source

import com.fzm.chat.core.data.bean.GroupInfoTO
import com.fzm.chat.core.data.bean.GroupUserTO
import com.fzm.chat.core.data.bean.StringList
import com.fzm.chat.core.data.po.GroupUserPO
import com.zjy.architecture.data.Result

/**
 * @author zhengjy
 * @since 2021/05/11
 * Description:
 */
interface GroupDataSource {

    /**
     * 在指定的服务器上创建群聊
     */
    suspend fun createGroup(url: String, name: String, users: List<String>): Result<GroupInfoTO>

    /**
     * 修改群头像
     */
    suspend fun editGroupAvatar(url: String?, gid: Long, avatar: String): Result<Any>

    /**
     * 修改群名
     */
    suspend fun editGroupName(url: String?, gid: Long, name: String): Result<Any>

    /**
     * 修改群名
     */
    suspend fun editGroupNames(url: String?, gid: Long, name: String, publicName: String): Result<Any>

    /**
     * 修改群内昵称
     */
    suspend fun editNicknameInGroup(url: String?, gid: Long, name: String): Result<Any>

    /**
     * 邀请群成员
     */
    suspend fun inviteMembers(url: String?, gid: Long, members: List<String>): Result<Any>

    /**
     * 删除群成员
     */
    suspend fun removeMembers(url: String?, gid: Long, members: List<String>): Result<StringList>

    /**
     * 获取某个群成员信息
     */
    suspend fun getGroupUser(url: String?, gid: Long, address: String): Result<GroupUserPO>

    /**
     * 获取某个群成员信息
     */
    suspend fun getGroupUserList(url: String?, gid: Long): Result<List<GroupUserPO>>

    /**
     * 获取群信息
     */
    suspend fun getGroupInfo(url: String?, gid: Long): Result<GroupInfoTO>

    /**
     * 获取群公开信息
     */
    suspend fun getGroupPubInfo(url: String?, gid: Long): Result<GroupInfoTO>

    /**
     * 直接进群
     */
    suspend fun joinGroup(url: String?, gid: Long, inviterId: String?): Result<Any>

    /**
     * 获取群信息
     */
    suspend fun getGroupList(url: String): Result<GroupInfoTO.Wrapper>

    /**
     * 修改群主
     */
    suspend fun changeGroupOwner(url: String?, gid: Long, address: String): Result<Any>

    /**
     * 修改群成员角色，如：管理员，普通成员等
     */
    suspend fun changeGroupUserRole(
        url: String?,
        gid: Long,
        address: String,
        role: Int
    ): Result<Any>

    /**
     * 修改群内加好友权限
     */
    suspend fun changeFriendType(url: String?, gid: Long, friendType: Int): Result<Any>

    /**
     * 修改入群方式的权限
     */
    suspend fun changeJoinType(url: String?, gid: Long, joinType: Int): Result<Any>

    /**
     * 修改禁言模式
     */
    suspend fun changeMuteType(url: String?, gid: Long, muteType: Int): Result<Any>

    /**
     * 解除禁言
     */
    suspend fun changeMuteTime(
        url: String?,
        gid: Long,
        muteTime: Long,
        members: List<String>
    ): Result<GroupUserTO.Wrapper>

    /**
     * 退出群聊
     */
    suspend fun exitGroup(url: String?, gid: Long): Result<Any>

    /**
     * 解散群聊
     */
    suspend fun disbandGroup(url: String?, gid: Long): Result<Any>
}