package com.fzm.chat.router.push

import com.alibaba.android.arouter.facade.template.IProvider

/**
 * @author zhengjy
 * @since 2021/04/13
 * Description:
 */
interface PushService : IProvider {

    /**
     * 启用离线推送
     */
    fun enablePush(callback: ((Boolean) -> Unit)? = null)

    /**
     * 禁用离线推送
     */
    fun disablePush(callback: ((Boolean) -> Unit)? = null)
}