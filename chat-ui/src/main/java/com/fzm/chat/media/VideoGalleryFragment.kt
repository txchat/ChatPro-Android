package com.fzm.chat.media

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.launcher.ARouter
import xyz.doikki.videocontroller.StandardVideoController
import xyz.doikki.videocontroller.component.CompleteView
import xyz.doikki.videocontroller.component.ErrorView
import xyz.doikki.videocontroller.component.GestureView
import xyz.doikki.videocontroller.component.PrepareView
import xyz.doikki.videoplayer.player.VideoView
import com.fzm.chat.R
import com.fzm.chat.biz.base.BizFragment
import com.fzm.chat.biz.utils.onExplain
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.*
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.data.po.MessageSource
import com.fzm.chat.core.data.po.getDisplayUrl
import com.fzm.chat.core.data.po.toFriendUser
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.FragmentVideoGalleryBinding
import com.fzm.chat.core.media.DownloadManager2
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.utils.ForwardHelper
import com.fzm.chat.widget.CustomControlView
import com.permissionx.guolindev.PermissionX
import com.zjy.architecture.data.Result
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.load
import com.zjy.architecture.ext.toast
import com.zjy.architecture.ext.visible
import dtalk.biz.Biz
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import xyz.doikki.videoplayer.controller.BaseVideoController

/**
 * @author zhengjy
 * @since 2021/03/31
 * Description:
 */
class VideoGalleryFragment : BizFragment() {

    companion object {
        fun create(message: ChatMessage?, forwardPos: Int, url: String?, initPlay: Boolean, showGallery: Boolean, showOptions: Boolean = true): VideoGalleryFragment {
            return VideoGalleryFragment().apply {
                arguments = bundleOf(
                    "message" to message,
                    "forwardPos" to forwardPos,
                    "url" to url,
                    "initPlay" to initPlay,
                    "showGallery" to showGallery,
                    "showOptions" to showOptions,
                )
            }
        }
    }

    private var forward: ForwardMsg? = null
    private var message: ChatMessage? = null
    private var forwardPos: Int = -1
    private var url: String? = null
    private var startPlayWhenInit = false
    private var showGallery = true
    private var showOptions = true

    private var paused = false
    private var controller: BaseVideoController? = null

    private val delegate by inject<LoginDelegate>()
    private val contactManager by inject<ContactManager>()

    private val binding by init<FragmentVideoGalleryBinding>()

    override val root: View
        get() = binding.root

    override fun initView(view: View, savedInstanceState: Bundle?) {
        message = arguments?.getSerializable("message") as? ChatMessage
        forwardPos = arguments?.getInt("forwardPos", -1) ?: -1
        url = arguments?.getString("url")
        startPlayWhenInit = arguments?.getBoolean("initPlay") ?: false
        showGallery = arguments?.getBoolean("showGallery") ?: true
        showOptions = arguments?.getBoolean("showOptions") ?: true
        if (forwardPos != -1) {
            forward = message?.msg?.forwardLogs?.filterMedia()?.get(forwardPos)
        }
        binding.preview.load(message?.msg?.getDisplayUrl(requireContext()))
        setupVideoPlayer(binding.videoPlayer)
    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.flThumb.setOnClickListener { start() }
    }

    override fun onResume() {
        super.onResume()
        if (paused && !binding.videoPlayer.isPlaying) {
            start()
            paused = false
        }
    }

    private fun start() {
        if (!fileExist()) {
            saveMedia(null)
        }
        if (binding.flThumb.isVisible) {
            binding.flThumb.gone()
        }
        if (binding.videoPlayer.isGone) {
            binding.videoPlayer.visible()
        }
        binding.videoPlayer.start()
    }

    override fun onPause() {
        super.onPause()
        if (binding.videoPlayer.isPlaying) {
            binding.videoPlayer.pause()
            paused = true
        }
    }

    override fun onBackPressedSupport(): Boolean {
        controller?.gone()
        return super.onBackPressedSupport()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.videoPlayer.release()
    }

    private fun setupVideoPlayer(player: VideoView<*>) {
        if (message != null && forwardPos == -1) {
            player.setUrl(message!!.msg.localUrl)
        } else {
            player.setUrl(forward?.msg?.localUrl ?: url)
        }
        controller = StandardVideoController(requireContext())

        controller?.apply {
            val completeView = CompleteView(context)
            val errorView = ErrorView(context)
            val prepareView = PrepareView(context)
            prepareView.setClickStart()
            addControlComponent(completeView, errorView, prepareView)
            val controlView = CustomControlView(context)
            controlView.fitsSystemWindows = true
            controlView.setGalleryVisible(showGallery)
            controlView.setOptionsVisible(showOptions)
            controlView.setOnControlEventListener(object :
                CustomControlView.OnControlEventListener {
                override fun showDownload(): Boolean {
                    return true
                }

                override fun openGallery() {
                    message?.also {
                        ARouter.getInstance().build(MainModule.FILE_MANAGEMENT)
                            .withString("target", it.contact)
                            .withInt("channelType", it.channelType)
                            .withInt("index", 1)
                            .navigation()
                        activity?.finish()
                    }
                }

                override fun saveVideo(downloadView: View) {
                    saveMedia(downloadView)
                }

                override fun forwardVideo() {
                    forward()
                }

                override fun close() {
                    controller?.gone()
                    activity?.finishAfterTransition()
                }
            })
            addControlComponent(controlView)
            addControlComponent(GestureView(context))
        }

        player.setVideoController(controller)
        if (startPlayWhenInit) {
            start()
        }
    }

    private fun fileExist(): Boolean {
        return if (message != null && forwardPos == -1) {
            message!!.localExists()
        } else if (forward != null) {
            forward!!.localExists()
        } else false
    }

    private fun saveMedia(downloadView: View?) {
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
                                if (message != null && forwardPos == -1) {
                                    binding.videoPlayer.setUrl(message!!.msg.localUrl)
                                } else {
                                    binding.videoPlayer.setUrl(forward?.msg?.localUrl ?: url)
                                }
                                downloadView?.also { view ->
                                    toast(getString(R.string.chat_tips_video_download_to, it.data().absolutePath), Toast.LENGTH_LONG)
                                    view.gone()
                                }
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
                    ForwardHelper.checkForwardFile(this@VideoGalleryFragment, arrayListOf(msg)) { error, list ->
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
                    ForwardHelper.checkForwardFile(this@VideoGalleryFragment, arrayListOf(msg)) { error, list ->
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
}