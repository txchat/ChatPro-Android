package com.fzm.chat.widget

import android.content.Context
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ListView
import com.fzm.chat.R
import com.qmuiteam.qmui.widget.QMUIWrapContentListView
import com.qmuiteam.qmui.widget.popup.QMUIPopup
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.gone

/**
 * @author zhengjy
 * @since 2020/12/09
 * Description:
 */
class ListPopup(
    private val context: Context,
    private val adapter: BaseAdapter,
    @Direction
    private val direction: Int = DIRECTION_BOTTOM
) : QMUIPopup(context, direction) {

    fun create(
        width: Int,
        maxHeight: Int,
        onItemClickListener: AdapterView.OnItemClickListener
    ) {
        val listView: ListView = QMUIWrapContentListView(context, maxHeight)
        val lp: FrameLayout.LayoutParams = FrameLayout.LayoutParams(width, maxHeight)
        listView.layoutParams = lp
        listView.adapter = adapter
        listView.isVerticalScrollBarEnabled = false
        listView.divider = ColorDrawable(context.resources.getColor(R.color.biz_color_divider))
        listView.dividerHeight = 0.5f.dp
        listView.onItemClickListener = onItemClickListener
        setContentView(listView)
    }

    override fun onShowBegin(parent: View, attachedView: View): Point {
        return super.onShowBegin(parent, attachedView).also {
            mArrowDown.gone()
            mArrowUp.gone()
        }
    }

    override fun getRootLayout(): Int {
        return R.layout.popup_layout_qmui_chat
    }
}