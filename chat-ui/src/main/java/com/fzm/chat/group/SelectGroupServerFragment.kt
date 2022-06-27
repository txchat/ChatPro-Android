package com.fzm.chat.group

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import androidx.navigation.fragment.findNavController
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.contact.ChatServerListFragment
import com.fzm.chat.contact.ContactViewModel
import com.fzm.chat.contact.ServerManagementActivity
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.databinding.FragmentSelectGroupServerBinding
import com.zjy.architecture.util.other.BarUtils
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/05/14
 * Description:
 */
class SelectGroupServerFragment : BizFragment() {

    private val binding by init<FragmentSelectGroupServerBinding>()
    private lateinit var severFragment: ChatServerListFragment

    private val delegate by inject<LoginDelegate>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {

    }

    override fun initData() {
        severFragment = ChatServerListFragment.create(true)
        severFragment.setOnSelectListener {
            findNavController().navigate(
                if (delegate.hasCompany()) R.id.selected_group_member_type else R.id.selected_group_member, bundleOf(
                    "server" to it.value
                ))
        }

        childFragmentManager.commit {
            add(R.id.server_container, severFragment, ChatServerListFragment::class.java.name)
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { activity?.onBackPressed() }
        binding.ctbTitle.setOnRightClickListener {
            startActivityForResult(Intent(requireContext(), ServerManagementActivity::class.java), 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        severFragment.refresh()
    }
}