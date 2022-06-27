package com.fzm.chat.group

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.widget.ChatAvatarView
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.databinding.ActivityMuteListBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.ext.format
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.toast
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/06/04
 * Description:
 */
@Route(path = MainModule.MUTE_LIST)
class MuteListActivity : BizActivity() {

    @JvmField
    @Autowired
    var groupId: Long = 0L

    private val viewModel by viewModel<GroupViewModel>()

    private val users = mutableListOf<GroupUser>()
    private lateinit var mAdapter: BaseQuickAdapter<GroupUser, BaseViewHolder>
    private lateinit var manager: LinearLayoutManager

    private val binding by init { ActivityMuteListBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        manager = LinearLayoutManager(this)
        binding.rvMute.layoutManager = manager
        mAdapter = object :
            BaseQuickAdapter<GroupUser, BaseViewHolder>(R.layout.item_mute_group_users, users) {
            override fun convert(holder: BaseViewHolder, item: GroupUser) {
                holder.setText(R.id.tag, item.getFirstLetter())
                val position = users.indexOf(item)
                if (position > 0) {
                    val last = users[position - 1].getFirstLetter()
                    val current = item.getFirstLetter()
                    if (last == current) {
                        holder.setGone(R.id.tag, true)
                    } else {
                        holder.setGone(R.id.tag, false)
                        holder.setText(R.id.tag, item.getFirstLetter())
                    }
                } else {
                    holder.setGone(R.id.tag, false)
                    holder.setText(R.id.tag, item.getFirstLetter())
                }

                holder.getView<ChatAvatarView>(R.id.iv_avatar)
                    .load(item.getDisplayImage(), R.mipmap.default_avatar_round)
                holder.setText(R.id.tv_name, item.getDisplayName())
                if (item.muteTime == ChatConst.MUTE_FOREVER) {
                    holder.setText(R.id.tv_mute_time, R.string.chat_tip_mute_forever)
                } else {
                    holder.setText(
                        R.id.tv_mute_time,
                        getString(
                            R.string.chat_tip_mute_to_time,
                            item.muteTime.format("yyyy/MM/dd HH:mm:ss")
                        )
                    )
                }
                holder.getView<View>(R.id.iv_cancel_mute).setOnClickListener {
                    viewModel.changeMuteTime(groupId, 0L, listOf(item.address))
                }
            }
        }
        binding.rvMute.adapter = mAdapter
        binding.sideBar.setTextView(binding.dialog)
        //设置右侧SideBar触摸监听
        binding.sideBar.setOnTouchingLetterChangedListener { s ->
            //该字母首次出现的位置
            val position = getPositionForSection(s[0].toInt())
            if (position != -1) {
                manager.scrollToPositionWithOffset(position, 0)
            }
        }
    }

    override fun initData() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.getMuteGroupUserList(groupId).observe(this) {
            mAdapter.setList(it)
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressed() }
        binding.ctbTitle.setOnRightClickListener {
            if (users.isEmpty()) {
                toast(R.string.chat_tip_no_group_user_muted)
                return@setOnRightClickListener
            }
            viewModel.changeMuteTime(groupId, 0L, users.map { it.address })
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)
        super.onBackPressed()
    }

    private fun getPositionForSection(section: Int): Int {
        for (i in users.indices) {
            val sortStr = users[i].getFirstLetter()
            val firstChar = sortStr[0]
            if (firstChar.toInt() == section) {
                return i
            }
        }
        return -1
    }
}