package com.fzm.chat.conversation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.fzm.chat.databinding.ItemTextPopupBinding
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/03/03
 * Description:
 */
class TextMenuAdapter(
    private val context: Context,
    private val list: List<TextMenuBean>,
) : BaseAdapter() {
    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(position: Int): Any {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val root: View
        val bind: ItemTextPopupBinding
        if (convertView == null) {
            bind = ItemTextPopupBinding.inflate(LayoutInflater.from(context))
            root = bind.root
            root.tag = bind
        } else {
            root = convertView
            bind = root.tag as ItemTextPopupBinding
        }

        list[position].apply {
            if (textRes != null) {
                bind.option.setText(textRes.invoke())
            } else if (text != null) {
                bind.option.text = text.invoke()
            }
        }
        bind.root.setOnClickListener { list[position].onClick() }
        return root
    }
}

class TextMenuBean(
    val textRes: (() -> Int)?,
    val onClick: () -> Unit,
    val text: (() -> String)? = null
) : Serializable