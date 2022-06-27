package com.fzm.chat.ui

import android.view.View
import android.view.WindowManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.databinding.ActivityLargeTextBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.notchSupport
import com.zjy.architecture.util.other.BarUtils

/**
 * @author zhengjy
 * @since 2021/12/21
 * Description:
 */
@Route(path = MainModule.LARGE_TEXT)
class LargeTextActivity : BizActivity() {

    @JvmField
    @Autowired
    var message: ChatMessage? = null

    @JvmField
    @Autowired
    var isRef: Boolean = false

    private val binding by init<ActivityLargeTextBinding>()

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        notchSupport(window)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        binding.tvText.apply {
            setPadding(
                paddingLeft,
                paddingTop + BarUtils.getStatusBarHeight(instance),
                paddingRight,
                paddingBottom
            )
        }

        val msg = if (isRef) message?.reference?.refMsg else message

        if (msg != null) {
            binding.tvText.text = msg.msg.content
        }
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.root.setOnClickListener { onBackPressed() }
        binding.tvText.setOnClickListener { onBackPressed() }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.biz_zoom_down_in, R.anim.biz_zoom_down_out)
    }
}