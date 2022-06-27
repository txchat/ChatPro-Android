package com.fzm.widget.verify

import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.text.InputFilter
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.widget.addTextChangedListener
import com.fzm.widget.R
import com.fzm.widget.dp
import java.lang.StringBuilder

/**
 * @author zhengjy
 * @since 2019/03/12
 * Description:
 */
class VerifyCodeView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    val NUMBER = 1
    val PASSWORD = 2
    val FLAT = 1
    val SHADOW = 2

    private var inputCount: Int

    @ColorInt
    private var textColor: Int
    private var textSize: Float
    private var itemSize: Float
    private var codeBg: Int
    private var inputType: Int

    private var editText: EditText? = null
    private val textViewList = mutableListOf<TextView>()

    private var listener: OnCodeCompleteListener? = null
    private var disable: Boolean = false

    private var content: StringBuilder = StringBuilder()

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerifyCodeView, defStyleAttr, 0)
        inputCount = typedArray.getInt(R.styleable.VerifyCodeView_input_count, 4)
        textColor = typedArray.getColor(R.styleable.VerifyCodeView_text_color, ContextCompat.getColor(context, R.color.widget_text_color))
        textSize = typedArray.getDimension(R.styleable.VerifyCodeView_text_size, 17f)
        itemSize = typedArray.getDimension(R.styleable.VerifyCodeView_item_size, 40f.dp.toFloat())
        codeBg = typedArray.getInt(R.styleable.VerifyCodeView_code_bg, FLAT)
        inputType = typedArray.getInt(R.styleable.VerifyCodeView_input_type, NUMBER)
        typedArray.recycle()
        initView()
    }

    private fun initView() {
        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.HORIZONTAL
        for (i in 0 until inputCount) {
            // 创建一个输入框
            val relativeLayout = RelativeLayout(context)
            val relativeLayoutParams = ViewGroup.LayoutParams(itemSize.toInt(), itemSize.toInt())
            relativeLayout.gravity = Gravity.CENTER
            relativeLayout.background = if (codeBg == FLAT) {
                ContextCompat.getDrawable(context, R.drawable.widget_code_item)
            } else {
                ContextCompat.getDrawable(context, R.mipmap.bg_code_box)
            }
            relativeLayout.layoutParams = relativeLayoutParams
            val textView = TextView(context)
            textView.setTextColor(textColor)
            textView.textSize = textSize
            textView.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            textView.gravity = Gravity.CENTER
            // 加入TextView动态数组
            textViewList.add(textView)

            val layoutParamsTextView = RelativeLayout.LayoutParams(itemSize.toInt(), itemSize.toInt())
            relativeLayout.addView(textView, layoutParamsTextView)

            val layoutParamsLinearLayout = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (codeBg == FLAT) {
                if (i != 0) {
                    layoutParamsLinearLayout.marginStart = 5.dp
                }
            } else {
                if (i == 0) {
                    layoutParamsLinearLayout.marginStart = (-4).dp
                    layoutParamsLinearLayout.marginEnd = (-8.5f).dp
                } else if (i == inputCount - 1) {
                    layoutParamsLinearLayout.marginStart = (-8.5f).dp
                    layoutParamsLinearLayout.marginEnd = (-4).dp
                } else {
                    layoutParamsLinearLayout.marginStart = (-8.5f).dp
                    layoutParamsLinearLayout.marginEnd = (-8.5f).dp
                }
            }
            linearLayout.addView(relativeLayout, layoutParamsLinearLayout)
        }
        val params = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        this.addView(linearLayout, params)
        editText = EditText(context)
        editText?.includeFontPadding = false
        editText?.background = null
        editText?.isCursorVisible = false
        editText?.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(1))
        editText?.isFocusable = true
        editText?.isFocusableInTouchMode = true
        editText?.addTextChangedListener(onTextChanged = { s: CharSequence?, _: Int, _: Int, _: Int ->
            if (disable) {
                return@addTextChangedListener
            }
            if (content.length < inputCount && s?.length == 1) {
                content.append(s)
                if (PASSWORD == inputType) {
                    textViewList[content.length - 1].text = "●"
                } else {
                    textViewList[content.length - 1].text = s
                }
                if (isCompleteText()) {
                    listener?.onCodeComplete(editText, content.toString())
                }
            }
            disable = true
            editText?.setText("")
            disable = false
        })
        editText?.setOnKeyListener(OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_UP) {
                if (content.isEmpty()) {
                    return@OnKeyListener true
                }
                textViewList[content.length - 1].text = ""
                content.delete(content.length - 1, content.length)
                return@OnKeyListener true
            }
            false
        })
        editText?.inputType = InputType.TYPE_CLASS_NUMBER
        this.addView(editText, params)
        setOnClickListener {
            (editText?.context?.getSystemService() as? InputMethodManager)?.apply {
                editText?.requestFocus()
                showSoftInput(editText, 0)
            }
        }
    }

    fun clear() {
        content.clear()
        editText?.setText("")
        for (tv in textViewList) {
            tv.text = ""
        }
    }

    fun isCompleteText(): Boolean {
        return content.length == inputCount
    }

    fun focus(): View {
        return editText!!
    }

    fun setOnCodeCompleteListener(listener: OnCodeCompleteListener) {
        this.listener = listener
    }

    interface OnCodeCompleteListener {
        fun onCodeComplete(view: View?, code: String)
    }
}