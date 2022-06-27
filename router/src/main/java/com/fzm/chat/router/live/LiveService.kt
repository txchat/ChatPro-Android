package com.fzm.chat.router.live

import android.content.Context
import com.alibaba.android.arouter.facade.template.IProvider

/**
 * @author zx
 * @since 2021/10/14
 * Description:
 */
interface LiveService : IProvider {

    companion object {
        val FROM_SCAN = 1
        val FROM_ACCOUNT = 2
    }

    suspend fun login(context: Context,type:Int,codeValue:String): String?

}