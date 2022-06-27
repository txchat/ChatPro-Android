package com.fzm.arch.connection

/**
 * @author zhengjy
 * @since 2020/12/08
 * Description:
 */
interface Disposable {

    /**
     * 清除用户相关的状态等数据
     */
    fun dispose()
}