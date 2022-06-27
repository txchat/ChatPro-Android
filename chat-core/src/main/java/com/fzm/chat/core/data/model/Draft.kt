package com.fzm.chat.core.data.model

import com.fzm.chat.core.at.AtBlock
import com.fzm.chat.core.data.po.Reference
import java.io.Serializable

/**
 * @author zhengjy
 * @since 2021/11/16
 * Description:
 */
data class Draft(
    /**
     * 草稿文字内容
     */
    val text: String,
    /**
     * 草稿的@信息
     */
    val atInfo: Map<String, AtBlock>?,
    /**
     * 引用信息
     */
    val reference: Reference?,
    /**
     * 草稿保存时间
     */
    val time: Long,
) : Serializable