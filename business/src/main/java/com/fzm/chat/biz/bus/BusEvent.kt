package com.fzm.chat.biz.bus

import com.fzm.chat.biz.bus.event.ChangeTabEvent
import com.fzm.chat.biz.bus.event.EndPointLoginEvent

/**
 * @author zhengjy
 * @since 2019/09/30
 * Description:[LiveDataBus]发送的事件类型
 */
interface BusEvent {

    /**
     * 主页面tab切换事件
     */
    fun changeTab(): ChatEventData<ChangeTabEvent>

    /**
     * 未读消息总数，由SessionFragment发出消息
     */
    fun unreadMessageNum(): ChatEventData<Int>

    /**
     * 会话页面展开状态
     */
    fun sessionPullState(): ChatEventData<Int>

    /**
     * 其他终端登录
     */
    fun endPointLogin(): ChatEventData<EndPointLoginEvent?>
}