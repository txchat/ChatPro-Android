package com.fzm.chat.group

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.bus.BusEvent
import com.fzm.chat.biz.bus.LiveDataBus
import com.fzm.chat.biz.bus.event.ChangeTabEvent
import com.fzm.chat.contract.CompanyMemberSelector
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.databinding.FragmentSelectMemberTypeBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.oa.OAModule
import com.fzm.chat.router.oa.OAService
import com.fzm.chat.router.route
import com.zjy.architecture.ext.setVisible
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.other.BarUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectGroupMemberTypeFragment : BizFragment(), View.OnClickListener {

    companion object {
        const val ACTION_CREATE_GROUP = "createGroup"
        const val ACTION_INVITE_GROUP = "inviteGroup"

        // 组织机构多选
        const val TYPE_MULTIPLE = "multiple"
    }

    private val oaService by route<OAService>(OAModule.SERVICE)
    private val viewModel by viewModel<GroupViewModel>()
    private val binding by init<FragmentSelectMemberTypeBinding>()

    override val root: View
        get() = binding.root

    private lateinit var groupMemberLauncher: ActivityResultLauncher<String>

    private var server = ""
    private var gid: Long = 0L
    private var groupUsers: ArrayList<String>? = null
    private var selectFromOA: Boolean = false

    override fun initView(view: View, savedInstanceState: Bundle?) {
        BarUtils.setStatusBarColor(requireActivity(), ContextCompat.getColor(requireContext(), R.color.biz_color_primary_dark), 0)
        BarUtils.setStatusBarLightMode(requireActivity(), true)

        binding.llInviteFromFriends.setOnClickListener(this)
        binding.llInviteFromOa.setOnClickListener(this)

        viewModel.createResult.observe(viewLifecycleOwner) {
            ARouter.getInstance().build(MainModule.CHAT)
                .withString("address", it.gid.toString())
                .withInt("channelType", ChatConst.GROUP_CHANNEL)
                .navigation(requireContext())
            dismiss()
            activity?.finish()
            LiveDataBus.of(BusEvent::class.java).changeTab().setValue(ChangeTabEvent(0, 1))
        }
        viewModel.inviteResult.observe(viewLifecycleOwner) {
            toast(R.string.tips_invite_group_members_success)
            dismiss()
            activity?.setResult(Activity.RESULT_OK)
            activity?.finish()
        }
    }


    override fun initData() {
        server = arguments?.getString("server") ?: ""
        gid = arguments?.getLong("groupId", 0L) ?: 0L
        groupUsers = arguments?.getSerializable("groupUsers") as? ArrayList<String>?
        selectFromOA = arguments?.getBoolean("selectFromOA") ?: false

        if (gid != 0L) {
            binding.ctbTitle.setTitle(getString(R.string.chat_title_invite_members_type))
        } else {
            binding.ctbTitle.setTitle(getString(R.string.chat_title_create_group))
        }
        binding.llInviteFromFriends.setVisible(!selectFromOA)

        groupMemberLauncher = registerForActivityResult(CompanyMemberSelector()) { selectUsers ->
            selectUsers?.let {
                if (gid != 0L) {
                    viewModel.inviteGroupMembers(server, gid, it)
                } else {
                    val username = viewModel.current.value?.nickname
                    viewModel.createGroup(server, getString(R.string.chat_tips_somebody_create_group, username), it)
                }
            }
        }
        if (selectFromOA) {
            selectFromOA()
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { activity?.onBackPressed() }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ll_invite_from_friends -> {
                findNavController().navigate(
                    R.id.select_group_member_type,
                    bundleOf("server" to server, "groupId" to gid, "groupUsers" to groupUsers)
                )
            }
            R.id.ll_invite_from_oa -> selectFromOA()
        }
    }

    private fun selectFromOA() {
        oaService?.let { service ->
            val action = if (gid != 0L) ACTION_INVITE_GROUP else ACTION_CREATE_GROUP
            val gid = if ( gid != 0L) gid.toString() else ""
            val map = mapOf(
                "action" to action,
                "type" to TYPE_MULTIPLE,
                "gid" to gid
            )
            groupMemberLauncher.launch(service.selectCompanyUserUrl(map))
        }
    }
}