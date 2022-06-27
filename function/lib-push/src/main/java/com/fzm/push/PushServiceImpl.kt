package com.fzm.push

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.router.push.PushModule
import com.fzm.chat.router.push.PushService

/**
 * @author zhengjy
 * @since 2021/04/13
 * Description:
 */
@Route(path = PushModule.SERVICE)
class PushServiceImpl : PushService {

    override fun enablePush(callback: ((Boolean) -> Unit)?) {
        PushManager.enablePush(callback)
    }

    override fun disablePush(callback: ((Boolean) -> Unit)?) {
        PushManager.disablePush(callback)
    }

    override fun init(context: Context?) {

    }
}