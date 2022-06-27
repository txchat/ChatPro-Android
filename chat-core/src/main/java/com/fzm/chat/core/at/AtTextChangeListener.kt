package com.fzm.chat.core.at

/**
 * @author zhengjy
 * @since 2019/08/19
 * Description:
 */
interface AtTextChangeListener {

    fun onTextAdd(content: String?, start: Int, length: Int)

    fun onTextDelete(start: Int, length: Int)
}