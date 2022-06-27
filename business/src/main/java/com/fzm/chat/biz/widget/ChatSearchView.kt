package com.fzm.chat.biz.widget

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.transition.ChangeBounds
import android.transition.Slide
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.fzm.chat.biz.R
import com.fzm.chat.biz.databinding.ChatSearchViewBinding
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.setVisible
import kotlinx.coroutines.*

/**
 * @author zhengjy
 * @since 2019/09/17
 * Description:通用搜索View
 */
class ChatSearchView : RelativeLayout {

    private var search: OnTextChangeListener? = null
    private var cancel: OnSearchCancelListener? = null
    private var hintText: String? = null
    private var maxWords: Int = MAX_LENGTH
    private var searchDelay: Long = DEFAULT_DELAY
    private var showCancel: Boolean = true
    private var enable: Boolean = true
    private var isExpand: Boolean = false
    private lateinit var binding: ChatSearchViewBinding

    private var keywordsJob: Job? = null

    companion object {
        const val MAX_LENGTH = 40

        const val DEFAULT_DELAY = 300L
    }

    constructor(context: Context) : super(context, null) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ChatSearchView)
        hintText = ta.getString(R.styleable.ChatSearchView_hint)
        maxWords = ta.getInteger(R.styleable.ChatSearchView_maxWords, MAX_LENGTH)
        searchDelay =
            ta.getInteger(R.styleable.ChatSearchView_searchDelay, DEFAULT_DELAY.toInt()).toLong()
        showCancel = ta.getBoolean(R.styleable.ChatSearchView_showCancel, true)
        ta.recycle()

        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        binding = ChatSearchViewBinding.inflate(LayoutInflater.from(context), this, true)
        if (!hintText.isNullOrEmpty()) {
            binding.etSearch.hint = hintText
        }
        binding.tvCancel.setVisible(showCancel)
        binding.tvCancel.setOnClickListener {
            cancel?.onSearchCancel()
        }
        binding.etSearch.filters = arrayOf(InputFilter.LengthFilter(maxWords))
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable) {
                if (enable) {
                    keywordsJob?.cancel()
                    keywordsJob = GlobalScope.launch(Dispatchers.Main) {
                        // 搜索延迟需要在布局文件中设置
                        delay(searchDelay)
                        search?.onTextChange(s.toString().trim())
                    }
                }
            }
        })
    }

    /**
     * 设置伸展状态时的布局
     */
    fun expand() {
        beginDelayedTransition(this)
        val params = layoutParams
        params.width = LayoutParams.MATCH_PARENT
        layoutParams = params
        isExpand = true
    }

    /**
     * 设置收缩状态时的布局
     */
    fun reduce() {
        beginDelayedTransition(this)
        val params = layoutParams
        params.width = 0.dp
        layoutParams = params
        isExpand = false
    }

    private fun beginDelayedTransition(view: ViewGroup) {
        val set = TransitionSet().apply {
            addTransition(ChangeBounds())
            addTransition(Slide(Gravity.END))
            duration = 200
        }
        TransitionManager.beginDelayedTransition(view, set)
    }

    private fun enableTextWatcher(enable: Boolean) {
        this.enable = enable
    }

    /**
     * 用于将EditText暴露给外部来获取焦点
     */
    fun getFocusView(): View {
        return binding.etSearch
    }

    fun getTransitionView(): View {
        return binding.llSearch
    }

    fun getText(): String {
        return binding.etSearch.text.toString().trim()
    }

    fun setText(text: CharSequence?) {
        binding.etSearch.setText(text)
        binding.etSearch.setSelection(text?.length ?: 0)
    }

    fun setTextWithoutWatcher(text: CharSequence?) {
        enableTextWatcher(false)
        setText(text)
        enableTextWatcher(true)
    }

    fun setHint(text: String?) {
        binding.etSearch.hint = text
    }

    fun isExpand(): Boolean {
        return isExpand
    }

    fun onBackPressed(): Boolean {
        if (isExpand()) {
            cancel()
            return true
        }
        return false
    }

    fun cancel() {
        cancel?.onSearchCancel()
    }

    fun setOnTextChangeListener(listener: OnTextChangeListener) {
        this.search = listener
    }

    fun setOnSearchCancelListener(listener: OnSearchCancelListener) {
        this.cancel = listener
    }

    interface OnTextChangeListener {
        fun onTextChange(s: String)
    }

    interface OnSearchCancelListener {
        fun onSearchCancel()
    }
}
