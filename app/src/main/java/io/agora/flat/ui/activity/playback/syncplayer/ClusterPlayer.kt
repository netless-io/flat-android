package io.agora.flat.ui.activity.playback.syncplayer

class ClusterPlayer constructor(private val one: AtomPlayer, private val two: AtomPlayer) : AtomPlayer() {

    private var players: Array<AtomPlayer> = arrayOf(one, two)
    private var pauseReason: Array<Boolean> = arrayOf(false, false)

    private var seeking = 0
    private var time: Long = 0

    init {
        val atomPlayerListener = LocalAtomPlayerListener()

        players[0].atomPlayerListener = atomPlayerListener
        players[1].atomPlayerListener = atomPlayerListener
    }

    private fun other(player: AtomPlayer): AtomPlayer {
        return if (players[0] == player) players[1] else players[0]
    }

    private fun index(player: AtomPlayer): Int {
        return if (players[0] == player) 0 else 1
    }

    override var playbackSpeed: Float = 1.0f
        set(value) {
            field = value
            players.forEach {
                it.playbackSpeed = value
            }
        }

    override fun play() {
        players[0].play()
        players[1].play()
    }

    override fun pause() {
        players[0].pause()
        players[1].pause()
    }

    override fun release() {
        players.forEach {
            it.release()
        }
    }

    override fun seek(timeMs: Long) {
        seeking = 2
        players[0].seek(timeMs)
        players[1].seek(timeMs)
    }

    override fun getPhase(): AtomPlayerPhase {
        return atomPlayerPhase
    }

    override fun currentTime(): Long {
        return time
    }

    override fun duration(): Long {
        return one.duration().coerceAtLeast(two.duration())
    }

    override fun syncTime(timeMs: Long) {
        time = timeMs
        players.forEach {
            it.syncTime(timeMs)
        }
    }

    private fun pauseWhenBuffering(atomPlayer: AtomPlayer) {
        pauseReason[index(atomPlayer)] = true
    }

    private fun isPauseWhenBuffering(atomPlayer: AtomPlayer): Boolean {
        return pauseReason[index(atomPlayer)]
    }

    private fun clearPauseWhenBuffering(atomPlayer: AtomPlayer) {
        pauseReason[index(atomPlayer)] = false
    }

    inner class LocalAtomPlayerListener : AtomPlayerListener {
        override fun onPlayerPhaseChange(atomPlayer: AtomPlayer, phaseChange: AtomPlayerPhase) {
            Logger.d("onPlayerPhaseChange ${atomPlayer.name} $phaseChange")
            when (phaseChange) {
                AtomPlayerPhase.Idle -> {
                }
                AtomPlayerPhase.Pause -> {
                    if (isPauseWhenBuffering(atomPlayer)) {
                        return
                    }
                    if (other(atomPlayer).isPlaying) {
                        other(atomPlayer).pause()
                    }
                    updatePlayerPhase(AtomPlayerPhase.Pause)
                }
                AtomPlayerPhase.Playing -> {
                    if (!other(atomPlayer).isPlaying) {
                        clearPauseWhenBuffering(other(atomPlayer))
                        other(atomPlayer).play()
                    }
                    updatePlayerPhase(AtomPlayerPhase.Playing)
                }
                AtomPlayerPhase.Buffering -> {
                    if (other(atomPlayer).isPlaying) {
                        pauseWhenBuffering(other(atomPlayer))
                        other(atomPlayer).pause()
                    }
                    updatePlayerPhase(AtomPlayerPhase.Buffering)
                }
            }
        }

        override fun onSeekTo(atomPlayer: AtomPlayer, timeMs: Long) {
            seeking--
            if (seeking != 0) {
                atomPlayerListener?.onSeekTo(this@ClusterPlayer, timeMs = timeMs)
            }
        }
    }
}
