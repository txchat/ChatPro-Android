package com.fzm.chat.ui

import android.Manifest
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.biz.base.AppConfig
import com.fzm.chat.biz.base.AppConst
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.base.shareCode
import com.fzm.chat.biz.utils.defaultCustomTabIntent
import com.fzm.chat.biz.utils.onExplain
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.databinding.ActivityQrCodeBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.vm.QRCodeViewModel
import com.king.zxing.util.CodeUtils
import com.permissionx.guolindev.PermissionX
import com.zjy.architecture.ext.clipboardManager
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.isAndroidQ
import com.zjy.architecture.util.other.BarUtils
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author zhengjy
 * @since 2021/01/06
 * Description:
 */
@Route(path = MainModule.QR_CODE, extras = AppConst.NEED_LOGIN)
class QRCodeActivity : BizActivity(), View.OnClickListener {

    @JvmField
    @Autowired
    var content: String? = null

    @JvmField
    @Autowired
    var channelType = 0

    private val viewModel by viewModel<QRCodeViewModel>()

    private val binding by init<ActivityQrCodeBinding>()

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        BarUtils.setStatusBarColor(this, resources.getColor(R.color.biz_color_accent), 0)
        BarUtils.setStatusBarLightMode(this, false)
        window?.navigationBarColor = resources.getColor(R.color.biz_color_primary)
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        if (channelType == ChatConst.PRIVATE_CHANNEL) {
            binding.ctbTitle.setTitle(getString(R.string.chat_title_my_qr_code))
            viewModel.current.observe(this) {
                binding.ivAvatar.load(it.avatar, R.mipmap.default_avatar_big)
                binding.tvName.text = it.nickname
                binding.tvUid.text = it.address
                content = it.address
                val bitmap = (ContextCompat.getDrawable(this, R.mipmap.ic_bitmap_chat33) as BitmapDrawable?)?.bitmap
                val url = AppConfig.shareCode(content)
                binding.ivMyQr.setImageBitmap(CodeUtils.createQRCode(url, 350, bitmap))
            }
            binding.tvTips.text = getString(R.string.chat_tips_qr_code_friend, getString(R.string.app_name))
        } else {

        }
        binding.tvDownloadUrl.text = "应用下载：${AppConfig.APP_DOWNLOAD_URL}"
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.saveResult.observe(this) {
            if (it != null) {
                toast(R.string.chat_tips_img_saved)
            } else {
                toast(R.string.chat_tips_img_not_exist)
            }
        }
    }

    override fun initData() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressed() }
    }

    override fun setEvent() {
        binding.tvDownloadUrl.setOnClickListener(this)
        binding.tvCopyAddress.setOnClickListener(this)
        binding.llShareChat33.setOnClickListener(this)
        binding.llSave.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_download_url -> {
                val data = ClipData.newPlainText("DownloadUrl", AppConfig.APP_DOWNLOAD_URL)
                clipboardManager?.setPrimaryClip(data)
                toast(R.string.chat_tips_copy_download_url)
                defaultCustomTabIntent(this).launchUrl(this, Uri.parse(AppConfig.APP_DOWNLOAD_URL))
            }
            R.id.tv_copy_address -> {
                val data = ClipData.newPlainText("Address", content)
                clipboardManager?.setPrimaryClip(data)
                toast(R.string.chat_tips_copy_address)
            }
            R.id.ll_share_chat33 -> {

            }
            R.id.ll_save -> {
                if (isAndroidQ) {
                    viewModel.saveQRCode(this, getShareBitmap())
                } else {
                    PermissionX.init(this)
                        .permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .onExplain(this)
                        .request { allGranted, _, _ ->
                            if (allGranted) {
                                viewModel.saveQRCode(this, getShareBitmap())
                            } else {
                                this.toast(com.fzm.chat.biz.R.string.biz_permission_not_granted)
                            }
                        }
                }
            }
        }
    }

    private fun getShareBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(binding.rlShareView.width, binding.rlShareView.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        binding.rlShareView.draw(canvas)
        return bitmap
    }
}