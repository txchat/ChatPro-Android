package com.fzm.widget.verify

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.fzm.widget.R
import com.fzm.widget.databinding.WidgetLayoutDialogVerifyCodeShortBinding
import com.fzm.widget.dp
import com.fzm.widget.encryptAccount
import com.fzm.widget.encryptPhone
import com.fzm.widget.verify.VerifyCodeView.OnCodeCompleteListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2020/08/10
 * Description:验证码输入弹窗
 */
class VerifyCodeDialog private constructor(
    private val mContext: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val length: CodeLength
) : Dialog(mContext) {

    companion object {
        private const val SEND_SMS = 1
        private const val SUGGEST_VOICE = 2
        private const val SEND_VOICE = 3

        const val TYPE_SEND_SMS = "sms"
        const val TYPE_SEND_VOICE = "voice"
    }

    private lateinit var container: View

    private var phone = ""
    private var mCode = ""
    private var autoSend = false
    private var sendType = TYPE_SEND_SMS

    private val spStr =
        SpannableString(getContext().getString(R.string.widget_error_receive_message)).apply {

        }

    private var onVerifyCodeCompleteListener: ((View?, String, String) -> Unit)? = null

    private var onSuspendSendCodeListener: (suspend (View?) -> Boolean)? = null
    private var onSendCodeListener: ((View?) -> Unit)? = null
    private var onSendVoiceCodeListener: ((View?) -> Unit)? = null

    private var countDownJob: Job? = null

    /**
     * widget_layout_dialog_verify_code_short和widget_layout_dialog_verify_code_long所包含的
     * 控件id相同，因此选取一种作为ViewBinding的类型
     */
    private lateinit var binding: WidgetLayoutDialogVerifyCodeShortBinding

    init {
        init()
    }

    private fun init() {
        window?.setBackgroundDrawableResource(R.color.widget_transparent)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        setCanceledOnTouchOutside(false)
        container = when (length) {
            CodeLength.SHORT -> LayoutInflater.from(mContext)
                .inflate(R.layout.widget_layout_dialog_verify_code_short, null)
            CodeLength.MEDIUM -> LayoutInflater.from(mContext)
                .inflate(R.layout.widget_layout_dialog_verify_code_medium, null)
            CodeLength.LONG -> LayoutInflater.from(mContext)
                .inflate(R.layout.widget_layout_dialog_verify_code_long, null)
        }
        setContentView(container)
        binding = WidgetLayoutDialogVerifyCodeShortBinding.bind(container)
        binding.bottomTips.highlightColor =
            ContextCompat.getColor(mContext, R.color.widget_transparent)
        val clickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun updateDrawState(ds: TextPaint) {
                ds.color = ContextCompat.getColor(mContext, R.color.widget_color_primary)
                ds.isUnderlineText = false
            }

            override fun onClick(widget: View) {
                switchState(SEND_VOICE)
                onSendVoiceCodeListener?.invoke(widget)
            }
        }
        spStr.setSpan(
            clickableSpan,
            spStr.length - 4,
            spStr.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        binding.icClose.setOnClickListener { dismiss() }
    }

    override fun show() {
        binding.codeTips.text =
            mContext.getString(R.string.widget_send_code_to, encryptAccount(phone))
        super.show()
        binding.verifyCode.postDelayed({
            if (autoSend) {
                binding.sendCode.performClick()
            }
            val focus = binding.verifyCode.focus()
            val manager = context.getSystemService<InputMethodManager>()
            focus.requestFocus()
            manager?.showSoftInput(focus, 0)
        }, 100)
        window?.attributes = window?.attributes?.apply { width = 300.dp }
    }

    private fun countDown() {
        countDownJob?.cancel()
        countDownJob = lifecycleOwner.lifecycleScope.launch {
            // 倒计时60s
            switchState(SEND_SMS)
            binding.sendCode.setTextColor(mContext.resources.getColor(R.color.widget_text_color_light))
            repeat(60) {
                binding.sendCode.text = mContext.getString(R.string.widget_send_code_timer, 60 - it)
                delay(1000)
            }
            switchState(SUGGEST_VOICE)
            binding.sendCode.setTextColor(mContext.resources.getColor(R.color.widget_color_primary))
            binding.sendCode.text = mContext.getString(R.string.widget_send_code)
        }
    }

    private fun switchState(state: Int) {
        when (state) {
            SEND_SMS -> {
                sendType = TYPE_SEND_SMS
                binding.bottomTips.setText(R.string.widget_send_code_success_tip)
                binding.bottomTips.setTextColor(
                    ContextCompat.getColor(mContext, R.color.widget_text_color_grey)
                )
                binding.bottomTips.visibility = View.VISIBLE
            }
            SUGGEST_VOICE -> {
                if (onSendVoiceCodeListener != null) {
                    sendType = TYPE_SEND_SMS
                    binding.bottomTips.text = spStr
                    binding.bottomTips.setTextColor(
                        ContextCompat.getColor(mContext, R.color.widget_text_color)
                    )
                    binding.bottomTips.movementMethod = LinkMovementMethod.getInstance()
                    binding.bottomTips.visibility = View.VISIBLE
                }
            }
            SEND_VOICE -> {
                if (onSendVoiceCodeListener != null) {
                    sendType = TYPE_SEND_VOICE
                    binding.bottomTips.setText(R.string.widget_warn_receive_radio_phone)
                    binding.bottomTips.setTextColor(
                        ContextCompat.getColor(mContext, R.color.widget_text_color)
                    )
                    binding.bottomTips.visibility = View.VISIBLE
                }
            }
        }
    }

    fun setOnSendCodeListener(listener: ((View?) -> Unit)?) {
        onSendCodeListener = listener
        binding.sendCode.setOnClickListener {
            onSendCodeListener?.invoke(binding.sendCode)
            countDown()
        }
    }

    fun setOnSendCodeListenerSuspend(listener: (suspend (View?) -> Boolean)?) {
        onSuspendSendCodeListener = listener
        binding.sendCode.setOnClickListener {
            lifecycleOwner.lifecycleScope.launch {
                if (onSuspendSendCodeListener?.invoke(binding.sendCode) == true) {
                    countDown()
                }
            }
        }
    }

    fun setOnSendVoiceCodeListener(listener: ((View?) -> Unit)?) {
        onSendVoiceCodeListener = listener
    }

    fun setOnCodeCompleteListener(listener: ((View?, String, String) -> Unit)?) {
        onVerifyCodeCompleteListener = listener
        binding.verifyCode.setOnCodeCompleteListener(object : OnCodeCompleteListener {
            override fun onCodeComplete(view: View?, code: String) {
                mCode = code
                onVerifyCodeCompleteListener?.invoke(view, sendType, code)
            }
        })
    }

    class Builder(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        codeLength: CodeLength = CodeLength.SHORT
    ) {

        private val mDialog: VerifyCodeDialog =
            VerifyCodeDialog(context, lifecycleOwner, codeLength)

        fun setPhone(phone: String): Builder {
            mDialog.phone = phone
            return this
        }

        fun setAutoSend(autoSend: Boolean): Builder {
            mDialog.autoSend = autoSend
            return this
        }

        fun setOnDismissListener(listener: DialogInterface.OnDismissListener?): Builder {
            mDialog.setOnDismissListener { dialog -> listener?.onDismiss(dialog) }
            return this
        }

        fun setOnSendCodeListener(listener: ((View?) -> Unit)?): Builder {
            mDialog.setOnSendCodeListener(listener)
            return this
        }

        fun setOnSendCodeListenerSuspend(listener: (suspend (View?) -> Boolean)?): Builder {
            mDialog.setOnSendCodeListenerSuspend(listener)
            return this
        }

        fun setOnSendVoiceCodeListener(listener: ((View?) -> Unit)?): Builder {
            mDialog.setOnSendVoiceCodeListener(listener)
            return this
        }

        fun setOnCodeCompleteListener(listener: ((view: View?, sendType: String, code: String) -> Unit)?): Builder {
            mDialog.setOnCodeCompleteListener(listener)
            return this
        }

        fun build(): VerifyCodeDialog {
            return mDialog
        }
    }
}

enum class CodeLength {
    SHORT, MEDIUM, LONG
}