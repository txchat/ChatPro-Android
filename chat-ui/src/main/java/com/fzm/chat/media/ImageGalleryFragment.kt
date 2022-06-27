package com.fzm.chat.media

import android.Manifest
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.launcher.ARouter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.fzm.chat.R
import com.fzm.chat.biz.QRCodeHelper
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.utils.ImageUtils
import com.fzm.chat.biz.utils.QRCodeUtils
import com.fzm.chat.biz.utils.onExplain
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.*
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.data.po.MessageSource
import com.fzm.chat.core.glide.ForwardModel
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.FragmentImageGalleryBinding
import com.fzm.chat.adapter.DialogOption
import com.fzm.chat.adapter.DialogOptionAdapter
import com.fzm.chat.core.data.po.toFriendUser
import com.fzm.chat.core.media.DownloadManager2
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.ui.MediaGalleryActivity
import com.fzm.chat.utils.ForwardHelper
import com.fzm.chat.widget.BottomListDialog
import com.permissionx.guolindev.PermissionX
import com.zjy.architecture.data.Result
import com.zjy.architecture.ext.*
import dtalk.biz.Biz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

/**
 * @author zhengjy
 * @since 2021/03/31
 * Description:
 */
class ImageGalleryFragment : BizFragment(), View.OnClickListener {

    companion object {
        fun create(message: ChatMessage?, forwardPos: Int, url: String?, showGallery: Boolean, showOptions: Boolean = true, placeholder: Int = 0): ImageGalleryFragment {
            return ImageGalleryFragment().apply {
                arguments = bundleOf(
                    "message" to message,
                    "forwardPos" to forwardPos,
                    "url" to url,
                    "showGallery" to showGallery,
                    "showOptions" to showOptions,
                    "placeholder" to placeholder
                )
            }
        }
    }

    private var forward: ForwardMsg? = null
    private var message: ChatMessage? = null
    private var forwardPos: Int = -1
    private var url: String? = null
    private var showGallery = true
    private var showOptions = true
    private var placeholder: Int = 0

    private val hideAnimation = AlphaAnimation(1f, 0f).apply { duration = 250 }
    private val fadeOut = Runnable { hideOptions() }

    private val delegate by inject<LoginDelegate>()
    private val contactManager by inject<ContactManager>()

    private var bottomDialog: BottomListDialog? = null

    private var options = mutableListOf<DialogOption>()

    private val binding by init<FragmentImageGalleryBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        message = arguments?.getSerializable("message") as? ChatMessage
        forwardPos = arguments?.getInt("forwardPos", -1) ?: -1
        url = arguments?.getString("url")
        showGallery = arguments?.getBoolean("showGallery") ?: true
        showOptions = arguments?.getBoolean("showOptions") ?: true
        placeholder = arguments?.getInt("placeholder", 0) ?: 0
        if (forwardPos != -1) {
            forward = message?.msg?.forwardLogs?.filterMedia()?.get(forwardPos)
        }
        binding.llOptions.setVisible(showOptions)
        binding.ivGallery.setVisible(showGallery)
    }

    override fun initData() {
        options.add(DialogOption(getString(R.string.biz_save)) { saveMedia(binding.ivDownload) })
        options.add(DialogOption(getString(R.string.chat_action_forward)) { forward() })
        adapter.setData(options)

        val options = RequestOptions()
            .centerInside()
//            .apply {
//                if (placeholder != 0) {
//                    placeholder(placeholder)
//                }
//            }
//            .diskCacheStrategy(DiskCacheStrategy.DATA)
        displayImage(binding.pvImage, options)
        checkDownloadState()
    }

    override fun setEvent() {
        binding.pvImage.setOnViewTapListener { _, _, _ ->
            activity?.onBackPressed()
        }
        binding.ivForward.setOnClickListener(this)
        binding.ivDownload.setOnClickListener(this)
        binding.ivGallery.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        checkDownloadState()
        if (showOptions) {
            binding.llOptions.visible()
            startFadeOut()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.pvImage.removeCallbacks(fadeOut)
    }

    override fun onBackPressedSupport(): Boolean {
        hideOptions()
        return super.onBackPressedSupport()
    }

    private fun checkDownloadState() {
        if (message != null && forwardPos == -1) {
            binding.ivDownload.setVisible(true)
        } else if (forward != null) {
            binding.ivDownload.setVisible(true)
        } else if (!url.isNullOrEmpty()) {
            binding.ivDownload.setVisible(true)
        } else {
            binding.ivDownload.gone()
        }
        binding.ivGallery.setVisible(showGallery && message != null)
    }

    private fun startFadeOut() {
        binding.pvImage.removeCallbacks(fadeOut)
        binding.pvImage.postDelayed(fadeOut, MediaGalleryActivity.FADE_OUT_TIMEOUT)
    }

    private fun hideOptions() {
        binding.llOptions.gone()
        binding.llOptions.startAnimation(hideAnimation)
    }

    private fun displayImage(imageView: ImageView, options: RequestOptions) {
        val target = object : DrawableImageViewTarget(imageView) {
            override fun onResourceReady(drawable: Drawable, transition: Transition<in Drawable>?) {
                super.onResourceReady(drawable, transition)
                // 转场动画进入图片浏览页面会导致Gif图片不动，需要手动调用start
                if (drawable is GifDrawable) {
                    drawable.setVisible(true, false)
                    drawable.start()
                }
                setOnLongClickListener(view, ImageUtils.drawable2Bitmap(drawable))
            }
        }
        when {
            forward != null -> {
                Glide.with(this).load(ForwardModel(forward!!, message!!.contact))
                        .apply(options)
                        .into(target)
            }
            message != null -> {
                Glide.with(this).load(message).apply(options).into(target)
            }
            url != null -> {
                Glide.with(this).load(url).apply(options.placeholder(placeholder)).into(target)
            }
        }
    }

    private val adapter by lazy { DialogOptionAdapter(requireContext()) }

    private fun setOnLongClickListener(photoView: ImageView, bitmap: Bitmap) {
        lifecycleScope.launch {
            val text = withContext(Dispatchers.IO) {
               QRCodeUtils.parseCode(bitmap)
            }
            if (text != null) {
                options.add(DialogOption(getString(R.string.chat_action_recognize_qrcode)) { recognizeQrcode(text) })
                adapter.setData(options)
            }
        }
        photoView.setOnLongClickListener {
            photoView.haptic(HapticFeedbackConstants.LONG_PRESS)
            bottomDialog?.cancel()
            bottomDialog = BottomListDialog.create(requireContext(), adapter, 360.dp)
            bottomDialog?.apply {
                setCancelVisible(true)
                setOnItemClickListener { parent, _, position, _ ->
                    val option = parent.getItemAtPosition(position) as DialogOption
                    option.action.invoke()
                    bottomDialog?.cancel()
                }
                show()
            }
            false
        }
    }

    private fun saveMedia(downloadView: View) {
        PermissionX.init(this)
            .permissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .onExplain(this)
            .request { allGranted, _, _ ->
                if (allGranted) {
                    lifecycleScope.launch {
                        val result = if (message != null && forwardPos == -1) {
                            DownloadManager2.saveToDownload(message!!)
                        } else if (forward != null) {
                            DownloadManager2.saveToDownload(message!!, forwardPos)
                        } else if (url != null) {
                            DownloadManager2.saveToDownload(url!!)
                        } else {
                            return@launch
                        }
                        result.observe(viewLifecycleOwner) {
                            if (it is Result.Success) {
                                toast(getString(R.string.chat_tips_image_download_to, it.data().absolutePath), Toast.LENGTH_LONG)
                                downloadView.gone()
                            } else if (it is Result.Error) {
                                toast(getString(R.string.chat_tips_download_fail, it.error().message))
                            }
                        }
                    }
                } else {
                    toast(R.string.permission_not_granted)
                }
            }
    }

    private fun forward() {
        when {
            message != null -> {
                lifecycleScope.launch {
                    val target = if (message!!.channelType == ChatConst.PRIVATE_CHANNEL) {
                        contactManager.getUserInfo(message?.contact ?: "", true)
                    } else {
                        contactManager.getGroupInfo(message?.contact ?: "")
                    }
                    val source = MessageSource.create(
                        message!!.channelType,
                        delegate.getAddress() ?: "",
                        delegate.toFriendUser().getRawName(),
                        target.getId(),
                        target.getRawName()
                    )
                    val msg = message!!.clone().also { it.source = source }
                    ForwardHelper.checkForwardFile(this@ImageGalleryFragment, arrayListOf(msg)) { error, list ->
                        if (error != null) {
                            toast(error)
                            return@checkForwardFile
                        }
                        ARouter.getInstance().build(MainModule.CONTACT_SELECT)
                            .withSerializable("messages", list)
                            .navigation()
                    }
                }
            }
            forward != null -> {
                lifecycleScope.launch {
                    val msg = ChatMessage.create("", "", 0, forward!!.msgType, forward!!.msg)
                    ForwardHelper.checkForwardFile(this@ImageGalleryFragment, arrayListOf(msg)) { error, list ->
                        if (error != null) {
                            toast(error)
                            return@checkForwardFile
                        }
                        ARouter.getInstance().build(MainModule.CONTACT_SELECT)
                            .withSerializable("messages", list)
                            .navigation()
                    }
                }
            }
            else -> {
                lifecycleScope.launch {
                    if (DownloadManager2.contains(url ?: "")) {
                        toast(R.string.chat_tips_is_downloading)
                        return@launch
                    }
                    loading(true)
                    DownloadManager2.downloadToApp(url ?: "").observe(viewLifecycleOwner) {
                        if (it.isSucceed()) {
                            val size = Utils.getImageSize(it.data().absolutePath ?: "")
                            val message = ChatMessage.create(
                                "",
                                "",
                                0,
                                Biz.MsgType.Image,
                                MessageContent.image(
                                    url,
                                    it.data().absolutePath,
                                    intArrayOf(size[0], size[1])
                                )
                            )
                            ARouter.getInstance().build(MainModule.CONTACT_SELECT)
                                .withSerializable("messages", arrayListOf(message))
                                .navigation()
                            dismiss()
                        } else if (it is Result.Error) {
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun recognizeQrcode(result: String) {
        if (QRCodeHelper.process(requireContext(), result)) {
            activity?.finish()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_forward -> forward()
            R.id.iv_download -> saveMedia(binding.ivDownload)
            R.id.iv_gallery -> {
                message?.also {
                    ARouter.getInstance().build(MainModule.FILE_MANAGEMENT)
                        .withString("target", it.contact)
                        .withInt("channelType", it.channelType)
                        .withInt("index", 1)
                        .navigation()
                    activity?.finish()
                }
            }
        }
    }
}