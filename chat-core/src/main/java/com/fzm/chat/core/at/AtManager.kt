package com.fzm.chat.core.at

import android.text.TextWatcher
import android.content.Intent
import android.app.Activity
import com.fzm.chat.core.data.bean.Contact
import android.text.Editable
import android.text.TextUtils

/**
 * @author zhengjy
 * @since 2019/08/19
 * Description:
 */
class AtManager(private val targetId: String) : TextWatcher {

    private val atContactsModel: AtContactsModel = AtContactsModel()

    private var curPos = 0

    private var ignoreTextChange = false

    private var listener: AtTextChangeListener? = null

    private var onOpenAitListListener: OnOpenAitListListener? = null

    fun setTextChangeListener(listener: AtTextChangeListener?) {
        this.listener = listener
    }

    fun setOnOpenAitListListener(listener: OnOpenAitListListener?) {
        onOpenAitListListener = listener
    }

    val aitMembers: List<String>?
        get() = atContactsModel.aitMembers

    val aitInfo: Map<String, AtBlock>
        get() = atContactsModel.aitInfo

    fun restoreFrom(aitInfo: Map<String, AtBlock>?) {
        aitInfo?.also { atContactsModel.restoreFrom(it) }
    }

    fun reset() {
        atContactsModel.reset()
        ignoreTextChange = false
        curPos = 0
    }

    fun setEnableTextChangedListener(enable: Boolean) {
        this.ignoreTextChange = !enable
    }

    interface OnOpenAitListListener {
        /**
         * 检测到输入@符号，需要启动@联系人界面
         */
        fun onOpenAitList()
    }

    /**
     * ------------------------------ 增加@成员 --------------------------------------
     */
    @JvmOverloads
    fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent,
        key: String? = RESULT_DATA,
        useScopeName: Boolean = true
    ) {
        if (requestCode == REQUEST_AIT && resultCode == Activity.RESULT_OK) {
            val contact = data.getSerializableExtra(key) as Contact?
            if (contact != null) {
                val account = contact.getId()
                val name = if (useScopeName) {
                    contact.getScopeName()
                } else {
                    contact.getDisplayName()
                }
                insertAitMemberInner(account, name, curPos, false)
            }
        }
    }

    @JvmOverloads
    fun onUserSelected(contact: Contact?, useRawName: Boolean = true) {
        if (contact != null) {
            val account = contact.getId()
            val name = if (useRawName) {
                contact.getRawName()
            } else {
                contact.getDisplayName()
            }
            insertAitMemberInner(account, name, curPos, false)
        }
    }

    fun insertAitMember(id: String, name: String, start: Int) {
        insertAitMemberInner(id, name, start, true)
    }

    private fun insertAitMemberInner(
        account: String,
        name: String,
        start: Int,
        needInsertAitInText: Boolean
    ) {
        // "\u200b"为不可见字符，"\u2004"区别于普通空格
        val nameWithSpace = name + "\u2004"
        val content = if (needInsertAitInText) "@$nameWithSpace" else nameWithSpace
        if (listener != null) {
            // 关闭监听
            ignoreTextChange = true
            // insert 文本到editText
            listener?.onTextAdd(content, start, content.length)
            // 开启监听
            ignoreTextChange = false
        }

        // update 已有的 aitBlock
        atContactsModel.onInsertText(start, content)
        val index = if (needInsertAitInText) start else start - 1
        // 添加当前到 aitBlock
        atContactsModel.addAitMember(account, nameWithSpace, index)
    }
    /*
     * ------------------------------ editText 监听 --------------------------------------
     */
    /**
     * 当删除尾部空格时，删除一整个segment,包含界面上也删除
     *
     * @param start 字符串变化起始位置
     * @param count 字符串变化长度
     * @return      是否删除了Segment
     */
    private fun deleteSegment(start: Int, count: Int): Boolean {
        if (count != 1) {
            return false
        }
        var result = false
        val segment = atContactsModel.findAitSegmentByEndPos(start)
        if (segment != null) {
            val length = start - segment.start
            if (listener != null) {
                ignoreTextChange = true
                listener?.onTextDelete(segment.start, length)
                ignoreTextChange = false
            }
            atContactsModel.onDeleteText(start, length)
            result = true
        }
        return result
    }

    /**
     * @param editable 变化后的Editable
     * @param start    text 变化区块的起始index
     * @param count    text 变化区块的大小
     * @param delete   是否是删除
     */
    private fun afterTextChanged(editable: Editable?, start: Int, count: Int, delete: Boolean) {
        curPos = if (delete) start else count + start
        if (ignoreTextChange) {
            return
        }
        if (delete) {
            val before = start + count
            if (deleteSegment(before, count)) {
                return
            }
            atContactsModel.onDeleteText(before, count)
        } else {
            if (count <= 0 || editable?.length ?: 0 < start + count) {
                return
            }
            val s = editable?.subSequence(start, start + count) ?: return
            if (s.toString() == "@") {
                // 启动@联系人界面
                if (!TextUtils.isEmpty(targetId)) {
                    onOpenAitListListener?.onOpenAitList()
                }
            }
            atContactsModel.onInsertText(start, s.toString())
        }
    }

    private var editTextStart = 0
    private var editTextCount = 0
    private var editTextBefore = 0
    private var delete = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        delete = count > after
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        editTextStart = start
        editTextCount = count
        editTextBefore = before
    }

    override fun afterTextChanged(s: Editable?) {
        afterTextChanged(s, editTextStart, if (delete) editTextBefore else editTextCount, delete)
    }

    companion object {
        var REQUEST_AIT = 2001
        var RESULT_DATA = "RESULT_DATA"
    }

}