package com.fzm.chat.ui

import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.widget.UpdateDialog
import com.fzm.update.UpdateDialogFragment
import com.fzm.update.interfaces.IUpdateDialog

/**
 * @author zhengjy
 * @since 2021/04/08
 * Description:聊天更新弹窗
 */
@Route(path = MainModule.UPDATE_DIALOG)
class ChatUpdateDialogFragment : UpdateDialogFragment() {

    override fun createDialog(): IUpdateDialog {
        return UpdateDialog(requireContext())
    }
}