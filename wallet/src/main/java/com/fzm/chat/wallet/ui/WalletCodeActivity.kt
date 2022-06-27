package com.fzm.chat.wallet.ui

import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.router.wallet.WalletModule
import com.fzm.chat.wallet.R
import com.fzm.chat.wallet.data.qrCode
import com.fzm.chat.wallet.databinding.ActivityWalletCodeBinding
import com.fzm.wallet.sdk.db.entity.Coin
import com.king.zxing.util.CodeUtils
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.clipboardManager
import com.zjy.architecture.ext.dp
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.other.BarUtils
import org.koin.android.ext.android.inject

/**
 * @author zhengjy
 * @since 2021/08/10
 * Description:
 */
@Route(path = WalletModule.WALLET_CODE)
class WalletCodeActivity : BizActivity() {

    @JvmField
    @Autowired
    var coin: Coin? = null

    private val currentCoin: Coin
        get() = coin ?: throw Exception("coin is empty")

    private val delegate by inject<LoginDelegate>()

    private val binding by init { ActivityWalletCodeBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        BarUtils.setStatusBarColor(this, resources.getColor(R.color.biz_wallet_accent), 0)
        BarUtils.setStatusBarLightMode(this, true)
        window?.navigationBarColor = resources.getColor(R.color.biz_wallet_accent)
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        val address = currentCoin.address
        binding.tvAddress.text = address
        Glide.with(this).asBitmap().load(delegate.current.value?.avatar)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    binding.ivCode.setImageBitmap(CodeUtils.createQRCode(currentCoin.qrCode, 170.dp, resource))
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    val bitmap = (ContextCompat.getDrawable(
                        instance,
                        R.mipmap.default_avatar_round
                    ) as BitmapDrawable?)?.bitmap
                    binding.ivCode.setImageBitmap(CodeUtils.createQRCode(currentCoin.qrCode, 170.dp, bitmap))
                }
            })
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.tvCopy.setOnClickListener {
            val data = ClipData.newPlainText("address", currentCoin.address)
            clipboardManager?.setPrimaryClip(data)
            toast("地址已复制到剪贴板")
        }
    }
}