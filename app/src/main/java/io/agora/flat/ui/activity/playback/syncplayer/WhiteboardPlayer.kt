package io.agora.flat.ui.activity.playback.syncplayer

import com.herewhite.sdk.Player
import com.herewhite.sdk.domain.PlayerPhase

class WhiteboardPlayer constructor(val player: Player) : AtomPlayer() {
    override var isPlaying: Boolean = false
        get() = atomPlayerPhase == AtomPlayerPhase.Playing

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            player.playbackSpeed = value.toDouble()
        }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun release() {
        player.stop()
    }

    override fun seek(timeMs: Long) {
        player.seekToScheduleTime(timeMs)
        atomPlayerListener?.onSeekTo(this, timeMs = timeMs)
    }

    override fun getPhase(): AtomPlayerPhase {
        return atomPlayerPhase
    }

    override fun currentTime(): Long {
        return player.playerTimeInfo.scheduleTime
    }

    override fun duration(): Long {
        return player.playerTimeInfo.timeDuration
    }

    fun updateWhitePlayerPhase(phase: PlayerPhase) {
        if (phase == PlayerPhase.buffering || phase == PlayerPhase.waitingFirstFrame) {
            updatePlayerPhase(AtomPlayerPhase.Buffering)
        } else if (phase == PlayerPhase.pause || phase == PlayerPhase.playing) {
            player.playbackSpeed = playbackSpeed.toDouble()
            updatePlayerPhase(if (phase == PlayerPhase.pause) AtomPlayerPhase.Pause else AtomPlayerPhase.Playing)
        } else {
            updatePlayerPhase(AtomPlayerPhase.End)
        }
    }
}