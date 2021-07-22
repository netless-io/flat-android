package com.herewhite.demo.player

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.net.Uri
import android.os.Bundle
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
import io.agora.flat.ui.activity.playback.syncplayer.AtomPlayer
import io.agora.flat.ui.activity.playback.syncplayer.AtomPlayerPhase
import io.agora.flat.ui.activity.playback.syncplayer.Logger

class RtcVideoExoPlayer(
    private val context: Context,
    private val videos: List<VideoItem>,
) : AtomPlayer(), Player.EventListener {

    private var exoPlayer = SimpleExoPlayer.Builder(context.applicationContext).build()
    private var mediaSource: MediaSource? = null
    private var playerView: PlayerView? = null
    private val dataSourceFactory: DataSource.Factory
    private val handler: Handler = Handler(Looper.getMainLooper())

    private var currentState = Player.STATE_IDLE
    private var currentPlaying: VideoItem? = null
    private var isRtcPlaying = false

    init {
        exoPlayer.addListener(this)
        exoPlayer.setAudioAttributes(AudioAttributes.DEFAULT, false)
        exoPlayer.playWhenReady = false
        dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, context.getString(R.string.app_name)))
        observeLifecycle()
    }

    private fun observeLifecycle() {
        (context as Activity).application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
                if (Util.SDK_INT > 23) {
                    playerView!!.onResume()
                }
            }

            override fun onActivityResumed(activity: Activity) {
                if (Util.SDK_INT <= 23) {
                    playerView!!.onResume()
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (Util.SDK_INT <= 23) {
                    playerView!!.onPause()
                }
            }

            override fun onActivityStopped(activity: Activity) {
                if (Util.SDK_INT > 23) {
                    playerView!!.onPause()
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

            }

            override fun onActivityDestroyed(activity: Activity) {
                context.application.unregisterActivityLifecycleCallbacks(this)
            }
        })
    }

    /**
     * 设置播放视图
     *
     * @param playerView 视图实例
     */
    override fun setPlayerView(playerView: View) {
        if (playerView is PlayerView) {
            this.playerView = playerView
            this.playerView!!.player = exoPlayer
        }
    }

    /**
     * 设置播放链接
     *
     * @param path 播放链接
     */
    private fun setVideoPath(path: String) {
        setVideoURI(Uri.parse(path))
    }

    /**
     * 设置播放 Uri
     *
     * @param uri 播放链接对应的 Uri
     */
    private fun setVideoURI(uri: Uri) {
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
        checkAndPlayTime(timeMs, true)
    }

    /**
     * 获取当前是否正在播放
     *
     * @return true or false
     */
    override var isPlaying: Boolean = false
        get() = isRtcPlaying && AtomPlayerPhase.Playing == atomPlayerPhase

    override var playbackSpeed = 1.0f
        set(value) {
            field = value
            exoPlayer.setPlaybackParameters(PlaybackParameters(value))
        }

    override fun play() {
        if (!isRtcPlaying) {
            isRtcPlaying = true
            exoPlayer.playWhenReady = true
        }
    }

    override fun pause() {
        if (isRtcPlaying) {
            isRtcPlaying = false
            exoPlayer.playWhenReady = false
        }
    }

    override fun release() {
        exoPlayer.release()
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

    override fun syncTime(timeMs: Long) {
        Logger.d("$name syncTime $timeMs")
        checkAndPlayTime(timeMs)
    }

    private fun checkAndPlayTime(timeMs: Long, seek: Boolean = false) {
        val recordItem = getRecordItem(timeMs)
        if (recordItem != null) {
            if (recordItem == currentPlaying) {
                if (seek) {
                    exoPlayer.seekTo(timeMs - recordItem.beginTime)
                }
            } else {
                currentPlaying = recordItem
                setVideoPath(recordItem.videoURL)
                exoPlayer.playWhenReady = true
            }
        } else {
            currentPlaying = null
            exoPlayer.playWhenReady = false
        }
    }

    private fun getRecordItem(timeMs: Long): VideoItem? {
        return videos.find { timeMs >= it.beginTime && timeMs < it.endTime }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Logger.d("$name onPlayerStateChanged $playWhenReady $playbackState")
        currentState = playbackState
        when (playbackState) {
            Player.STATE_IDLE -> {
            }
            Player.STATE_BUFFERING -> {
                updatePlayerPhase(AtomPlayerPhase.Buffering)
            }
            Player.STATE_READY -> {
                updatePlayerPhase(if (isRtcPlaying) AtomPlayerPhase.Playing else AtomPlayerPhase.Pause)
            }
            Player.STATE_ENDED -> {

            }
            else -> {
            }
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Logger.e("$name onPlayerError ${error.message}")
        currentPlaying = null
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

data class VideoItem constructor(
        val beginTime: Long,
        val endTime: Long,
        val videoURL: String,
)