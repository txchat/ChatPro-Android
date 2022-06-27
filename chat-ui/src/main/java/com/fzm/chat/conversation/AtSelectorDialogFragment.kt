package com.fzm.chat.conversation

import android.os.Bundle
import android.view.*
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import com.fzm.chat.R
import com.fzm.chat.biz.widget.ChatSearchView
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.bean.Contact
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.databinding.DialogAtSelectorBinding
import com.fzm.chat.group.GroupUserListFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.visible

/**
 * @author zhengjy
 * @since 2021/09/25
 * Description:
 */
class AtSelectorDialogFragment : BottomSheetDialogFragment() {

    companion object {
        fun create(gid: Long, role: Int) : AtSelectorDialogFragment {
            return AtSelectorDialogFragment().apply {
                arguments = bundleOf(
                    "gid" to gid,
                    "role" to role
                )
            }
        }
    }

    private val allUser by lazy {
        GroupUser(
            "", ChatConst.AT_ALL_MEMBERS, 0, Contact.RELATION, "所有人",
            0L, "", "", null, null
        )
    }

    private var gid: Long = 0L
    private var role: Int = GroupUser.LEVEL_USER

    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private lateinit var binding: DialogAtSelectorBinding

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
        binding = DialogAtSelectorBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database.groupUserDao().getGroupUserList(gid).observe(viewLifecycleOwner) {
            binding.tvAtAll.text = "所有人（${it.size}）"
        }
        binding.llAtAll.setOnClickListener {
            dismiss()
            singleSelectListener?.invoke(allUser)
        }
        val fragment = GroupUserListFragment.create(gid, GroupUserListFragment.ACTION_SINGLE, role = role, excludeSelf = true)
        fragment.setOnSingleSelectedListener {
            dismiss()
            singleSelectListener?.invoke(it)
        }
        childFragmentManager.commit {
            add(R.id.fcv_container, fragment)
        }
        binding.search.setOnTextChangeListener(object : ChatSearchView.OnTextChangeListener {
            override fun onTextChange(s: String) {
                fragment.getGroupUserList(s)
            }
        })
        binding.tvCancel.setOnClickListener { dismiss() }
    }

    private var singleSelectListener: ((GroupUser) -> Unit)? = null

    fun setAtSelectListener(listener: ((GroupUser) -> Unit)?) {
        this.singleSelectListener = listener
    }
}