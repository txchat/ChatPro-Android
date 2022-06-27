package com.fzm.chat.core.data.bean

/**
 * @author zhengjy
 * @since 2020/12/22
 * Description:
 */
interface Sortable {
    /**
     * 获取排序用的首字，如徐，王
     *
     * @return
     */
    fun getFirstChar(): String

    /**
     * 获取排序用的首字母，如徐(X)，王(W)
     *
     * @return
     */
    fun getFirstLetter(): String

    /**
     * 获取全拼，如张三(ZHANGSAN)
     *
     * @return
     */
    fun getLetters(): String

    /**
     * 优先级，优先级高的直接排在最前面
     *
     * @return
     */
    fun priority(): Int
}