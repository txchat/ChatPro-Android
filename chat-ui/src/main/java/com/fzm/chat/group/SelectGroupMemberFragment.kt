package com.fzm.chat.group

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.bus.BusEvent
import com.fzm.chat.biz.bus.LiveDataBus
import com.fzm.chat.biz.bus.event.ChangeTabEvent
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.biz.widget.ChatSearchView
import com.fzm.chat.contact.SelectFriendFragment
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.po.FriendUser
import com.fzm.chat.databinding.FragmentSelectGroupMemberBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.KeyboardUtils
import com.zjy.architecture.util.other.BarUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/05/14
 * Description:
 */
class SelectGroupMemberFragment : BizFragment() {

    private var server = ""
    private var gid: Long = 0L
    private var groupUsers: ArrayList<String>? = null

    private val viewModel by viewModel<GroupViewModel>()
    private val binding by init<FragmentSelectGroupMemberBinding>()
    private lateinit var fragment: SelectFriendFragment

    private lateinit var selectAdapter: BaseQuickAdapter<FriendUser, BaseViewHolder>

    private val selectFriends = mutableListOf<FriendUser>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        server = arguments?.getString("server") ?: ""
        gid = arguments?.getLong("groupId", 0L) ?: 0L
        groupUsers = arguments?.getSerializable("groupUsers") as? ArrayList<String>?
        if (gid != 0L) {
            binding.ctbTitle.setTitle(getString(R.string.chat_title_invite_group_members))
            binding.tvSubmit.setText(R.string.chat_action_invite)
        } else {
            binding.ctbTitle.setTitle(getString(R.string.chat_title_create_group))
            binding.tvSubmit.setText(R.string.biz_skip)
        }
        binding.rvSelected.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        selectAdapter = object : BaseQuickAdapter<FriendUser, BaseViewHolder>(R.layout.item_select_contact, selectFriends) {
            override fun convert(holder: BaseViewHolder, item: FriendUser) {
                holder.getView<ChatAvatarView>(R.id.iv_avatar).load(item.getDisplayImage(), R.mipmap.default_avatar_round)
                holder.itemView.setOnClickListener {
                    val index = selectFriends.indexOf(item)
                    if (index >= 0) {
                        selectFriends.removeAt(index)
                        selectAdapter.notifyItemRemoved(index)
                        binding.tvSelectedNum.text = "${selectFriends.size}"
                        fragment.clearCheck(item)
                    }
                    if (selectFriends.isEmpty()) {
                        binding.tvSubmit.setText(R.string.biz_skip)
                    } else {
                        binding.tvSubmit.setText(R.string.chat_action_create_group)
                    }
                }
            }
        }
        binding.rvSelected.adapter = selectAdapter
        viewModel.loading.observe(viewLifecycleOwner) { setupLoading(it) }
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
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            if (!binding.chatSearch.onBackPressed()) {
                if (!findNavController().popBackStack()) {
                    activity?.finish()
                }
            }
        }

        fragment = SelectFriendFragment.create(groupUsers)
        fragment.setOnSelectListener { user, checked ->
            if (checked) {
                selectFriends.add(user)
                selectAdapter.notifyItemInserted(selectFriends.size - 1)
                binding.rvSelected.scrollToPosition(selectFriends.size - 1)
            } else {
                val index = selectFriends.indexOf(user)
                if (index >= 0) {
                    selectFriends.removeAt(index)
                    selectAdapter.notifyItemRemoved(index)
                }
            }
            binding.tvSelectedNum.text = "${selectFriends.size}"
            if (gid == 0L) {
                if (selectFriends.isEmpty()) {
                    binding.tvSubmit.setText(R.string.biz_skip)
                } else {
                    binding.tvSubmit.setText(R.string.chat_action_create_group)
                }
            }
        }
        childFragmentManager.commit {
            add(R.id.fcv_container, fragment)
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { activity?.onBackPressed() }
        binding.ctbTitle.setOnRightClickListener {
            binding.chatSearch.expand()
            binding.chatSearch.postDelayed({ KeyboardUtils.showKeyboard(binding.chatSearch.getFocusView()) }, 100)
        }
        binding.chatSearch.setOnSearchCancelListener(object : ChatSearchView.OnSearchCancelListener {
            override fun onSearchCancel() {
                KeyboardUtils.hideKeyboard(binding.chatSearch.getFocusView())
                binding.chatSearch.setText(null)
                binding.chatSearch.reduce()
            }
        })
        binding.chatSearch.setOnTextChangeListener(object : ChatSearchView.OnTextChangeListener {
            override fun onTextChange(s: String) {
                fragment.setSearchKey(s)
            }
        })
        binding.tvSubmit.setOnClickListener {
            if (gid != 0L) {
                if (selectFriends.isEmpty()) {
                    toast(R.string.chat_tips_select_invite_users)
                    return@setOnClickListener
                }
                viewModel.inviteGroupMembers(server, gid, selectFriends.map { it.address }.distinct())
            } else {
                val username = viewModel.current.value?.nickname
                viewModel.createGroup(
                    server,
                    getString(R.string.chat_tips_somebody_create_group, username),
                    selectFriends.map { it.address }.distinct()
                )
            }
        }
    }
}