package com.fzm.chat.router.main

import androidx.core.graphics.drawable.IconCompat
import androidx.fragment.app.FragmentActivity
import com.alibaba.android.arouter.facade.template.IProvider

/**
 * @author zhengjy
 * @since 2021/02/04
 * Description:
 */
interface MainService : IProvider {

    /**
     * 添加登录后固有的Shortcuts
     */
    fun addInnateDynamicShortcuts()

    /**
     * 移除所有动态添加的Shortcuts
     */
    fun removeAllDynamicShortcuts()

    fun addContactShortcut(address: String, channelType: Int, name: String, avatar: String): String

    fun addContactShortcut(address: String, channelType: Int, name: String, icon: IconCompat): String

    /**
     * 检查app更新
     */
    fun checkUpdate(activity: FragmentActivity, complete: ((Boolean, String) -> Unit)? = null)

    /**
     * 其他终端登录
     *
     * @param deviceName    设备名称
     * @param datetime      时间
     * @param deviceType    设备类型，   Android = 0; iOS = 1; Windows = 2; Linux = 3; MacOS = 4;
     *
     */
    suspend fun onOtherEndPointLogin(deviceName: String, datetime: Long, deviceType: Int)
}