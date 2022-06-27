package com.fzm.chat.biz.utils

import android.content.Context
import kotlin.random.Random

/**
 * @author zhengjy
 * @since 2021/08/17
 * Description:
 */
object ColorUtil {

    /**
     * 随机生成一个币种的颜色
     */
    @Deprecated("币种直接显示币种对应的图标")
    fun generateColorPair(context: Context) : Pair<Int, Int> {
        val type = Random(System.currentTimeMillis()).nextInt(6) + 1
        val drawable = context.resources.getIdentifier("bg_coin$type", "drawable", context.packageName)
        val color = context.resources.getIdentifier("biz_wallet_coin$type", "color", context.packageName)
        return drawable to color
    }
}