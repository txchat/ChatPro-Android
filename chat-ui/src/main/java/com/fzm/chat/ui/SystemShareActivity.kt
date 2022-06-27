package com.fzm.chat.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.fzm.chat.R
import com.fzm.chat.core.data.bean.PreSendParams
import com.fzm.chat.biz.base.BizActivity
import com.fzm.chat.core.data.po.MessageContent
import com.fzm.chat.core.utils.Utils
import com.fzm.chat.databinding.ActivitySystemShareBinding
import com.fzm.chat.router.main.MainModule
import com.zjy.architecture.base.instance
import com.zjy.architecture.ext.toast
import com.zjy.architecture.ext.tryWith
import com.zjy.architecture.util.FileUtils
import com.zjy.architecture.util.isAndroidQ
import dtalk.biz.Biz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

@Route(path = MainModule.SYSTEM_SHARE)
class SystemShareActivity : BizActivity() {

    private val binding by init { ActivitySystemShareBinding.inflate(layoutInflater) }
    override val root: View
        get() = binding.root

    private var param: PreSendParams? = null

    @SuppressLint("MissingPermission")
    override fun initView() {
        lifecycleScope.launchWhenResumed {
            try {
                loading(true)
                if (Intent.ACTION_SEND == intent.action && intent.type != null) {
                    val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    if (uri == null) {
                        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                        if (text.isNullOrEmpty()) {
                            toast(R.string.biz_tips_shared_fail)
                        } else {
                            param = PreSendParams(
                                msgType = Biz.MsgType.Text_VALUE,
                                msg = MessageContent.text(text),
                                source = null
                            )
                        }
                    } else {
                        var fileName = "${System.currentTimeMillis()}"
                        if (isAndroidQ) {
                            tryWith {
                                contentResolver.query(uri, arrayOf(
                                    MediaStore.MediaColumns.DISPLAY_NAME,
                                    MediaStore.MediaColumns.MIME_TYPE
                                ), null, null, null)?.use {
                                    if (it.moveToFirst()) {
                                        fileName = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                                    }
                                }
                            }
                        } else {
                            tryWith {
                                contentResolver.query(uri, arrayOf(
                                    MediaStore.MediaColumns.DATA
                                ), null, null, null)?.use {
                                    if (it.moveToFirst()) {
                                        val data = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                                        fileName = data.split("/").lastOrNull() ?: ""
                                    }
                                }
                            }
                        }
                        val (folder, name) = getFileStorePath(intent.type ?: "", fileName)
                        val copyFile = withContext(Dispatchers.IO) {
                            copyFile(folder, name, uri)
                        }
                        if (copyFile == null) {
                            toast(R.string.biz_tips_resource_fail)
                        } else {
                            when {
                                intent.type!!.startsWith("image/") -> {
                                    val size = Utils.getImageSize(this@SystemShareActivity, uri)
                                    param = PreSendParams(
                                        msgType = Biz.MsgType.Image_VALUE,
                                        msg = MessageContent.image(null, copyFile.path, size),
                                        source = null
                                    )
                                }

                                intent.type!!.startsWith("video/") -> {
                                    val duration = Utils.getVideoDuration(this@SystemShareActivity, copyFile.path)
                                    val size = Utils.getVideoSize(this@SystemShareActivity, copyFile.path)
                                    param = PreSendParams(
                                        msgType = Biz.MsgType.Video_VALUE,
                                        msg = MessageContent.video(null, copyFile.path, size, (duration / 1000).toInt()),
                                        source = null
                                    )
                                }

                                else -> {
                                    val file = FileUtils.queryFile(this@SystemShareActivity, copyFile.path)
                                    if (file != null) {
                                        val size = file.getLong(MediaStore.MediaColumns.SIZE)
                                        if (size == 0L) {
                                            toast(R.string.biz_tips_send_empty_file_error)
                                        } else {
                                            param = PreSendParams(
                                                msgType = Biz.MsgType.File_VALUE,
                                                msg = MessageContent.file(null, copyFile.path, file.getString(MediaStore.MediaColumns.DISPLAY_NAME) ?: "", size, FileUtils.getMd5(this@SystemShareActivity, copyFile.path)),
                                                source = null
                                            )
                                        }
                                    } else {
                                        toast(R.string.biz_tips_resource_fail)
                                    }
                                }
                            }
                        }
                    }
                    param?.let {
                        ARouter.getInstance().build(MainModule.CONTACT_SELECT)
                            .withSerializable("preSend", it)
                            .navigation(instance)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                toast(R.string.biz_tips_shared_fail)
            } finally {
                delay(1000)
                dismiss()
                finish()
            }
        }
    }

    private fun getFileStorePath(mimeType: String, filename: String?): Pair<String, String> {
        val folder: String
        val name: String?
        when {
            mimeType.startsWith("image/") -> {
                folder = Environment.DIRECTORY_PICTURES
                name = "IMG_${System.currentTimeMillis()}_share.${FileUtils.getExtension(filename)}"
            }
            mimeType.startsWith("video/") -> {
                folder = "Video"
                name = "VID_${System.currentTimeMillis()}_share.${FileUtils.getExtension(filename)}"
            }
            else -> {
                folder = Environment.DIRECTORY_DOCUMENTS
                name = "$filename"
            }
        }
        return folder to name
    }

    private fun copyFile(folder: String, filename: String?, uri: Uri): File? {
        val dir = getExternalFilesDir(folder) ?: File(filesDir, folder)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val copyFile = File(dir.absolutePath + File.separator + filename)
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                BufferedOutputStream(FileOutputStream(copyFile)).use {
                    input.copyTo(it)
                    it.flush()
                }
            }
            copyFile
        } catch (e: Exception) {
            null
        }
    }

    override fun initData() {
    }

    override fun setEvent() {
    }
}