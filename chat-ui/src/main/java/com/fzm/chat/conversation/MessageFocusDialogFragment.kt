package com.fzm.chat.conversation

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.data.po.MessageFocusUser
import com.fzm.chat.databinding.DialogMessageFocusBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.widget.divider.RecyclerViewDivider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zjy.architecture.ext.format
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.singleClick
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * @author zhengjy
 * @since 2021/09/25
 * Description:
 */
class MessageFocusDialogFragment : BottomSheetDialogFragment() {

    companion object {
        fun create(gid: Long, role: Int) : MessageFocusDialogFragment {
            return MessageFocusDialogFragment().apply {
                arguments = bundleOf("gid" to gid, "role" to role)
            }
        }
    }

    private val manager by inject<ContactManager>()
    private lateinit var mAdapter: BaseQuickAdapter<MessageFocusUser, BaseViewHolder>

    private var gid: Long = 0L
    private var role: Int = GroupUser.LEVEL_USER

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private lateinit var binding: DialogMessageFocusBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog)
        gid = arguments?.getLong("gid") ?: 0L
        role = arguments?.getInt("role") ?: GroupUser.LEVEL_USER
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogMessageFocusBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvCancel.setOnClickListener { dismiss() }
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.addItemDecoration(
            RecyclerViewDivider(requireContext(),
            ContextCompat.getColor(requireContext(), R.color.biz_color_divider), 0.5f, LinearLayoutManager.VERTICAL)
        )
        mAdapter = object : BaseQuickAdapter<MessageFocusUser, BaseViewHolder>(R.layout.item_message_focus_record, null) {
            override fun convert(holder: BaseViewHolder, item: MessageFocusUser) {
                holder.getView<ChatAvatarView>(R.id.iv_avatar)
                    .load(item.contact?.getDisplayImage(), R.mipmap.default_avatar_round)
                holder.setText(R.id.tv_name, item.contact?.getDisplayName())
                holder.setText(R.id.time, item.datetime.format("yyyy/MM/dd HH:mm"))
                holder.getView<View>(R.id.ll_container).singleClick {
                    dismiss()
                    ARouter.getInstance().build(MainModule.CONTACT_INFO)
                        .withString("address", item.uid)
                        .withLong("groupId", gid)
                        .navigation()
                }
            }
        }
        binding.rvUsers.adapter = mAdapter
        mAdapter.setEmptyView(R.layout.layout_empty_loading)
    }

    fun setLogId(gid: String, logId: Long): MessageFocusDialogFragment {
        lifecycleScope.launch {
            database.focusUserDao().getFocusedUsers(logId).also { users ->
                binding.tvTitle.text = "${users.size}人已关注"
                users.forEach {
                    it.contact = manager.getGroupUserInfo(gid, it.uid)
                }
                mAdapter.setList(users)
            }
        }
        return this
    }
}