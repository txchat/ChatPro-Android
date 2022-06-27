package com.fzm.chat.core.data.enum

/**
 * @author zhengjy
 * @since 2021/12/02
 * Description:
 */
enum class CodeType(val value: String) {
    /**
     * 快速登录
     */
    QUICK("quick"),

    /**
     * 绑定账户
     */
    BIND("bind"),

    /**
     * 导出本地账户
     */
    EXPORT("export")

}