package io.agora.flat.ui.activity.playback.syncplayer

import android.view.View
import kotlin.math.abs

abstract class AtomPlayer {
    var name: String? = null
        get() {
            return field ?: "${this.javaClass}"
        }

    var atomPlayerListener: AtomPlayerListener? = null

    var atomPlayerPhase: AtomPlayerPhase = AtomPlayerPhase.Idle

    open var isPlaying: Boolean = false

    open var playbackSpeed = 1.0f

    abstract fun play()

    abstract fun pause()

    open fun stop() {
        pause()
        seek(0)
    }

    abstract fun release()

    abstract fun seek(timeMs: Long)

    abstract fun getPhase(): AtomPlayerPhase

    abstract fun currentTime(): Long

    abstract fun duration(): Long

    open fun syncTime(timeMs: Long) {
        if (abs(timeMs - currentTime()) > 1000 && duration() > timeMs) {
            seek(timeMs)
        }
    }

    open fun setPlayerView(view: View) {

    }

    /**
     * mostly，it's used for debug
     */
    fun setPlayerName(name: String) {
        this.name = name
    }

    internal fun updatePlayerPhase(newPhase: AtomPlayerPhase) {
        if (newPhase != atomPlayerPhase) {
            atomPlayerPhase = newPhase
            atomPlayerListener?.onPlayerPhaseChange(this, atomPlayerPhase)
        }
    }
}

interface AtomPlayerListener {
    fun onPlayerPhaseChange(atomPlayer: AtomPlayer, phaseChange: AtomPlayerPhase) {}

    fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {}
}

enum class AtomPlayerPhase {
    /**
     * 视频播放尚未开始或已经结束。
     */
    Idle,

    /**
     * 视频播放已暂停。
     */
    Pause,

    /**
     * 正在播放视频。
     */
    Playing,

    /**
     * 视频正在缓冲。
     */
    Buffering,

    End,
}