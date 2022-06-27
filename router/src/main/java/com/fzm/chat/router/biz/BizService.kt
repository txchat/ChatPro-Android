package com.fzm.chat.router.biz

import com.alibaba.android.arouter.facade.template.IProvider

/**
 * @author zhengjy
 * @since 2020/12/04
 * Description:
 */
interface BizService : IProvider {

    /**
     * 获取模块状态
     */
    fun fetchModuleState()

    /**
     * 获取服务器节点
     */
    fun fetchServerList()

    /**
     * 金额放大倍数
     */
    fun getAmountScale(): Int
}