package com.fzm.chat.widget

import android.content.ClipData
import android.content.Context
import android.graphics.Point
import android.media.AudioManager
import android.view.View
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.ListView
import com.fzm.chat.R
import com.fzm.chat.conversation.ChatViewModel
import com.fzm.chat.conversation.adapter.TextMenuAdapter
import com.fzm.chat.conversation.adapter.TextMenuBean
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.GroupUser
import com.fzm.chat.core.data.model.isSendType
import com.qmuiteam.qmui.widget.QMUIWrapContentListView
import com.qmuiteam.qmui.widget.popup.QMUIPopup
import com.zjy.architecture.ext.clipboardManager
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.toast
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/03/03
 * Description:
 */
class ChatMessagePopup(
    context: Context,
    private val address: String?,
    private val viewModel: ChatViewModel
) : QMUIPopup(context, DIRECTION_BOTTOM) {

    private var message: ChatMessage? = null
    private var listView: ListView? = null

    private var touchX = 0
    private var touchY = 0

    init {
        create(110.dp, 400.dp)
    }

    private fun create(width: Int, maxHeight: Int) {
        listView = QMUIWrapContentListView(mContext, maxHeight)
        val lp = FrameLayout.LayoutParams(width, maxHeight)
        listView?.layoutParams = lp
        listView?.isVerticalScrollBarEnabled = false
        listView?.divider = null
        setContentView(listView)
    }

    private fun getAdapter(message: ChatMessage, role: Int?): BaseAdapter {
        val list = mutableListOf<TextMenuBean>()
        val isSend = message.isSendType
        // 消息发送超过一天
        val timeout = false/*System.currentTimeMillis() - message.datetime > TimeUnit.DAYS.toMillis(1)*/
        val canRevoke = if (message.channelType == ChatConst.GROUP_CHANNEL) {
            !timeout && message.logId != 0L && (role ?: GroupUser.LEVEL_USER > GroupUser.LEVEL_USER || isSend)
        } else {
            !timeout && message.logId != 0L && isSend
        }
        val canRef = message.logId != 0L
        val canFocus = isSend && message.logId != 0L
        when (Biz.MsgType.forNumber(message.msgType)) {
            Biz.MsgType.Text -> {
                list.add(copy(message))
                list.add(forward(message))
                if (canRef) {
                    list.add(reference(message))
                }
                if (!canFocus) {
                    list.add(focus(message))
                }
            }
            Biz.MsgType.Audio -> {
                list.add(audioChannel())
                if (!canFocus) {
                    list.add(focus(message))
                }
            }
            Biz.MsgType.Image -> {
                list.add(forward(message))
                if (canRef) {
                    list.add(reference(message))
                }
                if (!canFocus) {
                    list.add(focus(message))
                }
            }
            Biz.MsgType.Video -> {
                list.add(forward(message))
                if (canRef) {
                    list.add(reference(message))
                }
                if (!canFocus) {
                    list.add(focus(message))
                }
            }
            Biz.MsgType.File -> {
                list.add(forward(message))
                if (canRef) {
                    list.add(reference(message))
                }
                if (!canFocus) {
                    list.add(focus(message))
                }
            }
            Biz.MsgType.Forward -> {
                list.add(forward(message))
                if (!canFocus) {
                    list.add(focus(message))
                }
            }
            Biz.MsgType.ContactCard -> {
                list.add(forward(message))
                if (!canFocus) {
                    list.add(focus(message))
                }
            }
            else -> {
            }
        }
        if (canRevoke) list.add(revoke(message))
        if (message.channelType == ChatConst.GROUP_CHANNEL
            && !isSend
            && role ?: GroupUser.LEVEL_USER > GroupUser.LEVEL_USER
        ) {
            list.add(mute(message))
        }
        list.add(delete(message))
        list.add(selectMode())
        return TextMenuAdapter(mContext, list)
    }

    fun setMessage(message: ChatMessage, role: Int?): ChatMessagePopup {
        this.message = message
        listView?.adapter = getAdapter(message, role)
        return this
    }

    fun setPosition(x: Int, y: Int): ChatMessagePopup {
        touchX = x
        touchY = y
        return this
    }

    fun setOnDismissListener(listener: () -> Unit): ChatMessagePopup {
        super.setOnDismissListener(listener)
        return this
    }

    override fun onShowBegin(parent: View, attachedView: View): Point {
        val width = parent.width
        val height = parent.height
        setPositionOffsetX(touchX - width / 2 + mWindowWidth / 2)
        setPositionOffsetYWhenBottom(touchY - height)
        setPositionOffsetYWhenTop(touchY)
        return super.onShowBegin(parent, attachedView).also {
            mArrowDown.gone()
            mArrowUp.gone()
        }
    }

    override fun getRootLayout(): Int {
        return R.layout.popup_layout_qmui_chat
    }

    private fun copy(message: ChatMessage) = TextMenuBean({ R.string.chat_popup_option_copy }, {
        val clipData = ClipData.newPlainText("message", message.msg.content)
        mContext.clipboardManager?.setPrimaryClip(clipData)
        mContext.toast(R.string.chat_tips_copy_message_content)
        dismiss()
    })

    private fun forward(message: ChatMessage) = TextMenuBean({ R.string.chat_popup_option_forward }, {
        viewModel.requestForward(message)
        dismiss()
    })

    private fun reference(message: ChatMessage) = TextMenuBean({ R.string.chat_popup_option_reference }, {
        viewModel.requestReference(message)
        dismiss()
    })

    private fun focus(message: ChatMessage) = TextMenuBean({ R.string.chat_popup_option_focus }, {
        viewModel.requestFocus(message)
        dismiss()
    })

    private fun revoke(message: ChatMessage) = TextMenuBean({ R.string.chat_popup_option_revoke }, {
        viewModel.requestRevoke(message)
        dismiss()
    })

    private fun delete(message: ChatMessage) = TextMenuBean({ R.string.chat_popup_option_delete }, {
        if (address == null) return@TextMenuBean
        viewModel.requestDelete(message)
        dismiss()
    })

    private fun audioChannel() = TextMenuBean({
        val currentMode = viewModel.preference.AUDIO_CHANNEL
        if (currentMode == AudioManager.MODE_NORMAL)
            R.string.chat_popup_option_audio_channel_receiver
        else
            R.string.chat_popup_option_audio_channel_speaker
    }, {
        val newMode = viewModel.switchAudioChannel()
        if (newMode == AudioManager.MODE_NORMAL) {
            mContext.toast(R.string.chat_tips_switch_to_speaker)
        } else {
            mContext.toast(R.string.chat_tips_switch_to_receiver)
        }
        dismiss()
    })

    private fun mute(message: ChatMessage) = TextMenuBean({ R.string.chat_popup_option_mute }, {
        if (address == null) return@TextMenuBean
        viewModel.requestMute(message.target.toLong(), message.from)
        dismiss()
    })

    private fun selectMode() = TextMenuBean({ R.string.chat_popup_option_multiple_choose }, {
        viewModel.requestSelect(true)
        // 默认先选中当前的消息
        message?.isSelected = true
        dismiss()
    })
}