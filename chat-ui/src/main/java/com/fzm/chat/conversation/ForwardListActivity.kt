package com.fzm.chat.conversation

import android.annotation.SuppressLint
import android.content.Intent
import android.view.MotionEvent
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.bean.SimpleFileBean
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.conversation.adapter.ForwardAdapter
import com.fzm.chat.conversation.adapter.forward.ForwardBaseItem
import com.fzm.chat.core.data.model.*
import com.fzm.chat.databinding.ActivityForwardListBinding
import com.fzm.chat.core.media.DownloadManager2
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.router.oa.OAModule
import com.fzm.chat.router.oa.OAService
import com.fzm.chat.router.route
import com.fzm.chat.ui.MediaGalleryActivity
import com.zjy.architecture.base.instance
import com.zjy.architecture.data.Result
import com.zjy.architecture.ext.format
import com.zjy.architecture.ext.toast
import dtalk.biz.Biz
import kotlinx.coroutines.launch

/**
 * @author zhengjy
 * @since 2021/06/23
 * Description:
 */
@Route(path = MainModule.FORWARD_LIST)
class ForwardListActivity : BizActivity(), ForwardBaseItem.ForwardMessageClickListener {

    @JvmField
    @Autowired(name = "message")
    var forwardMessage: ChatMessage? = null

    private lateinit var mAdapter: ForwardAdapter
    private val forwardList = mutableListOf<ForwardMsg>()
    private val oaService by route<OAService>(OAModule.SERVICE)

    private val binding by init { ActivityForwardListBinding.inflate(layoutInflater) }

    override val darkStatusColor: Boolean = true

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        forwardMessage?.msg?.forwardLogs?.apply { forwardList.addAll(this) }
        val source = forwardMessage?.getSourceText(this)
        binding.ctbTitle.setTitle(source)
        binding.rvForward.layoutManager = LinearLayoutManager(this)
        mAdapter = ForwardAdapter(forwardMessage, mutableListOf(), this)
        mAdapter.addData(forwardList)
        binding.rvForward.adapter = mAdapter
        if (forwardList.isNotEmpty()) {
            val firstDay = forwardList.first().datetime
            val lastDay = forwardList.last().datetime
            @SuppressLint("SetTextI18n")
            binding.tvDate.text = "${firstDay.format("yyyy-MM-dd")}~${lastDay.format("yyyy-MM-dd")}"
        }

    }

    override fun initData() {

    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { onBackPressed() }
    }

    override fun onTouchChatMainView(view: View, event: MotionEvent?): Boolean {
        return false
    }

    override fun onChatLayoutClick(view: View, message: ForwardMsg, item: ForwardBaseItem) {
        when (Biz.MsgType.forNumber(message.msgType)) {
            Biz.MsgType.Image -> {
                val list = mAdapter.data.filterMedia()
                val intent = Intent(instance, MediaGalleryActivity::class.java).apply {
                    putExtra("forward", forwardMessage)
                    putExtra("index", list.indexOf(message))
                }
                startActivity(
                    intent, ActivityOptionsCompat.makeSceneTransitionAnimation(
                        instance, view, "shareImage"
                    ).toBundle()
                )
            }
            Biz.MsgType.Video->{
                if (message.localExists()) {
                    val list = mAdapter.data.filterMedia()
                    val intent = Intent(instance, MediaGalleryActivity::class.java).apply {
                        putExtra("forward", forwardMessage)
                        putExtra("index", list.indexOf(message))
                    }
                    startActivity(
                        intent, ActivityOptionsCompat.makeSceneTransitionAnimation(
                            instance, view, "shareImage"
                        ).toBundle()
                    )
                } else {
                    lifecycleScope.launch {
                        if (DownloadManager2.contains(message)) {
                            toast(R.string.chat_tips_is_downloading)
                            return@launch
                        }
                        val pos = forwardMessage?.msg?.forwardLogs?.indexOf(message) ?: -1
                        if (pos == -1) {
                            toast("下载失败，未知错误")
                            return@launch
                        }
                        DownloadManager2.downloadToApp(forwardMessage!!, pos).observe(instance) {
                            if (it.isSucceed()) {
                                toast("下载完成")
                            } else if (it is Result.Error) {
                                toast("下载失败，${it.error().message}")
                            }
                        }
                    }
                }
            }
            Biz.MsgType.File -> {
                val pos = forwardMessage?.msg?.forwardLogs?.indexOf(message) ?: -1
                ARouter.getInstance().build(MainModule.FILE_DETAIL)
                    .withSerializable(
                        "file", SimpleFileBean(
                            message.msg.fileName,
                            message.msg.size,
                            message.msg.md5,
                            message.msg.localUrl
                        )
                    )
                    .withSerializable("message", forwardMessage)
                    .withInt("forwardPos", pos)
                    .navigation()
            }
            Biz.MsgType.ContactCard -> {
                when (message.msg.contactType) {
                    1 -> {
                        ARouter.getInstance().build(MainModule.CONTACT_INFO)
                            .withString("address", message.msg.contactId)
                            .navigation()
                    }
                    2 -> {
                        ARouter.getInstance().build(MainModule.JOIN_GROUP_INFO)
                            .withLong("groupId", message.msg.contactId?.toLong() ?: 0L)
                            .withString("server", message.msg.contactServer)
                            .withString("inviterId", message.msg.contactInviter)
                            .navigation()
                    }
                    3 -> oaService?.applyJoinTeamPage(
                        instance, mapOf(
                            "id" to message.msg.contactId,
                            "server" to message.msg.contactServer,
                            "inviterId" to message.msg.contactInviter
                        )
                    )
                }
            }
        }
    }

    override fun onChatLayoutDoubleTap(view: View, message: ForwardMsg) {
        
    }

    override fun onChatLayoutLongClick(view: View, message: ForwardMsg): Boolean {
        return false
    }

    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
        handleExitTransaction(resultCode, data)
    }

    private fun handleExitTransaction(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val exitPos = data.getIntExtra(MediaGalleryActivity.EXIT_IMG_POS, -1)
            setExitSharedElement(exitPos)
        }
    }

    private fun setExitSharedElement(position: Int) {
        if (position == -1) {
            return
        }
        val imageIndex = mAdapter.data.filterMedia()[position]
        val view = mAdapter.getViewByPosition(mAdapter.data.indexOf(imageIndex), R.id.iv_image)
        ActivityCompat.setExitSharedElementCallback(this, object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String?>,
                sharedElements: MutableMap<String?, View?>
            ) {
                names.clear()
                sharedElements.clear()
                if (view != null) {
                    names.add(ViewCompat.getTransitionName(view))
                    sharedElements[ViewCompat.getTransitionName(view)] = view
                    setExitSharedElementCallback(object : SharedElementCallback() {})
                } else {
                    // 如果图片不在当前界面内，则用默认转场动画
                    names.add(ViewCompat.getTransitionName(binding.emptyShare))
                    sharedElements[ViewCompat.getTransitionName(binding.emptyShare)] =
                        binding.emptyShare
                    setExitSharedElementCallback(object : SharedElementCallback() {})
                }
            }
        })
    }
}