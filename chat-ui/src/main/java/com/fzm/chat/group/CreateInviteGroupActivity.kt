package com.fzm.chat.group

import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.repo.GroupRepository
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.databinding.ActivityCreateInviteGroupBinding
import com.fzm.chat.router.main.MainModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * @author zhengjy
 * @since 2021/05/14
 * Description:
 */
@Route(path = MainModule.CREATE_INVITE_GROUP)
class CreateInviteGroupActivity : BizActivity() {

    @JvmField
    @Autowired
    var server: String = ""

    @JvmField
    @Autowired
    var groupId: Long = 0L

    @JvmField
    @Autowired
    var selectFromOA: Boolean = false

    private val delegate by inject<LoginDelegate>()
    private val repository by inject<GroupRepository>()
    private val binding by init { ActivityCreateInviteGroupBinding.inflate(layoutInflater) }

    override val darkStatusColor = true

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
    }

    override fun initData() {
        lifecycleScope.launch {
            loading(true)
            delay(300)
            if (groupId != 0L) {
                val users = repository.getGroupUserList(null, groupId).dataOrNull()?: emptyList()
                val action = if (delegate.hasCompany()) R.id.selected_group_member_type else R.id.selected_group_member
                findNavController(R.id.fcv_container).navigate(action, bundleOf(
                    "groupId" to groupId,
                    "groupUsers" to ArrayList(users.map { it.address }),
                    "server" to server,
                    "selectFromOA" to selectFromOA
                ))
            } else {
                findNavController(R.id.fcv_container).navigate(R.id.selected_group_server)
            }
            dismiss()
        }
    }

    override fun setEvent() {
        
    }
}