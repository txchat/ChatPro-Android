package com.fzm.chat.di

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.graphics.drawable.IconCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.liveData
import com.alibaba.android.arouter.facade.annotation.Route
import com.fzm.chat.R
import com.fzm.chat.biz.bean.UpdateInfo
import com.fzm.chat.biz.bus.BusEvent
import com.fzm.chat.biz.bus.LiveDataBus
import com.fzm.chat.biz.bus.event.EndPointLoginEvent
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.main.MainService
import com.fzm.chat.router.route
import com.fzm.chat.ui.ChatUpdateDialogFragment
import com.fzm.chat.ui.QRCodeActivity
import com.fzm.chat.ui.QRScanActivity
import com.fzm.chat.utils.ShortcutHelper
import com.fzm.update.interfaces.IUpdateInfo
import com.zjy.architecture.data.Result
import com.zjy.architecture.di.rootScope
import com.zjy.architecture.ext.apiCall
import com.zjy.architecture.ext.versionCode
import com.zjy.architecture.net.HttpResult
import dtalk.biz.Biz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.jessyan.retrofiturlmanager.RetrofitUrlManager
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * @author zhengjy
 * @since 2021/02/04
 * Description:
 */
@Route(path = MainModule.SERVICE)
class MainServiceImpl : MainService {

    private lateinit var mContext: Context

    private val service = rootScope.get<Retrofit>().create(UpdateService::class.java)
    private val delegate by rootScope.inject<LoginDelegate>()

    private var isCheckingUpdate = false
    private var isDialogShowing = false

    override fun addInnateDynamicShortcuts() {
        ShortcutHelper.addDynamicShortcut(
            context = mContext,
            id = MainModule.QR_CODE,
            label = mContext.getString(R.string.chat_title_my_qr_code),
            icon = IconCompat.createWithResource(mContext, R.drawable.icon_home_qr),
            intent = Intent(mContext, QRCodeActivity::class.java).apply {
                action = Intent.ACTION_DEFAULT
            }
        )
        ShortcutHelper.addDynamicShortcut(
            context = mContext,
            id = MainModule.QR_SCAN,
            label = mContext.getString(R.string.chat_home_action_scan),
            icon = IconCompat.createWithResource(mContext, R.drawable.icon_home_scan),
            intent = Intent(mContext, QRScanActivity::class.java).apply {
                action = Intent.ACTION_DEFAULT
            }
        )
    }

    override fun removeAllDynamicShortcuts() {
        ShortcutHelper.removeAllDynamicShortcuts(mContext)
    }

    override fun addContactShortcut(address: String, channelType: Int, name: String, avatar: String): String {
        ShortcutHelper.addContactShortcut(mContext, address, channelType, name, avatar)
        return address
    }

    override fun addContactShortcut(address: String, channelType: Int, name: String, icon: IconCompat): String {
        ShortcutHelper.addContactShortcut(mContext, address, channelType, name, icon)
        return address
    }

    override fun checkUpdate(activity: FragmentActivity, complete: ((Boolean, String) -> Unit)?) {
        if (isCheckingUpdate) return
        isCheckingUpdate = true
        liveData<Result<UpdateInfo>> {
            emit(apiCall { service.checkUpdate(mapOf("versionCode" to activity.versionCode)) })
        }.observe(activity) { result ->
            if (result.isSucceed()) {
                val data = result.data()
                if (data.getVersionCode() > activity.versionCode) {
                    showUpdateDialog(activity, data, complete)
                } else {
                    complete?.invoke(true, activity.getString(R.string.chat_update_latest_version))
                }
            } else {
                val msg = result.error().message ?: activity.getString(R.string.arch_error_unknown)
                complete?.invoke(false, msg)
            }
            isCheckingUpdate = false
        }
    }

    private fun showUpdateDialog(
        activity: FragmentActivity,
        updateInfo: IUpdateInfo,
        complete: ((Boolean, String) -> Unit)?
    ) {
        if (isDialogShowing) return
        isDialogShowing = true
         val updateDialog by route<ChatUpdateDialogFragment>(
            MainModule.UPDATE_DIALOG,
            Bundle().apply {
                putString("title", activity.getString(R.string.app_name))
                putInt("icon", R.drawable.ic_notification)
                putSerializable("info", updateInfo)
            }
        )
        updateDialog?.setOnDismissListener {
            isDialogShowing = false
            complete?.invoke(true, "")
        }
        updateDialog?.show(activity.supportFragmentManager, "UPDATE_DIALOG")
    }

    override suspend fun onOtherEndPointLogin(deviceName: String, datetime: Long, deviceType: Int) {
        withContext(Dispatchers.Main) {
            val isMobile = deviceType == Biz.Device.Android_VALUE || deviceType == Biz.Device.iOS_VALUE
            if (isMobile) {
                LiveDataBus.of(BusEvent::class.java).endPointLogin().setValue(EndPointLoginEvent(deviceName, datetime))
            }
        }
    }

    override fun init(context: Context?) {
        mContext = context!!
    }

    interface UpdateService {

        @JvmSuppressWildcards
        @Headers(value = [RetrofitUrlManager.DOMAIN_NAME_HEADER + ChatConst.CENTRALIZED_DOMAIN])
        @POST("/app/version/check")
        suspend fun checkUpdate(@Body map: Map<String, Any>): HttpResult<UpdateInfo>
    }
}