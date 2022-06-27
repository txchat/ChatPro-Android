package com.fzm.chat.widget

import android.content.Context
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.util.AttributeSet
import android.view.*
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.fzm.chat.R
import com.fzm.chat.animation.ControlFocusInsetsAnimationCallback
import com.fzm.chat.biz.base.FunctionModules
import com.fzm.chat.biz.base.enableRedPacket
import com.fzm.chat.biz.base.enableWallet
import com.fzm.chat.core.at.AtManager
import com.fzm.chat.core.at.AtTextChangeListener
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.po.Reference
import com.fzm.chat.databinding.LayoutChatInputBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.ext.*
import com.zjy.architecture.util.KeyboardUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2020/12/24
 * Description:
 */
class ChatInputView : LinearLayout, View.OnClickListener, AtTextChangeListener {

    companion object {
        const val INPUT_KEYBOARD = 1
        const val INPUT_VOICE = 2

        const val BOTTOM_SHOW_EMOJI = 1
        const val BOTTOM_SHOW_OTHER = 2
    }

    private var mInputType: Int = INPUT_KEYBOARD
    private var mIChatInputView: IChatInputView? = null
    private var mWindow: Window? = null
    private var keyboardHeight: Int = 0
    private var channelType: Int = 0
    private val gson by rootScope.inject<Gson>()

    private var atManager: AtManager? = null

    private val menus by lazy {
        mutableListOf<MsgTypeItem>().apply {
            add(MsgTypeItem(R.mipmap.icon_chat_images, R.string.chat_tips_input_media) { mIChatInputView?.onSelectMedia(it) })
            add(MsgTypeItem(R.mipmap.icon_chat_capture, R.string.chat_tips_input_shoot) { mIChatInputView?.onCapture(it) })
            add(MsgTypeItem(R.mipmap.icon_chat_file, R.string.chat_tips_input_file) { mIChatInputView?.onSelectFile(it) })
            if (channelType == ChatConst.PRIVATE_CHANNEL) {
                add(MsgTypeItem(R.mipmap.icon_chat_rtc, R.string.chat_tips_input_rtc) { mIChatInputView?.onStartRtc(it) })
                if (FunctionModules.enableRedPacket) {
                    add(MsgTypeItem(R.mipmap.icon_chat_red_packet, R.string.chat_tips_input_red_packet) { mIChatInputView?.onRedPacket(it) })
                }
                if (FunctionModules.enableWallet) {
                    add(MsgTypeItem(R.mipmap.icon_chat_transfer, R.string.chat_tips_input_transfer) { mIChatInputView?.onTransfer(it, ChatConst.TRANSFER) })
                }
            } else {
                if (FunctionModules.enableRedPacket) {
                    add(MsgTypeItem(R.mipmap.icon_chat_red_packet, R.string.chat_tips_input_red_packet) { mIChatInputView?.onRedPacket(it) })
                }
            }
            add(MsgTypeItem(R.mipmap.icon_chat_contact_card, R.string.chat_tips_input_contact_card) { mIChatInputView?.onContactCard(it) })
        }
    }

    private val emojis by lazy {
        mutableListOf<String>().apply {
            context.assets.open("emoji.json").use {
                val emojiList = gson.fromJson<List<EmojiItem>>(BufferedReader(InputStreamReader(it)), object : TypeToken<List<EmojiItem>>() {}.type)
                emojiList.forEach { item -> add(item.keyCode) }
            }
        }
    }

    private val resize = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
    private val nothing = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN

    private val enableOther: Boolean
        get() = menus.isNotEmpty()


    private lateinit var binding: LayoutChatInputBinding

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        binding = LayoutChatInputBinding.inflate(LayoutInflater.from(context), this, true)
        val ss = SpannableString(context.getString(R.string.chat_tips_input_say_sth))
        val ass = AbsoluteSizeSpan(14, true)
        ss.setSpan(ass, 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.etInput.hint = ss


        binding.chatInputType.setOnClickListener(this)
        binding.btnSend.setOnClickListener(this)
        binding.ivOther.setOnClickListener(this)
        binding.ivEmoji.setOnClickListener(this)
        binding.ivDelete.setOnClickListener(this)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            setWindowInsetsAnimationCallback(ControlFocusInsetsAnimationCallback(binding.etInput))
        }
    }

    fun bind(activity: FragmentActivity, iChatInputView: IChatInputView, channelType: Int) {
        mIChatInputView = iChatInputView
        this.channelType = channelType
        mWindow = activity.window
        mWindow?.setSoftInputMode(resize)
        binding.ivDelete.isEnabled = false
        binding.btnRecord.setAudioFinishRecorderListener(object : AudioRecordButton.AudioFinishRecorderListener {
            override fun onFinished(seconds: Float, filePath: String?) {
                mIChatInputView?.onAudioRecorderFinished(seconds, filePath)
            }
        })
        binding.rvOther.layoutManager = GridLayoutManager(context, 4)
        binding.rvOther.adapter = object : BaseQuickAdapter<MsgTypeItem, BaseViewHolder>(R.layout.item_message_type, menus) {
            override fun convert(holder: BaseViewHolder, item: MsgTypeItem) {
                holder.setImageResource(R.id.iv_image, item.icon)
                holder.setText(R.id.tv_type, item.text)
                holder.itemView.singleClick { item.action.invoke(it) }
            }
        }
        binding.rvEmoji.layoutManager = GridLayoutManager(context, 8)
        binding.rvEmoji.adapter = object : BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_message_emoji, emojis) {
            override fun convert(holder: BaseViewHolder, item: String) {
                holder.setText(R.id.tv_emoji, item)
                holder.itemView.setOnClickListener {
                    val selectionStart = binding.etInput.selectionStart
                    val selectionEnd = binding.etInput.selectionEnd
                    val stringBuilder = StringBuilder(binding.etInput.text.toString())
                    stringBuilder.replace(selectionStart, selectionEnd, item)
                    binding.etInput.setText(stringBuilder.toString())
                    binding.etInput.setSelection(selectionStart + item.length)
                }
            }
        }

        if (enableOther) {
            binding.ivOther.visible()
        } else {
            binding.ivOther.invisible()
        }

        binding.etInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                atManager?.beforeTextChanged(s, start, count, after)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    binding.ivDelete.isEnabled = false
                    if (binding.btnSend.isVisible) {
                        binding.btnSend.invisible()
                    }
                    if (enableOther) binding.ivOther.visible()
                } else {
                    binding.ivDelete.isEnabled = true
                    if (binding.btnSend.isInvisible) {
                        binding.btnSend.visible()
                    }
                    if (enableOther) binding.ivOther.invisible()
                }
                atManager?.onTextChanged(s, start, before, count)
            }

            override fun afterTextChanged(s: Editable?) {
                atManager?.afterTextChanged(s)
            }
        })
    }

    /**
     * 设置消息引用
     */
    fun setReferenceMsg(text: String, callback: () -> Unit) {
        binding.llReference.visible()
        binding.tvReference.text = text
        binding.ivClearRef.setOnClickListener {
            callback()
            clearReferenceMsg()
        }
    }

    /**
     * 清空消息引用
     */
    fun clearReferenceMsg() {
        binding.tvReference.text = ""
        binding.llReference.gone()
    }

    fun setAtManager(atManager: AtManager?) {
        this.atManager = atManager
        this.atManager?.setTextChangeListener(this)
    }

    override fun onTextAdd(content: String?, start: Int, length: Int) {
        if (visibility == VISIBLE) {
            if (!binding.etInput.isVisible) {
                binding.chatInputType.performClick()
                binding.etInput.requestFocus()
            }
            binding.etInput.post {
                KeyboardUtils.showKeyboard(binding.etInput)
            }
            binding.etInput.editableText.insert(start, content)
        }
    }

    override fun onTextDelete(start: Int, length: Int) {
        if (visibility == VISIBLE) {
            if (!binding.etInput.isVisible) {
                binding.chatInputType.performClick()
                binding.etInput.requestFocus()
            }
            val end = start + length - 1
            binding.etInput.editableText.replace(start, end, "")
        }
    }

    fun setText(text: CharSequence?) {
        binding.etInput.setText(text)
        binding.etInput.setSelection(text?.length ?: 0)
    }

    fun getEditText() = binding.etInput

    fun setKeyboardHeight(height: Int) {
        this.keyboardHeight = height
    }

    fun isExpand() = binding.flOther.isVisible or binding.flEmoji.isVisible

    /**
     * 显示底部菜单
     *
     * @param bottomPanelType 1: 显示Emoji  2: 添加更多选项
     */
    fun showBottomLayout(bottomPanelType: Int) {
        val manager = context.inputMethodManager
        val active = manager?.isActive ?: false
        if (keyboardHeight == 0) {
            mIChatInputView?.onLayoutAnimation(true)
            if (bottomPanelType == BOTTOM_SHOW_EMOJI) {
                binding.flEmoji.visible()
                binding.ivEmoji.setImageResource(R.drawable.icon_keyboard_add)
                binding.flOther.gone()
            } else {
                binding.flOther.visible()
                binding.ivEmoji.setImageResource(R.drawable.icon_emoji_add)
                binding.flEmoji.gone()
            }
        } else {
            mWindow?.setSoftInputMode(nothing)
            if (bottomPanelType == BOTTOM_SHOW_OTHER && binding.flOther.height != keyboardHeight) {
                // 高度与键盘高度不相等，则修改高度
                binding.flOther.layoutParams = binding.flOther.layoutParams.apply {
                    height = keyboardHeight
                }
            }
            if (bottomPanelType == BOTTOM_SHOW_EMOJI && binding.flEmoji.height != keyboardHeight) {
                binding.flEmoji.layoutParams = binding.flEmoji.layoutParams.apply {
                    height = keyboardHeight
                }
            }

            mIChatInputView?.onLayoutAnimation(true)
            if (bottomPanelType == BOTTOM_SHOW_EMOJI) {
                binding.flEmoji.visible()
                binding.ivEmoji.setImageResource(R.drawable.icon_keyboard_add)
                binding.flOther.gone()
            } else {
                binding.flOther.visible()
                binding.ivEmoji.setImageResource(R.drawable.icon_emoji_add)
                binding.flEmoji.gone()
            }
        }
        if (active) {
            manager?.hideSoftInputFromWindow(windowToken, 0)
        }
        mIChatInputView?.scrollMessageToBottom()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) {
            binding.etInput.clearFocus()
        }
        binding.etInput.isEnabled = enabled
    }

    /**
     * 隐藏底部菜单
     */
    fun hideBottomLayout(disableTransition: Boolean = false): Boolean {
        if (isExpand()) {
            if (!disableTransition) {
                mIChatInputView?.onLayoutAnimation(false)
            }
            binding.flOther.gone()
            binding.flEmoji.gone()
            binding.ivEmoji.setImageResource(R.drawable.icon_emoji_add)
            mWindow?.setSoftInputMode(resize)
            return true
        }
        return false
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.chat_input_type -> {
                val type = if (mInputType == INPUT_KEYBOARD) {
                    INPUT_VOICE
                } else {
                    INPUT_KEYBOARD
                }
                switchInputType(type, true)
            }
            R.id.btn_send -> {
                v.checkSingle(200) {
                    val end = atManager?.aitInfo?.values?.maxOfOrNull { block -> block.lastSegmentEnd } ?: -1
                    val text = binding.etInput.text
                    val content = if (end + 1 == text.length) {
                        // 如果以@结尾，则不对text末位进行trim，防止@块被意外破坏
                        text?.trimStart()?.toString()
                    } else {
                        text?.trim()?.toString()
                    }
                    if (content.isNullOrEmpty()) {
                        context.toast(R.string.chat_tips_input_msg1)
                        return@checkSingle
                    }
                    mIChatInputView?.onSend(v, content)
                    binding.etInput.text.clear()
                }
            }
            R.id.iv_other -> {
                if (binding.flOther.isVisible) {
                    binding.etInput.requestFocus()
                    context.inputMethodManager?.showSoftInput(binding.etInput, 0)
                } else {
                    switchInputType(INPUT_KEYBOARD, false)
                    showBottomLayout(bottomPanelType = BOTTOM_SHOW_OTHER)
                }
            }
            R.id.iv_emoji -> {
                if (binding.flEmoji.isVisible) {
                    binding.ivEmoji.setImageResource(R.drawable.icon_emoji_add)
                    binding.etInput.requestFocus()
                    context.inputMethodManager?.showSoftInput(binding.etInput, 0)
                } else {
                    switchInputType(INPUT_KEYBOARD, false)
                    showBottomLayout(bottomPanelType = BOTTOM_SHOW_EMOJI)
                }
            }
            R.id.iv_delete -> {
                binding.etInput.onKeyDown(KeyEvent.KEYCODE_DEL, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                binding.etInput.onKeyUp(KeyEvent.KEYCODE_DEL, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
        }
    }

    private fun switchInputType(type: Int, showKeyboard: Boolean) {
        if (mInputType == type) return
        mIChatInputView?.switchInputType(type) {
            if (it) {
                mInputType = type
                if (mInputType == INPUT_KEYBOARD) {
                    binding.etInput.visible()
                    binding.btnRecord.gone()
                    if (showKeyboard) {
                        binding.etInput.requestFocus()
                        context.inputMethodManager?.showSoftInput(binding.etInput, 0)
                    }
                } else {
                    binding.btnSend.invisible()
                    binding.etInput.gone()
                    if (enableOther) binding.ivOther.visible()
                    binding.btnRecord.visible()
                    context.inputMethodManager?.hideSoftInputFromWindow(binding.etInput.windowToken, 0)
                    hideBottomLayout()
                }
            }
        }
    }

    inner class EmojiItem(val name: String, val keyCode: String) : Serializable

    inner class MsgTypeItem(
        val icon: Int,
        val text: Int,
        val action: (View) -> Unit
    ) : Serializable

    interface IChatInputView {

        fun onLayoutAnimation(appear: Boolean)

        /**
         * 点击发送按钮
         */
        fun onSend(view: View, content: String)

        /**
         * 选择图片视频
         */
        fun onSelectMedia(view: View)

        /**
         * 拍摄
         */
        fun onCapture(view: View)

        /**
         * 选择阅后即焚
         */
        @Deprecated("暂不支持此功能")
        fun onSnapChat(view: View)

        /**
         * 选择文件
         */
        fun onSelectFile(view: View)

        /**
         * 准备开始音视频通话
         */
        fun onStartRtc(view: View)

        /**
         * 转账收款
         */
        fun onTransfer(view: View, transferType: Int)

        /**
         * 发红包
         */
        fun onRedPacket(view: View)

        /**
         * 发送名片
         */
        fun onContactCard(view: View)

        /**
         * 消息列表滚动到底部
         */
        fun scrollMessageToBottom()

        /**
         * 切换语音输入和文字输入
         */
        fun switchInputType(type: Int, confirm: (Boolean) -> Unit)

        /**
         * 语音输入结束
         */
        fun onAudioRecorderFinished(seconds: Float, filePath: String?)
    }
}