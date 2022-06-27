package com.fzm.chat.widget

import android.app.Dialog
import android.content.ClipData
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import com.fzm.chat.R
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.databinding.DialogExportWordsBinding
import com.zjy.architecture.ext.clipboardManager
import com.zjy.architecture.ext.toast

/**
 * @author zhengjy
 * @since 2019/05/24
 * Description:导出助记词/私钥Dialog
 */
class ExportWordsDialog : Dialog {

    /**
     * 1：助记词
     * 2：私钥
     */
    private var mType = 1
    private var mWords = ""
    private var mContext: Context? = null

    private lateinit var binding: DialogExportWordsBinding

    constructor(context: Context, words: String, type: Int) : super(context) {
        mContext = context
        mWords = words
        mType = type
        init()
    }

    constructor(context: Context, themeResId: Int, words: String, type: Int) : super(context, themeResId) {
        mContext = context
        mWords = words
        mType = type
        init()
    }

    private fun init() {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.attributes = window?.attributes?.apply {
            gravity = Gravity.CENTER
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        binding = DialogExportWordsBinding.inflate(LayoutInflater.from(mContext))
        window?.setContentView(binding.root)
        setCanceledOnTouchOutside(false)
        if (mType == 1) {
            binding.tvTitle.setText(R.string.chat_tips_export_words)
            if (mWords.isNotEmpty()) {
                val first = mWords.substring(0, 1)
                if (first.matches(ChatConst.REGEX_CHINESE.toRegex())) {
                    // 如果是中文助记词，则需要添加空格
                    val sb = StringBuilder()
                    mWords = mWords.replace(" ", "")
                    for (i in mWords.indices) {
                        sb.append(mWords[i])
                        if ((i + 1) % 3 == 0 && i != mWords.length - 1) {
                            if (i + 1 == 9) {
                                // 九个字之后进行换行
                                sb.append("\n")
                            } else {
                                sb.append("  ")
                            }
                        }
                    }
                    mWords = sb.toString()
                }
            }
        } else if (mType == 2) {
            binding.tvTitle.setText(R.string.chat_tips_export_private)
        }
        binding.tvWords.text = mWords
        binding.confirm.setOnClickListener {
            val cm = mContext?.clipboardManager
            val mClipData = ClipData.newPlainText("Label", mWords)
            if (cm != null) {
                cm.setPrimaryClip(mClipData)
                mContext?.toast(R.string.chat_tips_chat_operate4)
            }
        }
        binding.ivClose.setOnClickListener { dismiss() }
    }
}