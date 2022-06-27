package com.fzm.chat.biz.bean

import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/08/17
 * Description:
 */
data class ModuleState(
    /**
     * 模块名
     */
    val name: String,
    /**
     * 模块启用状态
     */
    val isEnabled: Boolean,
    /**
     * 模块请求域名
     */
    val endPoints: List<String>
) : Serializable