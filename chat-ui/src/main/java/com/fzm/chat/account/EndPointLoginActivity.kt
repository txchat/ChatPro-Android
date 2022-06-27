package com.fzm.chat.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.router.main.MainModule
import com.fzm.widget.dialog.EasyDialog
import com.zjy.architecture.ext.format
import com.zjy.architecture.util.ActivityUtils

/**
 * @author zhengjy
 * @since 2022/03/11
 * Description:
 */
@Route(path = MainModule.END_POINT_LOGIN)
class EndPointLoginActivity : AppCompatActivity() {

    @JvmField
    @Autowired
    var deviceName: String = ""

    @JvmField
    @Autowired
    var datetime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ARouter.getInstance().inject(this)
        EasyDialog.Builder()
            .setHeaderTitle("退出通知")
            .setContent("你的账号于${datetime.format("yyyy/MM/dd HH:mm")}在${deviceName}设备上登录，如果不是你的操作，可能你的密码或助记词已经泄露，请尽快修改密码或创建新助记词账户使用。")
            .setBottomRightText("知道了")
            .setCancelable(false)
            .setBottomRightClickListener {
                it.dismiss()
                ActivityUtils.popUpTo("", true)
                ARouter.getInstance().build(MainModule.CHOOSE_LOGIN).navigation(this)
            }
            .create(this)
            .show()
    }
}