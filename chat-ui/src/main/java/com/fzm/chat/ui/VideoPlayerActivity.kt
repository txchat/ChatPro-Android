package com.fzm.chat.ui

import android.view.View
import androidx.core.content.ContextCompat
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.danikula.videocache.HttpProxyCacheServer
import xyz.doikki.videocontroller.StandardVideoController
import xyz.doikki.videocontroller.component.*
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.data.ChatDatabase
import com.fzm.chat.core.data.ChatDatabaseProvider
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.contact
import com.fzm.chat.core.data.po.getDisplayUrl
import com.fzm.chat.databinding.ActivityVideoPlayerBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.widget.CustomControlView
import com.zjy.architecture.base.instance
import com.zjy.architecture.util.FileUtils
import com.zjy.architecture.util.other.BarUtils
import org.koin.android.ext.android.inject

/**
 * @author zhengjy
 * @since 2021/03/23
 * Description:
 */
@Deprecated("暂时没有用到，需要用到的话，则要修改下载部分代码")
@Route(path = MainModule.VIDEO_PLAYER)
class VideoPlayerActivity : BizActivity() {

    @JvmField
    @Autowired
    var url: String? = null

    @JvmField
    @Autowired
    var message: ChatMessage? = null

    private var mUrl: String? = null

    private val proxy by inject<HttpProxyCacheServer>()
    private val database: ChatDatabase
        get() = ChatDatabaseProvider.provide()

    private val binding by init { ActivityVideoPlayerBinding.inflate(layoutInflater) }

    private lateinit var controlView: CustomControlView

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        BarUtils.setStatusBarColor(
            this,
            ContextCompat.getColor(this, android.R.color.transparent),
            0
        )
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        mUrl = if (url != null) url else message?.msg?.getDisplayUrl(this)
        if (mUrl?.startsWith("http") == true) {
            mUrl = proxy.getProxyUrl(mUrl)
        }
        binding.videoPlayer.setUrl(mUrl)

        val controller = StandardVideoController(this)

        controller.apply {
            val completeView = CompleteView(context)
            val errorView = ErrorView(context)
            val prepareView = PrepareView(context)
            prepareView.setClickStart()
            addControlComponent(completeView, errorView, prepareView)
            controlView = CustomControlView(context)
            controlView.setOnControlEventListener(object :
                CustomControlView.OnControlEventListener {
                override fun showDownload(): Boolean {
                    return !FileUtils.fileExists(instance, message?.msg?.localUrl)
                }

                override fun openGallery() {
                    message?.also {
                        ARouter.getInstance().build(MainModule.FILE_MANAGEMENT)
                            .withString("target", it.contact)
                            .withInt("channelType", it.channelType)
                            .withInt("index", 1)
                            .navigation()
                        finish()
                    }
                }

                override fun saveVideo(downloadView: View) {
                    download(downloadView)
                }

                override fun forwardVideo() {
                    TODO("Not yet implemented")
                }

                override fun close() {
                    TODO("Not yet implemented")
                }
            })
            addControlComponent(controlView)
            addControlComponent(GestureView(context))
        }

        binding.videoPlayer.setVideoController(controller)

        binding.videoPlayer.start()

    }

    private fun download(view: View) {
        TODO("下载没有实现，因为这个类没有被用到")
    }

    override fun initData() {

    }

    override fun setEvent() {

    }

    override fun onPause() {
        super.onPause()
        binding.videoPlayer.pause()
    }

    override fun onResume() {
        super.onResume()
        binding.videoPlayer.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.videoPlayer.release()
    }


    override fun onBackPressed() {
        if (!binding.videoPlayer.onBackPressed()) {
            // 退出视频播放页面时，先隐藏控制条，避免动画中出现控制条
            controlView.controller.hide()
            super.onBackPressed()
        }
    }
}