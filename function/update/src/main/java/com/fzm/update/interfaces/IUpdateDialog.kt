package com.fzm.update.interfaces

import android.view.View

/**
 * @author zhengjy
 * @since 2020/08/11
 * Description:
 */
interface IUpdateDialog {

    /**
     * 根据更新信息设置Dialog的显示
     */
    fun setupDialog(updateInfo: IUpdateInfo)

    /**
     * 获取确认更新按钮
     */
    fun getUpdateButton(): View?

    /**
     * 获取取消更新按钮
     */
    fun getRefuseButton(): View?

    /**
     * 获取安装更新按钮
     */
    fun getInstallButton(): View?

    /**
     * 获取更新下载进度条
     */
    fun onProgress(progress: Int)

    /**
     * 下载完成后Dialog样式可能发生一些变化
     *
     * @param success 下载更新包是否成功
     */
    fun onFinish(success: Boolean)
}