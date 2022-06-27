package com.fzm.chat.media.manager

import com.zjy.architecture.data.LocalFile

/**
 * @author zhengjy
 * @since 2021/03/17
 * Description:
 */
sealed class DownState {
    class Running(val progress: Float) : DownState()
    class Fail(val e: Throwable?) : DownState()
    class Success(val file: LocalFile) : DownState()
}