package com.fzm.chat.contact

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.core.data.po.GroupInfo
import com.fzm.chat.core.repo.ContractRepository
import com.fzm.chat.core.repo.GroupRepository
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.isBTYAddress
import com.fzm.chat.router.oa.CompanyUser
import com.fzm.chat.router.oa.OAModule
import com.fzm.chat.router.oa.OAService
import com.fzm.chat.router.route
import com.zjy.architecture.data.Result
import com.zjy.architecture.mvvm.LoadingViewModel
import com.zjy.architecture.mvvm.request
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2020/12/22
 * Description:
 */
class ContactViewModel(
    private val repository: ContractRepository,
    private val groupRepo: GroupRepository,
    private val manager: ContactManager,
    delegate: LoginDelegate
) : LoadingViewModel(), LoginDelegate by delegate {

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val oaService by route<OAService>(OAModule.SERVICE)

    private val _userInfo by lazy { MutableLiveData<FriendUser>() }
    val userInfo: LiveData<FriendUser>
        get() = _userInfo

    private val _companyUser by lazy { MutableLiveData<CompanyUser>() }
    val companyUserInfo: LiveData<CompanyUser>
        get() = _companyUser

    private val _groupRelativeInfo by lazy { MutableLiveData<Pair<GroupInfo, GroupUser>>() }
    val groupRelativeInfo: LiveData<Pair<GroupInfo, GroupUser>>
        get() = _groupRelativeInfo

    private val _muteResult by lazy { MutableLiveData<Any>() }
    val muteResult: LiveData<Any>
        get() = _muteResult

    private val _modifyGroup by lazy { MutableLiveData<Unit>() }
    val modifyGroup: LiveData<Unit>
        get() = _modifyGroup

    private val _addFriendResult by lazy { MutableLiveData<Unit>() }
    val addFriendResult: LiveData<Unit>
        get() = _addFriendResult

    private val _deleteFriendResult by lazy { MutableLiveData<Unit>() }
    val deleteFriendResult: LiveData<Unit>
        get() = _deleteFriendResult

    private val _blockUserResult by lazy { MutableLiveData<Unit>() }
    val blockUserResult: LiveData<Unit>
        get() = _blockUserResult

    private val _unBlockUserResult by lazy { MutableLiveData<Unit>() }
    val unBlockUserResult: LiveData<Unit>
        get() = _unBlockUserResult

    fun getFriendList() {
        request<Unit> {
            onRequest {
                repository.getFriendList()
                Result.Success(Unit)
            }
        }
    }

    fun getBlockList() {
        request<Unit> {
            onRequest {
                repository.getBlockList()
                Result.Success(Unit)
            }
        }
    }

    fun getUser(address: String) {
        if (address.isBTYAddress()) {
            request<FriendUser>(false) {
                onRequest { repository.getFriendUser(address, 0, true) }
                onSuccess { _userInfo.value = it }
            }
        }
        launch {
            _companyUser.value = oaService?.getCompanyUser(address)
        }
    }

    fun getGroupRelativeInfo(gid: Long, address: String) {
        launch {
            val groupInfo = manager.getGroupInfo(gid.toString())
            val groupUser = manager.getGroupUserInfo(gid.toString(), address) as GroupUser
            _groupRelativeInfo.value = groupInfo to groupUser
            groupRepo.getGroupUser(null, gid, address).dataOrNull()?.also {
                _groupRelativeInfo.value = groupInfo to manager.getGroupUserInfo(gid.toString(), address) as GroupUser
            }
        }
    }

    fun muteUser(gid: Long, muteTime: Long, address: String) {
        request<Any> {
            onRequest {
                groupRepo.changeMuteTime(null, gid, muteTime, listOf(address))
            }
            onSuccess {
                _muteResult.value = it
            }
        }
    }

    fun addFriend(address: String, groups: List<String> = emptyList()) {
        request<String> {
            onRequest { repository.addFriends(listOf(address), groups) }
            onSuccess {
                if (groups.isEmpty()) {
                    _addFriendResult.value = Unit
                } else {
                    _modifyGroup.value = Unit
                }
            }
        }
    }

    fun deleteFriend(address: String) {
        request<String> {
            onRequest { repository.deleteFriends(listOf(address)) }
            onSuccess { _deleteFriendResult.value = Unit }
        }
    }

    fun blockUser(address: String) {
        request<String> {
            onRequest { repository.blockUser(listOf(address)) }
            onSuccess { _blockUserResult.value = Unit }
        }
    }

    fun unBlockUser(address: String) {
        request<String> {
            onRequest { repository.unBlockUser(listOf(address)) }
            onSuccess { _unBlockUserResult.value = Unit }
        }
    }

    fun changeStickTop(id: String, stickTop: Boolean) {
        launch {
            if (stickTop) {
                database.friendUserDao().addFlag(id, Contact.STICK_TOP)
            } else {
                database.friendUserDao().deleteFlag(id, Contact.STICK_TOP)
            }
        }
    }

    fun changeNoDisturb(id: String, noDisturb: Boolean) {
        launch {
            if (noDisturb) {
                database.friendUserDao().addFlag(id, Contact.NO_DISTURB)
            } else {
                database.friendUserDao().deleteFlag(id, Contact.NO_DISTURB)
            }
        }
    }
}