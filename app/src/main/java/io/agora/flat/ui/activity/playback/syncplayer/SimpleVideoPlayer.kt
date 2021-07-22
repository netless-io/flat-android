package io.agora.flat.ui.activity.playback.syncplayer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import io.agora.flat.R

class SimpleVideoPlayer(
        context: Context,
        videoPath: String,
) : AtomPlayer(), Player.EventListener {
    private var exoPlayer: SimpleExoPlayer = SimpleExoPlayer.Builder(context.applicationContext).build()
    private var mediaSource: MediaSource? = null
    private var playerView: PlayerView? = null
    private val dataSourceFactory: DataSource.Factory
    private val handler: Handler = Handler(Looper.getMainLooper())

    private var currentState = Player.STATE_IDLE

    init {
        exoPlayer.addListener(this)
        exoPlayer.setAudioAttributes(AudioAttributes.DEFAULT, false)
        exoPlayer.playWhenReady = false
        dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, context.getString(R.string.app_name)))

        setVideoPath(videoPath)
    }

    /**
     * 设置播放视图
     *
     * @param playerView 视图实例
     */
    override fun setPlayerView(playerView: View) {
        if (playerView is PlayerView) {
            this.playerView = playerView
            this.playerView!!.requestFocus()
            this.playerView!!.player = exoPlayer
        }
    }

    /**
     * 设置播放链接
     *
     * @param path 播放链接
     */
    fun setVideoPath(path: String) {
        setVideoURI(Uri.parse(path))
    }

    /**
     * 设置播放 Uri
     *
     * @param uri 播放链接对应的 Uri
     */
    fun setVideoURI(uri: Uri) {
        mediaSource = createMediaSource(uri)
        atomPlayerPhase = AtomPlayerPhase.Buffering
        exoPlayer.prepare(mediaSource!!)
    }

    /**
     * 由 nativePlayer 进行主动 seek，然后在 seek 完成后，再调用 [PlayerSyncManager] 同步
     *
     * @param time 跳转时间戳
     * @param unit 时间戳单位
     */
    override fun seek(timeMs: Long) {
        exoPlayer.seekTo(timeMs)
    }

    override var isPlaying: Boolean = false
        get() = exoPlayer.isPlaying

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            exoPlayer.setPlaybackParameters(PlaybackParameters(value))
        }

    override fun play() {
        handler.post {
            if (isPlaying) {
                return@post
            }
            exoPlayer.playWhenReady = true

            Logger.d("play $name isPlaying $isPlaying")
        }
    }

    override fun pause() {
        handler.post {
            if (!isPlaying) {
                return@post
            }
            exoPlayer.playWhenReady = false

            Logger.d("pause $name isPlaying $isPlaying")
        }
    }

    override fun release() {
        exoPlayer.release()
        mediaSource = null
    }

    override fun getPhase(): AtomPlayerPhase {
        return atomPlayerPhase
    }

    override fun currentTime(): Long {
        return exoPlayer.currentPosition
    }

    override fun duration(): Long {
        return exoPlayer.duration
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Logger.d("$name onPlayerState $isPlaying $playbackState")
        currentState = playbackState
        when (playbackState) {
            Player.STATE_IDLE -> {
            }
            Player.STATE_BUFFERING -> {
                updatePlayerPhase(AtomPlayerPhase.Buffering)
            }
            Player.STATE_READY -> {
                updatePlayerPhase(if (isPlaying) AtomPlayerPhase.Playing else AtomPlayerPhase.Pause)
            }
            Player.STATE_ENDED -> {
            }
            else -> {
            }
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Logger.d("$name onPlayerError ${error.message}")
        when (error.type) {
            ExoPlaybackException.TYPE_SOURCE -> {
            }
            ExoPlaybackException.TYPE_RENDERER -> {
            }
            ExoPlaybackException.TYPE_UNEXPECTED -> {
            }
            ExoPlaybackException.TYPE_REMOTE -> {
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        Logger.d("$name onIsPlayingChanged $isPlaying")
    }

    override fun onSeekProcessed() {
        val pos = exoPlayer.currentPosition
        atomPlayerListener?.onSeekTo(this, pos)
    }

    private fun createMediaSource(uri: Uri): MediaSource {
        return when (val type = Util.inferContentType(uri)) {
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }
}