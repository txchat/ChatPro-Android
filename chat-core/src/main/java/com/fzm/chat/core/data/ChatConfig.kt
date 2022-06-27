package com.fzm.chat.core.data

import com.fzm.chat.core.BuildConfig
import com.zjy.architecture.util.AbstractConfig

/**
 * @author zhengjy
 * @since 2020/12/14
 * Description:
 */
object ChatConfig {

    private object ChatDev : AbstractConfig("chat-dev.properties")
    private object ChatPro : AbstractConfig("chat-pro.properties")

    val config: AbstractConfig = if (BuildConfig.DEVELOP) ChatDev else ChatPro

    /**
     * 中心化服务器地址，用于支持发现服务，更新等接口
     */
    val CENTRALIZED_SERVER = config["CENTRALIZED_SERVER"]

    /**
     * 每页数据数量
     */
    val PAGE_SIZE = config["PAGE_SIZE"].toInt()

    /**
     * 最大语音录音时长
     */
    val MAX_AUDIO_DURATION = config["MAX_AUDIO_DURATION"].toInt()

    /**
     * 代扣地址私钥
     */
    val NO_BALANCE_PRIVATE_KEY = config["NO_BALANCE_PRIVATE_KEY"]

    /**
     * 是否启用消息加密
     */
    val ENCRYPT = config["ENCRYPT"].toBoolean()

    /**
     * 是否启用文件加密
     */
    val FILE_ENCRYPT = config["FILE_ENCRYPT"].toBoolean()

    /**
     * 重新编辑超时时间（撤回多久以后不能重新编辑）
     */
    const val REEDIT_TIMEOUT = 10 * 60 * 1000L

    /**
     * 当前聊天对象
     */
    var CURRENT_TARGET: String? = null
}