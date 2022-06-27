package com.fzm.chat.media.manager

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.zjy.architecture.ext.audioManager
import java.io.IOException

/**
 * @author zhengjy
 * @since 2021/03/10
 * Description:
 */
object MediaManager : LifecycleEventObserver {

    private var mPlayer: MediaPlayer? = null

    private var mUrl: String = ""
    private var mActive = false
    private var isPlaying = false
    private var listener: ((Boolean) -> Unit)? = null

    fun isPlaying() = mPlayer?.isPlaying ?: false

    fun currentAudio() = mUrl

    fun setOnPlayStateChangedListener(context: Context, listener: (Boolean) -> Unit) {
        (context as LifecycleOwner).lifecycle.addObserver(this)
        this.listener = listener
    }

    fun play(
        context: Context,
        url: String?,
        mode: Int,
        onCompletionListener: MediaPlayer.OnCompletionListener
    ) {
        if (mPlayer == null) {
            mPlayer = MediaPlayer()
            mPlayer?.setOnErrorListener { _, _, _ ->
                mPlayer?.reset()
                false
            }
        } else {
            mPlayer?.reset()
        }
        if (url == null) {
            onCompletionListener.onCompletion(mPlayer)
            return
        }
        mUrl = url

        try {
            context.audioManager?.mode = mode
            mPlayer?.setAudioAttributes(
                AudioAttributes.Builder()
                    .apply {
                        if (mode == AudioManager.MODE_NORMAL) {
                            setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        } else {
                            setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
                        }
                    }
                    .build()
            )
            mPlayer?.setOnCompletionListener {
                isPlaying = false
                listener?.invoke(isPlaying)
                onCompletionListener.onCompletion(it)
            }
            if (url.startsWith("http")) {
                mPlayer?.setDataSource(context, Uri.parse(url), null)
            } else {
                mPlayer?.setDataSource(url)
            }
            mPlayer?.prepareAsync()
            mPlayer?.setOnPreparedListener {
                isPlaying = true
                listener?.invoke(isPlaying)
                mPlayer?.start()
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stop() {
        if (isPlaying()) {
            isPlaying = false
            mUrl = ""
            listener?.invoke(isPlaying)
            mPlayer?.reset()
        }
    }


    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (source.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            source.lifecycle.removeObserver(this)
            onDestroy()
            return
        }
        activeStateChanged(shouldBeActive(source))
    }

    private fun activeStateChanged(newActive: Boolean) {
        if (mActive == newActive) {
            return
        }
        mActive = newActive
        if (mActive) {
            onActive()
        } else {
            onInActive()
        }
    }

    private fun shouldBeActive(source: LifecycleOwner): Boolean {
        return source.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    private fun onActive() {

    }

    private fun onInActive() {
        stop()
    }

    private fun onDestroy() {
        listener = null
        mUrl = ""
        isPlaying = false
        mPlayer?.release()
        mPlayer = null
    }
}