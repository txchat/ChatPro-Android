package com.fzm.chat.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.vm.ContactSelectViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.getViewModel
import org.koin.core.parameter.parametersOf

/**
 * @author zhengjy
 * @since 2021/10/06
 * Description:
 */
class ContactCardSelectFragment : ContactSelectFragment() {

    companion object {
        fun create(channelFilter: Int): ContactCardSelectFragment {
            return ContactCardSelectFragment().apply {
                arguments = bundleOf("channelFilter" to channelFilter)
            }
        }
    }

    private val contactManager by inject<ContactManager>()
    private var channelFilter: Int = 0

    override val viewModel: ContactCardViewModel
        get() = requireActivity().getViewModel { parametersOf(channelFilter) }

    override val showOA: Boolean get() = channelFilter and ContactSelectViewModel.GROUP == 0

    override fun initView(view: View, savedInstanceState: Bundle?) {
        super.initView(view, savedInstanceState)
        channelFilter = arguments?.getInt("channelFilter") ?: ContactSelectViewModel.PRIVATE
    }

    override fun setEvent() {
        super.setEvent()
        binding.ctlSelectOa.root.setOnClickListener {
            openTeamMemberSelectPage(mapOf("action" to ForwardSelectFragment.ACTION_FORWARD_MESSAGE))
        }
        mAdapter.setOnItemClickListener { _, _, position ->
            requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("contact", mAdapter.data[position].contact)
            })
            requireActivity().finish()
        }
    }

    override fun onSelectTeamMembers(users: List<String>) {
        lifecycleScope.launch {
            loading(true)
            val contact = contactManager.getUserInfo(users[0], true)
            dismiss()
            requireActivity().setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("contact", contact)
            })
            requireActivity().finish()
        }
    }

    class ContactCardViewModel(delegate: LoginDelegate, private val filter: Int) : ContactSelectViewModel(delegate) {

        override val channelFilter: Int
            get() = filter
    }
}