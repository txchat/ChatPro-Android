package com.fzm.chat.biz.widget

import android.view.View
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * @author zhengjy
 * @since 2021/11/19
 * Description:
 */
class FadePageTransformer : ViewPager.PageTransformer, ViewPager2.PageTransformer {

    companion object {
        /**
         * 隐藏阈值
         */
        const val threshHold = 0.5f
        /**
         * 原地固定，只改变透明度阈值
         */
        const val threshHold2 = 0.1f

        /**
         * 视察系数
         */
        const val parallax = 0.8f
    }

    override fun transformPage(page: View, position: Float) {
        if (position <= -threshHold || position >= threshHold) {
            page.translationX = 0f
            page.alpha = 0f
        } else if (position == 0f) {
            page.translationX = 0f
            page.alpha = 1f
        } else if (abs(position) <= threshHold2) {
            val width = page.measuredWidth
            page.translationX = -width * position
            page.alpha = 1 - abs(position / threshHold)
        } else if (position > threshHold2) {
            val width = page.measuredWidth
            page.translationX = -width * (position - threshHold2) * parallax + (-width * threshHold2)
            page.alpha = 1 - abs(position / threshHold)
        } else if (position < -threshHold2) {
            val width = page.measuredWidth
            page.translationX = -width * (position + threshHold2) * parallax - (-width * threshHold2)
            page.alpha = 1 - abs(position / threshHold)
        }
    }
}