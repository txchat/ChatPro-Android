package com.fzm.chat.setting

import android.view.View
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.databinding.ActivitySettingCenterBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.push.PushModule
import com.fzm.chat.router.push.PushService
import com.fzm.chat.router.route
import com.fzm.widget.SwitchView
import com.zjy.architecture.ext.setVisible
import com.zjy.architecture.util.rom.RomUtils
import org.koin.android.ext.android.inject

/**
 * @author zhengjy
 * @since 2021/04/13
 * Description:
 */
@Route(path = MainModule.SETTING_CENTER)
class SettingCenterActivity : BizActivity(), View.OnClickListener {

    private val delegate by inject<LoginDelegate>()
    private val pushService by route<PushService>(PushModule.SERVICE)

    private val binding by init { ActivitySettingCenterBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override val darkStatusColor: Boolean = true

    override fun initView() {
        binding.newMsg.isOpened = delegate.preference.NEW_MSG_NOTIFY
        binding.newCall.isOpened = delegate.preference.NEW_CALL_NOTIFY
        delegate.preference.msgNotify.observe(this) {
            binding.ivSetMsg.setVisible(!it)
        }
        delegate.preference.callNotify.observe(this) {
            binding.ivSetCall.setVisible(!it)
        }
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressed() }
        binding.newMsg.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
            override fun toggleToOn(view: SwitchView?) {
                pushService?.also {
                    it.enablePush {
                        delegate.preference.NEW_MSG_NOTIFY = true
                        view?.toggleSwitch(true)
                    }
                }
            }

            override fun toggleToOff(view: SwitchView?) {
                pushService?.also {
                    it.disablePush {
                        delegate.preference.NEW_MSG_NOTIFY = false
                        view?.toggleSwitch(false)
                    }
                }
            }
        })
        binding.newCall.setOnStateChangedListener(object : SwitchView.OnStateChangedListener {
            override fun toggleToOn(view: SwitchView?) {
                delegate.preference.NEW_CALL_NOTIFY = true
                view?.toggleSwitch(true)
            }

            override fun toggleToOff(view: SwitchView?) {
                delegate.preference.NEW_CALL_NOTIFY = false
                view?.toggleSwitch(false)
            }
        })
        binding.llSysNewMsg.setOnClickListener(this)
        binding.llSysNewCall.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ll_sys_new_msg -> {
                if (!delegate.preference.SET_MSG_NOTIFY) {
                    delegate.preference.setMsgNotify()
                }
                // channelId暂时不传，否则部分手机系统设置会崩溃
                RomUtils.openNotificationSetting(this, "chatMessage")
            }
            R.id.ll_sys_new_call -> {
                if (!delegate.preference.SET_CALL_NOTIFY) {
                    delegate.preference.setCallNotify()
                }
                RomUtils.openNotificationSetting(this, "rtcCall")
            }
        }
    }
}