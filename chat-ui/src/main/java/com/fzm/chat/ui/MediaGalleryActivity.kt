package com.fzm.chat.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.ForwardMsg
import com.fzm.chat.core.data.model.filterMedia
import com.fzm.chat.databinding.ActivityMediaGalleryBinding
import com.fzm.chat.media.ImageGalleryFragment
import com.fzm.chat.media.VideoGalleryFragment
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.ext.notchSupport
import dtalk.biz.Biz

/**
 * @author zhengjy
 * @since 2021/01/25
 * Description:
 */
@Route(path = MainModule.MEDIA_GALLERY)
class MediaGalleryActivity : BizActivity() {

    companion object {
        const val EXIT_IMG_POS = "exitPos"

        const val FADE_OUT_TIMEOUT = 5_000L
    }

    @JvmField
    @Autowired
    var messages: ArrayList<ChatMessage>? = null

    @JvmField
    @Autowired
    var images: ArrayList<String>? = null

    @JvmField
    @Autowired
    var forward: ChatMessage? = null

    @JvmField
    @Autowired
    var index: Int = 0

    @JvmField
    @Autowired
    var placeholder: Int = 0

    @JvmField
    @Autowired
    var showGallery: Boolean = true

    @JvmField
    @Autowired
    var showOptions: Boolean = true

    private var currentIndex = 0
    private var forwardMessages = mutableListOf<ForwardMsg>()

    private lateinit var mAdapter: ImageAdapter

    private val binding by init { ActivityMediaGalleryBinding.inflate(layoutInflater) }

    override val root: View
        get() = binding.root

    override fun setSystemBar() {
        notchSupport(window)
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        ARouter.getInstance().inject(this)
        forward?.msg?.forwardLogs?.apply { forwardMessages.addAll(this.filterMedia()) }
        mAdapter = ImageAdapter(this, messages ?: listOf(), images ?: listOf(), forward)
        binding.vpImage.offscreenPageLimit = 3
        binding.vpImage.adapter = mAdapter
        binding.vpImage.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                currentIndex = position
            }

            override fun onPageScrollStateChanged(state: Int) {

            }
        })
        currentIndex = index
        binding.vpImage.currentItem = currentIndex
    }

    override fun initData() {

    }

    override fun setEvent() {

    }

    override fun onBackPressedSupport() {
        finishAfterTransition()
    }

    override fun finishAfterTransition() {
        val intent = Intent()
        if (index == currentIndex) {
            //没有改变
            intent.putExtra(EXIT_IMG_POS, -1)
        } else {
            intent.putExtra(EXIT_IMG_POS, currentIndex)
        }
        setResult(RESULT_OK, intent)
        super.finishAfterTransition()
    }

    inner class ImageAdapter(
        activity: FragmentActivity,
        private val messages: List<ChatMessage>,
        private val images: List<String>,
        private val forwards: ChatMessage?
    ) : FragmentStatePagerAdapter(activity.supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private var init = true

        override fun getCount(): Int {
            return when {
                messages.isNotEmpty() -> messages.size
                forwardMessages.isNotEmpty() -> forwardMessages.size
                else -> images.size
            }
        }

        override fun getItem(position: Int): Fragment {
            val initPlay = init
            init = false
            return if (messages.isNotEmpty()) {
                if (messages[position].msgType == Biz.MsgType.Image_VALUE) {
                    ImageGalleryFragment.create(messages[position], -1, null, showGallery, showOptions)
                } else {
                    VideoGalleryFragment.create(messages[position], -1, null, initPlay, showGallery, showOptions)
                }
            } else if (forwards != null) {
                if (forwardMessages[position].msgType == Biz.MsgType.Image_VALUE) {
                    ImageGalleryFragment.create(forwards, position, null, false, showOptions)
                } else {
                    VideoGalleryFragment.create(forwards, position, null, initPlay, false, showOptions)
                }
            } else {
                ImageGalleryFragment.create(null, -1, images[position], showGallery, showOptions, placeholder)
            }
        }
    }
    
}