package com.fzm.chat.widget

import android.content.Context
import android.graphics.Point
import android.text.Html
import android.view.View
import android.widget.*
import com.fzm.chat.R
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.conversation.SessionViewModel
import com.fzm.chat.conversation.adapter.TextMenuAdapter
import com.fzm.chat.conversation.adapter.TextMenuBean
import com.fzm.chat.core.data.bean.Contact
import com.fzm.widget.dialog.EasyDialog
import com.qmuiteam.qmui.widget.QMUIWrapContentListView
import com.qmuiteam.qmui.widget.popup.QMUIPopup
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.gone

/**
 * @author zhengjy
 * @since 2020/12/29
 * Description:
 */
class SessionOperationPopup(
    private val context: Context,
    private val viewModel: SessionViewModel
) : QMUIPopup(context, DIRECTION_BOTTOM) {

    private var listView: ListView? = null
    private var address: String = ""
    private var name: String = ""
    private var channelType: Int = 0

    private var touchX = 0
    private var touchY = 0

    override fun getRootLayout(): Int {
        return R.layout.popup_layout_qmui_chat
    }

    init {
        create(110.dp, 350.dp)
    }

    private fun create(width: Int, maxHeight: Int) {
        listView = QMUIWrapContentListView(mContext, maxHeight)
        val lp = FrameLayout.LayoutParams(width, maxHeight)
        listView?.layoutParams = lp
        listView?.isVerticalScrollBarEnabled = false
        listView?.divider = null
        setContentView(listView)
    }

    private fun getAdapter(flag: Int): BaseAdapter {
        val list = mutableListOf<TextMenuBean>()
        // 消息置顶
        val stickTop = flag and Contact.STICK_TOP == Contact.STICK_TOP
        list.add(TextMenuBean({
            if (stickTop) R.string.chat_top_cancel else R.string.chat_top_operate
        }, {
            viewModel.changeStickTop(address, channelType, !stickTop)
            dismiss()
        }))
        // 消息免打扰
        val noDisturb = flag and Contact.NO_DISTURB == Contact.NO_DISTURB
        list.add(TextMenuBean({
            if (noDisturb) R.string.chat_disturb_cancel else R.string.chat_disturb_operate
        }, {
            viewModel.changeNoDisturb(address, channelType, !noDisturb)
            dismiss()
        }))
        // 删除聊天
        list.add(TextMenuBean({ R.string.chat_message_delete }, {
            val content = context.getString(
                R.string.chat_dialog_delete_session,
                AppConfig.APP_ACCENT_COLOR_STR,
                name
            )
            val dialog = EasyDialog.Builder()
                .setHeaderTitle(context.getString(R.string.biz_tips))
                .setBottomLeftText(context.getString(R.string.biz_cancel))
                .setBottomRightText(context.getString(R.string.biz_confirm))
                .setContent(Html.fromHtml(content))
                .setBottomLeftClickListener(null)
                .setBottomRightClickListener { dialog ->
                    dialog.dismiss()
                    viewModel.deleteSession(address, channelType)
                }.create(context)
            dialog.show()
            dismiss()
        }))
        return TextMenuAdapter(context, list)
    }

    fun setSessionInfo(address: String, name: String, flag: Int, channelType: Int): SessionOperationPopup {
        this.address = address
        this.name = name
        listView?.adapter = getAdapter(flag)
        this.channelType = channelType
        return this
    }

    fun setPosition(x: Int, y: Int): SessionOperationPopup {
        touchX = x
        touchY = y
        return this
    }

    fun setOnDismissListener(listener: () -> Unit): SessionOperationPopup {
        super.setOnDismissListener(listener)
        return this
    }

    override fun onShowBegin(parent: View, attachedView: View): Point {
        val point = super.onShowBegin(parent, attachedView)
        return Point(touchX, point.y + touchY - parent.height).also {
            mArrowDown.gone()
            mArrowUp.gone()
        }
    }
}