package com.fzm.chat.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.fzm.arch.connection.utils.urlKey
import com.fzm.chat.core.data.bean.ServerGroupInfo
import com.fzm.chat.group.GroupListFragment

/**
 * @author zhengjy
 * @since 2021/05/07
 * Description:
 */
class GroupPagerAdapter(
    fragment: Fragment,
) : FragmentStateAdapter(fragment) {

    private val servers: MutableList<ServerGroupInfo> = mutableListOf()

    override fun getItemCount(): Int {
        return servers.size
    }

    override fun createFragment(position: Int): Fragment {
        return GroupListFragment.create(servers[position].value)
    }

    override fun getItemId(position: Int): Long {
        return servers[position].value.urlKey().hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        servers.forEach {
            if (it.value.urlKey().hashCode().toLong() == itemId) {
                return true
            }
        }
        return false
    }

    fun setList(list: Collection<ServerGroupInfo>) {
        if (list !== this.servers) {
            this.servers.clear()
            if (!list.isNullOrEmpty()) {
                this.servers.addAll(list)
            }
        } else {
            if (!list.isNullOrEmpty()) {
                val newList = ArrayList(list)
                this.servers.clear()
                this.servers.addAll(newList)
            } else {
                this.servers.clear()
            }
        }
        notifyDataSetChanged()
    }
}
