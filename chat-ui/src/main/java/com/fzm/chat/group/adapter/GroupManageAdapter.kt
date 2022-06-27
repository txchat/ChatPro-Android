package com.fzm.chat.group.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.fzm.chat.databinding.ItemBottomTextBinding
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.visible
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/06/03
 * Description:
 */
class GroupManageAdapter(private val context: Context) : BaseAdapter() {

    fun setData(list: List<ManageOption>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    private val data = mutableListOf<ManageOption>()

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): Any {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val root: View
        val bind: ItemBottomTextBinding
        if (convertView == null) {
            bind = ItemBottomTextBinding.inflate(LayoutInflater.from(context))
            root = bind.root
            root.tag = bind
        } else {
            root = convertView
            bind = root.tag as ItemBottomTextBinding
        }
        bind.tvAction.text = data[position].text
        if (data[position].subText.isNullOrEmpty()) {
            bind.tvSubText.gone()
        } else {
            bind.tvSubText.visible()
            bind.tvSubText.text = data[position].subText
        }

        return root
    }
}


data class ManageOption(
    val text: String,
    val subText: String?,
    val action: () -> Unit
) : Serializable