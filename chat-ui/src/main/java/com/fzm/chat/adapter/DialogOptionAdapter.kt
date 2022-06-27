package com.fzm.chat.adapter

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
 * @since 2021/03/31
 * Description:
 */
class DialogOptionAdapter(private val context: Context) : BaseAdapter() {

    fun setData(list: List<DialogOption>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    private val data = mutableListOf<DialogOption>()

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
        bind.tvAction.text = data[position].name
        if (data[position].subText.isNullOrEmpty()) {
            bind.tvSubText.gone()
        } else {
            bind.tvSubText.visible()
            bind.tvSubText.text = data[position].subText
        }

        return root
    }
}


data class DialogOption(
    val name: String,
    val subText: String? = null,
    val action: () -> Unit
) : Serializable