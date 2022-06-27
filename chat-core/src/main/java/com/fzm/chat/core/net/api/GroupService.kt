package com.fzm.chat.core.net.api

import com.fzm.chat.core.data.bean.GroupInfoTO
import com.fzm.chat.core.data.bean.GroupUserTO
import com.fzm.chat.core.data.bean.StringList
import com.zjy.architecture.net.HttpResult
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * @author zhengjy
 * @since 2021/05/11
 * Description:
 */
@JvmSuppressWildcards
interface GroupService {

    companion object {

        private const val PREFIX = "/group/app"

        /**
         * 创建群聊
         */
        const val API_CREATE_GROUP = "$PREFIX/create-group"

        /**
         * 群头像，群名，群内昵称
         */
        const val API_EDIT_GROUP_NAME = "$PREFIX/name"
        const val API_EDIT_GROUP_AVATAR = "$PREFIX/avatar"
        const val API_CHANGE_OWNER = "$PREFIX/change-owner"
        const val API_EDIT_NICKNAME_IN_GROUP = "$PREFIX/member/name"

        /**
         * 邀请移除群成员
         */
        const val API_INVITE_MEMBERS = "$PREFIX/invite-group-members"
        const val API_REMOVE_MEMBERS = "$PREFIX/group-remove"

        /**
         * 群信息
         */
        const val API_GROUP_LIST = "$PREFIX/group-list"
        const val API_GROUP_INFO = "$PREFIX/group-info"
        const val API_GROUP_PUB_INFO = "$PREFIX/group-pub-info"

        /**
         * 群成员信息
         */
        const val API_GROUP_USER_LIST = "$PREFIX/group-member-list"
        const val API_GROUP_USER_INFO = "$PREFIX/group-member-info"

        /**
         * 群操作
         */
        const val API_GROUP_ROLE = "$PREFIX/member/type"
        const val API_MUTE_TIME = "$PREFIX/member/muteTime"
        const val API_FRIEND_TYPE = "$PREFIX/friendType"
        const val API_JOIN_TYPE = "$PREFIX/joinType"
        const val API_MUTE_TYPE = "$PREFIX/muteType"
        const val API_GROUP_EXIT = "$PREFIX/group-exit"
        const val API_GROUP_DISBAND = "$PREFIX/group-disband"
        const val API_JOIN_GROUP = "$PREFIX/join-group"
    }

    @POST
    suspend fun createGroup(@Url url: String, @Body map: Map<String, Any>): HttpResult<GroupInfoTO>

    @POST
    suspend fun editGroupAvatar(@Url url: String, @Body map: Map<String, Any>): HttpResult<Any>

    @POST
    suspend fun editGroupName(@Url url: String, @Body map: Map<String, Any>): HttpResult<Any>

    @POST
    suspend fun editNicknameInGroup(@Url url: String, @Body map: Map<String, Any>): HttpResult<Any>

    @POST
    suspend fun inviteMembers(@Url url: String, @Body map: Map<String, Any>): HttpResult<Any>

    @POST
    suspend fun removeMembers(@Url url: String, @Body map: Map<String, Any>): HttpResult<StringList>

    @POST
    suspend fun getGroupUser(@Url url: String, @Body map: Map<String, Any>): HttpResult<GroupUserTO>

    @POST
    suspend fun getGroupUserList(@Url url: String, @Body map: Map<String, Any>): HttpResult<GroupUserTO.Wrapper>

    @POST
    suspend fun getGroupInfo(@Url url: String, @Body map: Map<String, Any>): HttpResult<GroupInfoTO>

    @POST
    suspend fun getGroupPubInfo(@Url url: String, @Body map: Map<String, Any>): HttpResult<GroupInfoTO>

    @POST
    suspend fun getGroupList(@Url url: String): HttpResult<GroupInfoTO.Wrapper>

    @POST
    suspend fun changeGroupOwner(@Url url: String, @Body map: Map<String, Any>): HttpResult<Any>

    @POST
    suspend fun changeGroupUserRole(@Url url: String, @Body map: Map<String, Any>): HttpResult<Any>

    @POST
    suspend fun changeFriendType(@Url url: String, @Body map: Map<String, Any>): HttpResult<Any>

    @POST
    suspend fun changeJoinType(@Url url: String, @Body map: Map<String, Any>): HttpResult<Any>

    @POST
    suspend fun changeMuteType(@Url url: String, @Body map: Map<String, Any>): HttpResult<Any>

    @POST
    suspend fun changeMuteTime(@Url url: String, @Body map: Map<String, Any>): HttpResult<GroupUserTO.Wrapper>

    @POST
    suspend fun exitGroup(@Url url: String, @Body map: Map<String, Any>): HttpResult<Any>

    @POST
    suspend fun disbandGroup(@Url url: String, @Body map: Map<String, Any>): HttpResult<Any>
}