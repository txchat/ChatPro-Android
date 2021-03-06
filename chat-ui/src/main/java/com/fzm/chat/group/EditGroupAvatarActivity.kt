package com.fzm.chat.group

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.forjrking.lubankt.Luban
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.base.BizFileProvider
import com.fzm.chat.biz.utils.PERMISSION_CAMERA
import com.fzm.chat.biz.utils.cropImage
import com.fzm.chat.biz.utils.onExplain
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.ActivityEditGroupAvatarBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.utils.GlideEngine
import com.fzm.chat.widget.OnSelectListener
import com.fzm.chat.widget.SelectPhotoDialog
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.listener.OnResultCallbackListener
import com.permissionx.guolindev.PermissionX
import com.yalantis.ucrop.UCrop
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.toast
import com.zjy.architecture.util.isContent
import com.zjy.architecture.util.other.BarUtils
import com.zjy.architecture.util.toUri
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File

/**
 * @author zhengjy
 * @since 2021/06/02
 * Description:
 */
@Route(path = MainModule.EDIT_GROUP_AVATAR)
class EditGroupAvatarActivity: BizActivity() {

    companion object {
        const val REQUEST_TAKE_PHOTO = 100
    }

    @JvmField
    @Autowired
    var groupId: Long = 0L

    @JvmField
    @Autowired
    var canEdit: Boolean = false

    @JvmField
    @Autowired
    var avatar: String? = null

    private val viewModel by viewModel<GroupViewModel>()

    private val binding by init { ActivityEditGroupAvatarBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        BarUtils.setStatusBarColor(
            this,
            ContextCompat.getColor(this, android.R.color.transparent),
            0
        )
        BarUtils.addMarginTopEqualStatusBarHeight(this, binding.ctbTitle)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        binding.ctbTitle.setRightVisible(canEdit)
        binding.pvImage.load(avatar, R.mipmap.default_avatar_room_big)
    }

    override fun initData() {
        viewModel.loading.observe(this) { setupLoading(it) }
        viewModel.editAvatarResult.observe(this) {
            toast(R.string.chat_tip_avatar_edit_success)
            binding.pvImage.load(it, R.mipmap.default_avatar_room_big)
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressedSupport() }
        binding.ctbTitle.setOnRightClickListener { showOptions() }
    }

    private fun showOptions() {
        SelectPhotoDialog.create(this, object : OnSelectListener {
            override fun onTakePhoto() {
                PermissionX.init(instance)
                    .permissions(PERMISSION_CAMERA)
                    .onExplain(instance)
                    .request { allGranted, _, _ ->
                        if (allGranted) {
                            ARouter.getInstance().build(MainModule.CAPTURE)
                                .withInt("mode", 1)
                                .navigation(instance, REQUEST_TAKE_PHOTO)
                        } else {
                            instance.toast(com.fzm.chat.biz.R.string.biz_permission_not_granted)
                        }
                    }
            }

            override fun onSelectFromGallery() {
                PictureSelector.create(instance)
                    .openGallery(PictureMimeType.ofImage())
                    .maxSelectNum(1)
                    .isCompress(false)
                    .isCamera(false)
                    .theme(R.style.chat_picture_style)
                    .imageEngine(GlideEngine.createGlideEngine())
                    .forResult(object : OnResultCallbackListener<LocalMedia> {
                        override fun onResult(result: MutableList<LocalMedia>?) {
                            if (result.isNullOrEmpty()) {
                                toast(R.string.chat_tips_img_not_exist)
                                return
                            }
                            val media = result[0]
                            if (media.path == null) {
                                toast(R.string.chat_tips_img_not_exist)
                                return
                            }
                            if (media.path.isContent()) {
                                cropImage(media.path.toUri())
                            } else {
                                cropImage(File(media.path).toUri(instance, BizFileProvider.authority(instance)))
                            }
                        }

                        override fun onCancel() {

                        }
                    })
            }
        }).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_PHOTO) {
                val uri = data?.getParcelableExtra<Uri>("result")
                if (uri == null) {
                    toast(R.string.chat_tips_img_not_exist)
                    return
                }
                cropImage(uri)
            } else if (requestCode == UCrop.REQUEST_CROP) {
                val uri = data?.getParcelableExtra<Uri>(UCrop.EXTRA_OUTPUT_URI)
                if (uri == null) {
                    toast(R.string.chat_tips_img_not_exist)
                    return
                }
                Luban.with(this).load(uri)
                    .setOutPutDir(Utils.getImageCacheDir(this))
                    .compressObserver {
                        onStart = {
                            loading(true)
                        }
                        onSuccess = {
                            viewModel.editGroupAvatar(groupId, it.absolutePath)
                        }
                        onError = { e, _ ->
                            toast(e.message.toString())
                        }
                        onCompletion = {
                            dismiss()
                        }
                    }
                    .launch()
            } else if (requestCode == UCrop.RESULT_ERROR) {
                val throwable = data?.getSerializableExtra(UCrop.EXTRA_ERROR) as? Throwable
                toast(throwable?.message ?: "???????????????????????????")
            }
        }
    }
}