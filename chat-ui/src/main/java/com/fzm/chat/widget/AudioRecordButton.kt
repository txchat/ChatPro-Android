package com.fzm.chat.widget

import android.app.Dialog
import android.content.Context
import android.media.MediaRecorder
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.fzm.chat.R
import com.fzm.chat.core.utils.uuid
import com.fzm.chat.databinding.DialogAudioRecordBinding
import com.zjy.architecture.ext.gone
import com.zjy.architecture.ext.haptic
import com.zjy.architecture.ext.visible
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * @author zhengjy
 * @since 2021/01/26
 * Description:
 */
class AudioRecordButton : AppCompatTextView {

    companion object {
        private const val PREPARED = 1
        private const val RECORDING = 2
        private const val WANT_CANCEL = 3

        private const val DISTANCE_Y_CANCEL = 100

        /**
         * 最短录音时间
         */
        private const val MIN_RECORD_TIME = 1

        /**
         * 最长录音时间
         */
        private const val MAX_RECORD_TIME = 60

        /**
         * 开始显示倒计时的时间
         */
        private const val COUNTDOWN_SECONDS = 10

        private const val MSG_AUDIO_PREPARED = 0X110
        private const val MSG_VOICE_CHANGE = 0X111
        private const val MSG_DIALOG_DISMISS = 0X112
    }

    private var mCurrentState: Int = PREPARED

    /**
     * 正在录音
     */
    private var isRecording = false

    /**
     * 是否正在倒计时
     */
    private var isCountDown = false
    private var mDuration = 0f
    private var mReady = false
    private lateinit var mPath: String

    private lateinit var dialogManager: DialogManager
    private lateinit var audioManager: AudioManager

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        gravity = Gravity.CENTER
        dialogManager = DialogManager(context)
        audioManager = AudioManager()
        mHandler = MyHandler(this)

        mPath = "${(context.externalCacheDir ?: context.cacheDir).absolutePath}/audio"
        audioManager.setAudioPath(mPath)
        audioManager.setMaxRecordTime(MAX_RECORD_TIME)
        audioManager.setOnAudioStageListener { prepared() }
        setOnLongClickListener {
            mReady = true
            haptic(HapticFeedbackConstants.LONG_PRESS)
            audioManager.prepareAudio()
            true
        }
    }

    private fun prepared() {
        mHandler?.sendEmptyMessage(MSG_AUDIO_PREPARED)
    }

    private var mListener: AudioFinishRecorderListener? = null

    interface AudioFinishRecorderListener {
        fun onFinished(seconds: Float, filePath: String?)
    }

    fun setAudioFinishRecorderListener(listener: AudioFinishRecorderListener?) {
        mListener = listener
    }

    private fun changeState(state: Int) {
        if (mCurrentState != state) {
            mCurrentState = state
            when (mCurrentState) {
                PREPARED -> {
                    setBackgroundResource(R.drawable.bg_send_record_btn)
                    setText(R.string.chat_record_state_prepared)
                }
                RECORDING -> {
                    setBackgroundResource(R.drawable.bg_send_record_btn_press)
                    setText(R.string.chat_record_state_recording)
                    if (isRecording) {
                        if (isCountDown) {
                            dialogManager.countdown()
                        } else {
                            dialogManager.recording()
                        }
                    }
                }
                WANT_CANCEL -> {
                    setBackgroundResource(R.drawable.bg_send_record_btn_press)
                    setText(R.string.chat_record_state_want_cancel)
                    dialogManager.wantToCancel()
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> changeState(RECORDING)
            MotionEvent.ACTION_MOVE -> {
                if (isRecording) {
                    // 根据x，y来判断用户是否想要取消
                    if (wantToCancel(event.x.toInt(), event.y.toInt())) {
                        changeState(WANT_CANCEL)
                    } else {
                        changeState(RECORDING)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 首先判断是否有触发onLongClick事件，没有的话直接返回reset
                if (!mReady) {
                    reset()
                    return super.onTouchEvent(event)
                }
                if (mCurrentState == WANT_CANCEL) {
                    dialogManager.dismissDialog()
                    audioManager.cancel()
                } else if (!isRecording || mDuration < MIN_RECORD_TIME) {
                    // 如果按的时间太短，还没准备好或者时间录制太短，就离开了，则显示这个dialog
                    dialogManager.tooShort()
                    audioManager.cancel()
                    mHandler?.sendEmptyMessageDelayed(MSG_DIALOG_DISMISS, 1300)
                } else if (mCurrentState == RECORDING && isRecording) {
                    //正常录制结束
                    dialogManager.dismissDialog()
                    audioManager.release()
                    mListener?.onFinished(mDuration, audioManager.getCurrentAudioPath())
                }

                reset()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun wantToCancel(x: Int, y: Int): Boolean {
        if (x < 0 || x > width) {
            return true
        }
        return y < -DISTANCE_Y_CANCEL || y > height + DISTANCE_Y_CANCEL
    }

    private fun reset() {
        changeState(PREPARED)
        isRecording = false
        mReady = false
        mDuration = 0f
    }

    private fun showLastSeconds(duration: Int) {
        if (mCurrentState != WANT_CANCEL) {
            if (duration >= MAX_RECORD_TIME) { //达到最长录音时间
                isRecording = false //停止录音
                dialogManager.tooLong()
                audioManager.release()
                mListener?.onFinished(MAX_RECORD_TIME.toFloat(), audioManager.getCurrentAudioPath())
                mHandler?.sendEmptyMessageDelayed(MSG_DIALOG_DISMISS, 300)
                isCountDown = false
                reset()
            } else {
                dialogManager.updateVoiceSecond(MAX_RECORD_TIME - duration - 1)
            }
        }
    }

    private var mHandler: Handler? = null
    private var voiceLevelThread: Thread? = null

    private val mGetVoiceLevelRunnable = Runnable {
        while (isRecording) {
            try {
                Thread.sleep(100)
                mDuration += 0.1f
                mHandler?.sendEmptyMessage(MSG_VOICE_CHANGE)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    inner class DialogManager(private val mContext: Context) {

        private var _binding: DialogAudioRecordBinding? = null

        private val binding: DialogAudioRecordBinding
            get() = _binding!!

        private var mDialog: Dialog? = null

        fun showRecordingDialog() {
            mDialog = Dialog(mContext, R.style.Theme_audioDialog)
            _binding = DialogAudioRecordBinding.inflate(LayoutInflater.from(mContext))
            mDialog?.setContentView(binding.root)
            mDialog?.show()
        }

        /**
         * 设置正在录音时的dialog界面
         */
        fun recording() {
            if (mDialog?.isShowing == true) {
                binding.dialogIcon.visible()
                binding.dialogCancel.gone()
                binding.recorderTips.visible()
                binding.dialogIcon.setText(R.string.icon_record_vol1)
                binding.recorderTips.setText(R.string.chat_record_slide_to_cancel)
                binding.recorderTips.setTextColor(ContextCompat.getColor(mContext, android.R.color.white))
            }
        }

        fun countdown() {
            if (mDialog?.isShowing == true) {
                binding.dialogIcon.visible()
                binding.dialogCancel.gone()
                binding.recorderTips.visible()
                binding.recorderTips.setText(R.string.chat_record_slide_to_cancel)
                binding.recorderTips.setTextColor(ContextCompat.getColor(mContext, android.R.color.white))
            }
        }

        fun wantToCancel() {
            if (mDialog?.isShowing == true) {
                binding.dialogIcon.gone()
                binding.dialogCancel.visible()
                binding.recorderTips.visible()
                binding.dialogCancel.setIconText(R.string.icon_yuyin_chehui)
                binding.recorderTips.setText(R.string.chat_record_state_want_cancel)
                binding.recorderTips.setTextColor(ContextCompat.getColor(mContext, R.color.biz_red_tips))
            }
        }

        fun tooShort() {
            if (mDialog?.isShowing == true) {
                binding.dialogIcon.gone()
                binding.dialogCancel.visible()
                binding.recorderTips.visible()
                binding.dialogCancel.setIconBackground(R.mipmap.voice_to_short)
                binding.recorderTips.setText(R.string.chat_record_tips_too_short)
                binding.recorderTips.setTextColor(ContextCompat.getColor(mContext, R.color.biz_red_tips))
            }
        }

        fun tooLong() {

        }

        fun dismissDialog() {
            if (mDialog?.isShowing == true) {
                mDialog?.dismiss()
                mDialog = null
                _binding = null
            }
        }

        fun updateVoiceLevel(level: Int) {
            if (mDialog?.isShowing == true) {
                // 通过level来找到图片的id，也可以用switch来寻址，但是代码可能会比较长
                val resId = mContext.resources.getIdentifier("icon_record_vol$level",
                    "string", mContext.packageName)
                binding.dialogIcon.setIconText(resId)
            }
        }

        fun updateVoiceSecond(second: Int) {
            if (mDialog?.isShowing == true) {
                val showLastSeconds = mContext.getString(R.string.chat_speak_time_remain, second)
                binding.recorderTips.text = showLastSeconds
            }
        }
    }

    inner class AudioManager {
        // 最长录音时间（秒）
        private var maxRecordTime = 60

        private var mRecorder: MediaRecorder? = null
        private var mCurrentFilePathString: String? = null
        private var isPrepared = false

        private var mDir: String = ""
        private var mListener: (() -> Unit)? = null

        fun setOnAudioStageListener(listener: () -> Unit) {
            mListener = listener
        }

        fun setAudioPath(path: String) {
            mDir = path
        }

        fun setMaxRecordTime(max: Int) {
            maxRecordTime = max
        }

        fun prepareAudio() {
            try {
                // 一开始应该是false的
                isPrepared = false
                val dir = File(mDir)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val fileName = "${uuid()}.aac"
                val file = File(dir, fileName)
                mCurrentFilePathString = file.absolutePath
                mRecorder = MediaRecorder()
                // 设置输出文件
                mRecorder?.setOutputFile(file.absolutePath)
                // 设置mediaRecorder的音频源是麦克风
                mRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                // 设置文件音频的输出格式为amr
                mRecorder?.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                // 设置音频的编码格式为amr
                mRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                mRecorder?.setMaxDuration(maxRecordTime * 1000)
                // 严格遵守google官方api给出的mediaRecorder的状态流程图
                mRecorder?.prepare()
                mRecorder?.start()
                mRecorder?.setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        // 如果达到要求的最长时间，停止录音
                        release()
                    }
                }
                // 准备结束
                isPrepared = true
                // 已经准备好了，可以录制了
                mListener?.invoke()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        /**
         * 获得声音的level
         */
        fun getVoiceLevel(maxLevel: Int): Int {
            // mRecorder.getMaxAmplitude()是音频的振幅范围，值域是1-32767
            if (isPrepared) {
                mRecorder?.also {
                    try {
                        // 取正+1，否则取不到7
                        return (maxLevel * it.maxAmplitude / 32768 + 1).coerceIn(1..7)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            return 1
        }

        fun release() {
            mRecorder?.also {
                try {
                    it.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                it.release()
                mRecorder = null
            }
        }

        fun cancel() {
            release()
            mCurrentFilePathString?.also {
                File(it).delete()
                mCurrentFilePathString = null
            }
        }

        fun getCurrentAudioPath() = mCurrentFilePathString
    }

    private class MyHandler constructor(out: AudioRecordButton) : Handler() {
        val weakReference: WeakReference<AudioRecordButton> = WeakReference(out)
        override fun handleMessage(msg: Message) {
            val out = weakReference.get() ?: return
            when (msg.what) {
                MSG_AUDIO_PREPARED -> {
                    // 显示应该是在audio end prepare之后回调
                    out.dialogManager.showRecordingDialog()
                    out.isRecording = true
                    // 需要开启一个线程来变换音量
                    out.voiceLevelThread = Thread(out.mGetVoiceLevelRunnable)
                    out.voiceLevelThread?.start()
                }
                MSG_VOICE_CHANGE -> {
                    if (out.mDuration >= MAX_RECORD_TIME - COUNTDOWN_SECONDS - 1) {
                        out.showLastSeconds(out.mDuration.toInt())
                        out.isCountDown = true
//                        //是切换状态且当前状态不是STATE_WANT_TO_CANCEL
//                        if (out.isSwitch && out.mCurrentState != WANT_CANCEL) {
//                            out.showLastSeconds(out.mDuration.toInt())
//                            out.isCountDown = true
//                            out.isSwitch = false
//                        }
                    } else {
                        out.isCountDown = false
                    }
                    out.dialogManager.updateVoiceLevel(out.audioManager.getVoiceLevel(7))
                }
                MSG_DIALOG_DISMISS -> out.dialogManager.dismissDialog()
            }
        }

    }
}