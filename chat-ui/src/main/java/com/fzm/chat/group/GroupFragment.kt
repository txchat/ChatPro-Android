package com.fzm.chat.group

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.alibaba.android.arouter.facade.annotation.Route
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.arch.connection.utils.urlKey
import com.fzm.chat.R
import com.fzm.chat.adapter.GroupPagerAdapter
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.widget.FadePageTransformer
import com.fzm.chat.core.data.bean.ServerGroupInfo
import com.fzm.chat.databinding.FragmentGroupBinding
import com.fzm.chat.router.main.MainModule
import okhttp3.HttpUrl
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

/**
 * @author zhengjy
 * @since 2021/05/06
 * Description:
 */
@Route(path = MainModule.GROUP_SERVER)
class GroupFragment : BizFragment() {

    private val viewModel by viewModel<GroupViewModel>()
    private lateinit var pagerAdapter: GroupPagerAdapter

    /**
     * 选中的服务器分组索引
     */
    private var selectPos = 0

    private lateinit var mAdapter: BaseQuickAdapter<ServerGroupInfo, BaseViewHolder>

    private val binding by init<FragmentGroupBinding>()

    override val root: View
        get() = binding.root

    override fun initData() {
        binding.rvServer.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        mAdapter = object : BaseQuickAdapter<ServerGroupInfo, BaseViewHolder>(R.layout.item_server_group_segment, mutableListOf()) {
            override fun convert(holder: BaseViewHolder, item: ServerGroupInfo) {
                val index = mAdapter.data.indexOf(item)
                val name = holder.getView<TextView>(R.id.tv_name)
                val socket = viewModel.connection.getChatSocket(item.value)
                val id = if (socket?.isAlive == true) {
                    if (selectPos == index) {
                        holder.setVisible(R.id.select_arrow, true)
                        holder.setImageResource(R.id.select_arrow, R.drawable.ic_triangle_green)
                        name.setTextColor(resources.getColor(R.color.biz_green_tips))
                        holder.setTextColorRes(R.id.tv_address, R.color.biz_green_tips)
                        holder.setBackgroundColor(R.id.sl_container, resources.getColor(R.color.biz_green_tips_light))
                    } else {
                        holder.setVisible(R.id.select_arrow, false)
                        name.setTextColor(resources.getColor(R.color.biz_text_grey_dark))
                        holder.setTextColorRes(R.id.tv_address, R.color.biz_text_grey_light)
                        holder.setBackgroundColor(R.id.sl_container, resources.getColor(R.color.biz_color_primary_dark))
                    }
                    item.state = socket.state.value ?: 0
                    R.drawable.ic_server_connect
                } else {
                    if (selectPos == index) {
                        holder.setVisible(R.id.select_arrow, true)
                        holder.setImageResource(R.id.select_arrow, R.drawable.ic_triangle_red)
                        name.setTextColor(resources.getColor(R.color.biz_red_tips))
                        holder.setTextColorRes(R.id.tv_address, R.color.biz_red_tips)
                        holder.setBackgroundColor(R.id.sl_container, resources.getColor(R.color.biz_red_tips_light))
                    } else {
                        holder.setVisible(R.id.select_arrow, false)
                        name.setTextColor(resources.getColor(R.color.biz_text_grey_dark))
                        holder.setTextColorRes(R.id.tv_address, R.color.biz_text_grey_light)
                        holder.setBackgroundColor(R.id.sl_container, resources.getColor(R.color.biz_color_primary_dark))
                    }
                    item.state = socket?.state?.value ?: 0
                    R.drawable.ic_server_disconnect
                }
                val status = ResourcesCompat.getDrawable(resources, id, null)?.apply {
                    setBounds(0, 0, minimumWidth, minimumHeight)
                }
                name.setCompoundDrawables(null, null, status, null)
                name.text = item.name
                holder.setText(R.id.tv_address, HttpUrl.parse(item.value)?.host())
            }
        }
        mAdapter.setOnItemClickListener { _, _, position ->
            binding.vpGroup.currentItem = position
        }
        binding.rvServer.adapter = mAdapter

        pagerAdapter = GroupPagerAdapter(this)
        binding.vpGroup.apply {
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    val lastPos = selectPos
                    if (binding.vpGroup.currentItem != selectPos) {
                        binding.rvServer.smoothScrollToPosition(position)
                        selectPos = position
                        mAdapter.notifyItemChanged(lastPos)
                        mAdapter.notifyItemChanged(selectPos)
                    }
                }
            })
            setPageTransformer(FadePageTransformer())
            offscreenPageLimit = 3
            adapter = pagerAdapter
        }
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        viewModel.servers.observe(viewLifecycleOwner) { servers ->
            val list = servers.map {
                ServerGroupInfo(it.id, 0, it.name, it.address)
            }.distinctBy { it.value }
            pagerAdapter.setList(list)

            mAdapter.setList(list)
        }
        viewModel.connection.observeSocketState(viewLifecycleOwner) { (key, state) ->
            mAdapter.data.forEachIndexed { index, info ->
                if (info.value.urlKey() == key) {
                    if (info.state != state) {
                        mAdapter.notifyItemChanged(index)
                    }
                    return@forEachIndexed
                }
            }
        }
    }

    override fun setEvent() {

    }
}