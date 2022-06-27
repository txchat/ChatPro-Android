package com.fzm.chat.router.shop

import android.content.Context

/**
 * @author zhengjy
 * @since 2021/10/18
 * Description:
 */
interface ShopService {

    /**
     * 打开商城主页面
     */
    fun openShopHomePage(context: Context?)

    /**
     * 打开商城划转页面
     */
    fun openShopTransferPage(context: Context?, symbol: String)
}