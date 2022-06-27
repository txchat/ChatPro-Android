package com.fzm.chat.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.FileProvider
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.adapter.DialogOption
import com.fzm.chat.adapter.DialogOptionAdapter
import com.fzm.chat.bean.SimpleFileBean
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.biz.base.BizFileProvider
import com.fzm.chat.biz.utils.onExplain
import com.fzm.chat.core.data.ChatConst
import com.fzm.chat.core.data.local.ContactManager
import com.fzm.chat.core.data.model.ChatMessage
import com.fzm.chat.core.data.model.contact
import com.fzm.chat.core.data.model.localExists
import com.fzm.chat.core.data.po.MessageSource
import com.fzm.chat.core.data.po.toFriendUser
import com.fzm.chat.core.media.DownloadManager2
import com.fzm.chat.core.session.LoginDelegate
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.ActivityFileDetailBinding
import com.fzm.chat.router.main.MainModule
import com.fzm.chat.utils.ForwardHelper
import com.fzm.chat.widget.BottomListDialog
import com.permissionx.guolindev.PermissionX
import com.zjy.architecture.data.LocalFile
import com.zjy.architecture.data.Result
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.singleClick
import com.zjy.architecture.ext.toast
import com.zjy.architecture.ext.visible
import com.zjy.architecture.util.FileUtils
import com.zjy.architecture.util.InstallUtil
import com.zjy.architecture.util.isContent
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File

/**
 * @author zhengjy
 * @since 2021/03/16
 * Description:
 */
@Route(path = MainModule.FILE_DETAIL)
class FileDetailActivity : BizActivity() {

    @JvmField
    @Autowired
    var file: SimpleFileBean? = null

    @JvmField
    @Autowired
    var message: ChatMessage? = null

    @JvmField
    @Autowired
    var forwardPos: Int = -1

    private var bottomDialog: BottomListDialog? = null
    private val adapter by lazy { DialogOptionAdapter(this) }

    private val contactManager by inject<ContactManager>()
    private val delegate by inject<LoginDelegate>()

    private val binding by init<ActivityFileDetailBinding>()

    override val root: View
        get() = binding.root

    override fun initView() {
        ARouter.getInstance().inject(this)
        binding.ctbTitle.setRightVisible(false)
        binding.ctbTitle.setTitle(file?.name)
        binding.tvFileName.text = file?.name
        binding.tvFileSize.text = Utils.byteToSize(file?.size ?: 0L)
        when (FileUtils.getExtension(file?.name)) {
            "doc", "docx" -> binding.ivFileType.setImageResource(R.mipmap.icon_file_doc)
            "pdf" -> binding.ivFileType.setImageResource(R.mipmap.icon_file_pdf)
            "ppt", "pptx" -> binding.ivFileType.setImageResource(R.mipmap.icon_file_other)
            "xls", "xlsx" -> binding.ivFileType.setImageResource(R.mipmap.icon_file_xls)
            "mp3", "wma", "wav", "ogg" -> binding.ivFileType.setImageResource(R.mipmap.icon_file_music)
            "mp4", "avi", "rmvb", "flv", "f4v", "mpg", "mkv" -> binding.ivFileType.setImageResource(R.mipmap.icon_file_video)
            else -> binding.ivFileType.setImageResource(R.mipmap.icon_file_other)
        }
        binding.openTips.text = getString(R.string.chat_tips_can_not_open, getString(R.string.app_name))
    }

    override fun initData() {
        adapter.setData(listOf(
            DialogOption("转发") { lifecycleScope.launch { forwardFile() } },
            DialogOption("分享") { shareFile(file?.path) },
            DialogOption("保存到手机") { saveFile() }
        ))
        lifecycleScope.launch {
            val msg = message ?: return@launch
            if (forwardPos == -1) {
                when {
                    msg.localExists() -> {
                        binding.ctbTitle.setRightVisible(true)
                        binding.llOpen.visible()
                        binding.pbFile.gone()
                        binding.tvDownload.gone()
                    }
                    DownloadManager2.contains(msg) -> {
                        binding.tvDownload.gone()
                        DownloadManager2.downloadToApp(msg).observe(this@FileDetailActivity) {
                            downloadCallback(it)
                        }
                    }
                    else -> {
                        binding.llOpen.gone()
                        binding.pbFile.gone()
                        binding.tvDownload.visible()
                    }
                }
            } else {
                val logs = msg.msg.forwardLogs
                if (logs.size > forwardPos) {
                    when {
                        logs[forwardPos].localExists() -> {
                            binding.ctbTitle.setRightVisible(true)
                            binding.llOpen.visible()
                            binding.pbFile.gone()
                            binding.tvDownload.gone()
                        }
                        logs.size > forwardPos && DownloadManager2.contains(logs[forwardPos]) -> {
                            binding.tvDownload.gone()
                            DownloadManager2.downloadToApp(msg, forwardPos)
                                .observe(this@FileDetailActivity) {
                                    downloadCallback(it)
                                }
                        }
                        else -> {
                            binding.llOpen.gone()
                            binding.pbFile.gone()
                            binding.tvDownload.visible()
                        }
                    }
                }
            }
        }
    }

    private fun downloadCallback(result: Result<LocalFile>) {
        when (result) {
            is Result.Success -> {
                binding.ctbTitle.setRightVisible(true)
                binding.llOpen.visible()
                binding.pbFile.gone()
                binding.tvDownload.gone()
                file?.path = result.data.absolutePath
            }
            is Result.Loading -> {
                if (binding.pbFile.isGone) {
                    binding.llOpen.gone()
                    binding.pbFile.visible()
                    binding.tvDownload.gone()
                }
                binding.pbFile.progress = (result.progress * 100).toInt()
            }
            is Result.Error -> {
                toast("文件接收失败")
                binding.pbFile.progress = 0
                binding.llOpen.gone()
                binding.pbFile.gone()
                binding.tvDownload.visible()
            }
        }
    }

    override fun setEvent() {
        binding.ctbTitle.setOnLeftClickListener { finish() }
        binding.ctbTitle.setOnRightClickListener { openMenu() }
        binding.tvOpen.setOnClickListener {
            file?.path?.also {
                openFile(this, it)
            }
        }
        binding.tvDownload.singleClick {
            val msg = message ?: return@singleClick
            binding.pbFile.visible()
            binding.tvDownload.gone()
            if (forwardPos == -1) {
                DownloadManager2.downloadToApp(msg).observe(this@FileDetailActivity) {
                    downloadCallback(it)
                }
            } else {
                DownloadManager2.downloadToApp(msg, forwardPos).observe(this@FileDetailActivity) {
                    downloadCallback(it)
                }
            }
        }
    }

    private fun openMenu() {
        bottomDialog = BottomListDialog.create(this, adapter, LinearLayout.LayoutParams.MATCH_PARENT)
        bottomDialog?.apply {
            setCancelVisible(true)
            setOnItemClickListener { parent, _, position, _ ->
                val option = parent.getItemAtPosition(position) as DialogOption
                option.action.invoke()
                cancel()
            }
            show()
        }
    }

    private suspend fun forwardFile() {
        try {
            if (message == null) {
                toast("转发失败消息不存在")
                return
            }
            message?.also { chatMessage ->
                loading(true)
                if (forwardPos == -1) {
                    val target = if (chatMessage.channelType == ChatConst.PRIVATE_CHANNEL) {
                        contactManager.getUserInfo(chatMessage.contact, true)
                    } else {
                        contactManager.getGroupInfo(chatMessage.contact)
                    }
                    val source = MessageSource.create(
                        chatMessage.channelType,
                        delegate.getAddress() ?: "",
                        delegate.toFriendUser().getRawName(),
                        target.getId(),
                        target.getRawName()
                    )
                    val msg = chatMessage.clone().also { it.source = source }
                    ForwardHelper.checkForwardFile(this@FileDetailActivity, arrayListOf(msg)) { error, list ->
                        if (error != null) {
                            toast(error)
                            return@checkForwardFile
                        }
                        ARouter.getInstance().build(MainModule.CONTACT_SELECT)
                            .withSerializable("messages", list)
                            .navigation()
                    }
                } else {
                    val forward = chatMessage.msg.forwardLogs[forwardPos]
                    val msg = ChatMessage.create("", "", 0, forward.msgType, forward.msg)
                    ForwardHelper.checkForwardFile(this@FileDetailActivity, arrayListOf(msg)) { error, list ->
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
        } catch (e: Exception) {
            toast("转发失败")
        } finally {
            dismiss()
        }
    }

    private fun shareFile(path: String?) {
        try {
            if (path.isNullOrEmpty()) {
                toast("分享失败，文件不存在")
                return
            }
            val uri = getUriForFile(this, path)
            val type = contentResolver.getType(uri)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = type
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(intent, "分享"))
        } catch (e: Exception) {
            toast("分享失败")
        }
    }

    private fun saveFile() {
        PermissionX.init(this)
            .permissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .onExplain(this)
            .request { allGranted, _, _ ->
                if (allGranted) {
                    val result = if (message != null && forwardPos == -1) {
                        DownloadManager2.saveToDownload(message!!)
                    } else if (message != null && forwardPos >= 0) {
                        DownloadManager2.saveToDownload(message!!, forwardPos)
                    } else {
                        toast("文件不存在")
                        return@request
                    }
                    result.observe(this) {
                        if (it is Result.Success) {
                            toast(getString(R.string.chat_tips_file_download_to, it.data().absolutePath))
                        } else if (it is Result.Error) {
                            toast(getString(R.string.chat_tips_download_fail, it.error().message))
                        }
                    }
                } else {
                    toast(R.string.permission_not_granted)
                }
            }
    }

    private fun openFile(context: Context, path: String) {
        try {
            val uri = getUriForFile(context, path)
            val type = contentResolver.getType(uri)
            if (type == "application/vnd.android.package-archive") {
                InstallUtil.install(context, uri)
            } else {
                val intent = Intent().run {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    action = Intent.ACTION_VIEW
                    setDataAndType(uri, type)
                }
                context.startActivity(intent)
                Intent.createChooser(intent, getString(R.string.chat_tips_select_open_type))
            }
        } catch (e: ActivityNotFoundException) {
            toast(R.string.chat_tips_unsupported_file_type)
        }
    }

    private fun getUriForFile(context: Context, path: String): Uri {
        return if (path.isContent()) {
            Uri.parse(path)
        } else {
            FileProvider.getUriForFile(context, BizFileProvider.authority(this), File(path))
        }
    }
}